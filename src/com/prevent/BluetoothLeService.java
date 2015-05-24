package com.prevent;

import android.app.Service;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Service for managing connection and data communication
 * with a GATT server hosted on a Bluetooth LE device
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    // Used to connect to the BLE device
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    // Recurring event that polls for new data from the device
    private static Timer timer = new Timer();
    private static byte[] previousData = new byte[]{};

    // Characteristic to read data from
    private BluetoothGattCharacteristic mCharacteristic;

    // State variables
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // Intent actions and extras
    public final static String ACTION_GATT_CONNECTED =
            "com.prevent.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.prevent.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.prevent.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_TEMP_DATA =
            "com.prevent.EXTRA_TEMP_DATA";
    public final static String EXTRA_HUMI_DATA =
            "com.prevent.EXTRA_HUMI_DATA";
    public final static String EXTRA_VOC_DATA =
            "com.prevent.EXTRA_VOC_DATA";
    public final static String EXTRA_PM_DATA =
            "com.prevent.EXTRA_PM_DATA";

    // Characteristic UUID from which to read data
    private static final String DATA_CHARACTERISTIC_UUID = "21819ab0-c937-4188-b0db-b9621e1696cd";

    // Interval at which to poll for data
    // Should match the sleep/wake cycle of the device
    private static final int DATA_POLL_INTERVAL = 8000;
    
    // Some arbitrary number
    private static final int ONGOING_NOTIFICATION_ID = 8989;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");

                // Attempts to discover services after successful connection.
                Log.i(TAG, "Starting service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                filterGattServices();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Helper for packaging data from Bluetooth into an Intent
     */
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();

        // Check for the expected payload
        if (data == null || data.length != 6) {
            Log.e(TAG, "Unexpected data length: " + data.length);

            // Write the data formatted in HEX
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.e(TAG, "Unexpected data: " + stringBuilder.toString());
            }
        } else if (Arrays.equals(data, previousData)) {
            // If we've gotten the same data as before, 
            // then our connection to the GATT server has probably been lost
            // Note: the Android device takes significantly longer to realize this
            Log.w(TAG, "Detected stale (cached) data :(");
        } else {
            // Convert unsigned bytes to int
            // Note: the masking is necessary
            int[] extras = new int[]{ data[0] & 0xFF, data[1] & 0xFF,
                data[2] & 0xFF, data[3] & 0xFF, data[4] & 0xFF, data[5] & 0xFF };

            // Parse data into four integers
            //   14-bits Temperature
            //   14-bits Humidity
            //   10-bits VOC
            //   10-bits PM
            int temperature = extras[0] << 8 | (extras[1] & 0xFC) >> 2;
            int humidity = (extras[1] & 0x3) << 12 | extras[2] << 4 | (extras[3] & 0xF0) >> 4;
            int voc = (extras[3] & 0xF) << 6 | (extras[4] & 0xFC) >> 2;
            int particulates = (extras[4] & 0x3) << 8 | extras[5];

            Log.d(TAG, "Got data ("
                 +  "T: " + temperature
                 + ",H: " + humidity
                 + ",V: " + voc
                 + ",P: " + particulates + ")");

            // Add parsed data into intent
            intent.putExtra(EXTRA_TEMP_DATA, temperature);
            intent.putExtra(EXTRA_HUMI_DATA, humidity);
            intent.putExtra(EXTRA_VOC_DATA, voc);
            intent.putExtra(EXTRA_PM_DATA, particulates);

            previousData = data;
            sendBroadcast(intent);
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        // Start polling for data every few seconds
        timer.scheduleAtFixedRate(new PollDeviceTask(), 0, DATA_POLL_INTERVAL);
        
        // Promote this service to the foreground
        Notification notification = new Notification(R.drawable.icon, 
            getText(R.string.foreground_service_text), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, WelcomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                getText(R.string.foreground_service_text), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }
    
    @Override
    public void onDestroy() {
        disconnect();
    }

    /**
     * Initializes a reference to the local Bluetooth adapter
     * @return Return true if the initialization is successful
     */
    public boolean initialize() {
        // Get a reference to BluetoothAdapter through BluetoothManager
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully.
     *         The connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to manually manage the connection to the device,
        // so we are setting the autoConnect parameter to false
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection.
     * The disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mCharacteristic = null;
    }

    /**
     * Iterates through discovered services and characteristics
     * Pulls out the one characteristic from which data should be read
     */
    private void filterGattServices() {
        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();

        // Loop through available GATT Services
        String uuid = null;
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loop through available Characteristics
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if (uuid.equals(DATA_CHARACTERISTIC_UUID)) {
                    mCharacteristic = gattCharacteristic;
                }
            }
        }
    }

    private class PollDeviceTask extends TimerTask {
        public void run() {
            if (mBluetoothAdapter != null
                    && mBluetoothGatt != null
                    && mCharacteristic != null) {
                if (mConnectionState == STATE_DISCONNECTED && mBluetoothDeviceAddress != null) {
                    connect(mBluetoothDeviceAddress);
                } else if (mConnectionState == STATE_CONNECTED) {
                    mBluetoothGatt.readCharacteristic(mCharacteristic);
                }
            }
        }
    }
}
