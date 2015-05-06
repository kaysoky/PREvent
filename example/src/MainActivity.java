/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF 
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.bluegiga.BLEDemo;

import java.util.Iterator;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bluegiga.BLEDemo.datamodel.Common;
import com.bluegiga.BLEDemo.datamodel.Consts;
import com.bluegiga.BLEDemo.datamodel.Device;
import com.bluegiga.BLEDemo.datamodel.DeviceAdapter;
import com.bluegiga.BLEDemo.datamodel.Engine;
import com.bluegiga.BLEDemo.datamodel.ScanRecordParser;

// MainActivity - initializes all necessary parts of application and displays list of BLE devices.
// It manages statuses of devices. User can connect/disconnect device or start scanning for new BLE devices
public class MainActivity extends Activity {

    private static final int BlUETOOTH_SETTINGS_REQUEST_CODE = 100;
    public static final int SCAN_PERIOD = 10000;

    private static IntentFilter bleIntentFilter;

    private GridView devicesGrid;
    private ImageButton scanButton;
    private ProgressBar scanProgress;
    private TextView titleBarView;
    private TextView noDevicesFoundView;;
    private DeviceAdapter adapter;
    private BluetoothLeService mBluetoothLeService;
    private Dialog mDialog;
    private ProgressDialog mProgressDialog;
    private boolean bleIsSupported = true;
    private float lastScale = 0.0f;

    // Implements callback method for service connection
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            startScanning();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Implements receive methods that handle a specific intent actions from
    // mBluetoothLeService
    private final BroadcastReceiver mBluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_DEVICE_DISCOVERED.equals(action)) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent
                        .getParcelableExtra(BluetoothLeService.DISCOVERED_DEVICE);
                int rssi = (int) intent.getIntExtra(BluetoothLeService.RSSI, 0);
                byte[] scanRecord = intent.getByteArrayExtra(BluetoothLeService.SCAN_RECORD);

                Device device = Engine.getInstance().addBluetoothDevice(bluetoothDevice, rssi, scanRecord);
                device.setRssi(rssi);
                device.setAdvertData(ScanRecordParser.getAdvertisements(scanRecord));

                ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
            } else if (intent.getAction().equals(BluetoothLeService.ACTION_STOP_SCAN)) {
                setScanningStatus(Engine.getInstance().getDevices().size() > 0);
                setScanningProgress(false);
            } else if (intent.getAction().equals(BluetoothLeService.ACTION_GATT_CONNECTED)) {
                refreshViewOnUiThread();
                startServiceCharacteristicAtivity(intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS));
            } else if (intent.getAction().equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)
                    || intent.getAction().equals(BluetoothLeService.ACTION_READ_REMOTE_RSSI)
                    || intent.getAction().equals(BluetoothLeService.ACTION_GATT_CONNECTION_STATE_ERROR)) {
                refreshViewOnUiThread();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if Bluetooth Low Energy technology is supported on device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleIsSupported = false;

            mDialog = Dialogs.showAlert(this.getText(R.string.app_name), this.getText(R.string.ble_not_supported),
                    this, getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            mDialog.dismiss();
                            finish();
                        }
                    }, null);
            return;
        }

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        titleBarView = (TextView) findViewById(R.id.titleBar);
        titleBarView.setText(getText(R.string.scanning));

        noDevicesFoundView = (TextView) findViewById(R.id.noDevicesFoundLabel);

        scanProgress = (ProgressBar) findViewById(R.id.scanProgress);

        configureScanButton();

        Engine.getInstance().init(this.getApplicationContext());

        adapter = new DeviceAdapter(this, Engine.getInstance().getDevices());

        configureDeviceGrid();
        configureBanner();
        configureLogo();

        // Check if Bluetooth module is enabled
        checkBluetoothAdapter();

        registerForContextMenu(devicesGrid);
    }

    // Configures grid view for showing devices list
    private void configureDeviceGrid() {
        devicesGrid = (GridView) findViewById(R.id.deviceGrid);

        devicesGrid.setAdapter(adapter);

        devicesGrid.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Device elem = Engine.getInstance().getDevices().get(position);
                if (elem.isConnected()) {
                    startServiceCharacteristicAtivity(elem.getAddress());
                } else {
                    mProgressDialog = Dialogs.showProgress(getText(R.string.connecting_title),
                            getText(R.string.connecting), MainActivity.this, new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    mProgressDialog.dismiss();
                                    restartBluetooth();
                                }
                            });
                    mBluetoothLeService.connect(elem);
                }
            }
        });
    }

    // Configures scan button
    private void configureScanButton() {
        scanButton = (ImageButton) findViewById(R.id.actionButton);
        scanButton.setBackground(getResources().getDrawable(R.drawable.ic_action_refresh));
        scanButton.setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startScanning();
            }
        });
    }

    // Called after back button click while connection was establishing
    private void restartBluetooth() {
        mBluetoothLeService.close();
        Engine.getInstance().clearDeviceList(false);
        ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
        startScanning();
    }

    // Sets click handler on logo image
    private void configureLogo() {
        ImageView logo = (ImageView) findViewById(R.id.logo);
        logo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showAbout();
            }
        });
    }

    // Sets click handler on banner
    private void configureBanner() {
        View banner = findViewById(R.id.banner);
        banner.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Common.BLUEGIGA_URL));
                startActivity(i);

            }
        });
    }

    // If Bluetooth is not supported on device, the application is closed
    // in other case method enable Bluetooth
    private void checkBluetoothAdapter() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            bluetoothNotSupported();
        } else if (!bluetoothAdapter.isEnabled()) {
            bluetoothEnable();
        } else {
            connectService();
        }
    }

    private void connectService() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    // Displays dialog with information that phone doesn't support Bluetooth
    private void bluetoothNotSupported() {
        mDialog = Dialogs.showAlert(getText(R.string.app_name), getText(R.string.bluetooth_not_supported), this,
                getText(android.R.string.ok), null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }, null);
    }

    // Starts ServiceCharacteristicActivity activity with DEVICE_ADDRESS extras
    private void startServiceCharacteristicAtivity(String deviceAddress) {
        Intent myIntent = new Intent(MainActivity.this, ServiceCharacteristicActivity.class);
        myIntent.putExtra(Consts.DEVICE_ADDRESS, deviceAddress);
        startActivity(myIntent);
    }

    // Displays scanning status in UI and starts scanning for new BLE devices
    private void startScanning() {
        setScanningProgress(true);
        setScanningStatus(true);
        // Connected devices are not deleted from list
        Engine.getInstance().clearDeviceList(true);
        // For each connected device read rssi
        Iterator<Device> device = Engine.getInstance().getDevices().iterator();
        while (device.hasNext()) {
            mBluetoothLeService.readRemoteRssi(device.next());
        }
        ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
        // Starts a scan for Bluetooth LE devices for SCAN_PERIOD miliseconds
        mBluetoothLeService.startScanning(SCAN_PERIOD);

        registerReceiver(mBluetoothLeReceiver, getGattUpdateIntentFilter());
    }

    private void setScanningStatus(boolean foundDevices) {
        if (foundDevices) {
            noDevicesFoundView.setVisibility(View.GONE);
        } else {
            noDevicesFoundView.setVisibility(View.VISIBLE);
        }
    }

    private void setScanningProgress(boolean isScanning) {
        if (isScanning) {
            scanButton.setVisibility(View.GONE);
            scanProgress.setVisibility(View.VISIBLE);
            titleBarView.setText(getText(R.string.scanning));
        } else {
            scanButton.setVisibility(View.VISIBLE);
            scanProgress.setVisibility(View.GONE);
            titleBarView.setText(getText(R.string.not_scanning));
        }
    }

    // Returns intent filter for receiving specific action from
    // mBluetoothLeService:
    // - BluetoothLeService.ACTION_DEVICE_DISCOVERED - new
    // device was discovered
    // - BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED - services were
    // discovered
    // - BluetoothLeService.ACTION_STOP_SCAN - scanning finished -
    // BluetoothLeService.ACTION_GATT_CONNECTED - device connected
    // - BluetoothLeService.ACTION_GATT_DISCONNECTED - device disconnected -
    // BluetoothLeService.ACTION_READ_REMOTE_RSSI - device rssi was read
    // This method is used when registerReceiver method is called
    private static IntentFilter getGattUpdateIntentFilter() {
        if (bleIntentFilter == null) {
            bleIntentFilter = new IntentFilter();
            bleIntentFilter.addAction(BluetoothLeService.ACTION_DEVICE_DISCOVERED);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_STOP_SCAN);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_READ_REMOTE_RSSI);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTION_STATE_ERROR);
        }
        return bleIntentFilter;
    }

    private void refreshViewOnUiThread() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    // Displays dialog and request user to enable Bluetooth
    private void bluetoothEnable() {
        mDialog = Dialogs.showAlert(this.getText(R.string.no_bluetooth_dialog_title_text), this
                .getText(R.string.no_bluetooth_dialog_text), this, getText(android.R.string.ok),
                getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Intent intentBluetooth = new Intent();
                        intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        MainActivity.this.startActivityForResult(intentBluetooth, BlUETOOTH_SETTINGS_REQUEST_CODE);
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        MainActivity.this.finish();
                    }
                });
    }

    // Displays about dialog
    private void showAbout() {
        Intent myIntent = new Intent(getApplicationContext(), AboutActivity.class);
        startActivity(myIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BlUETOOTH_SETTINGS_REQUEST_CODE) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled() && mDialog != null) {
                mDialog.show();
            } else {
                connectService();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bleIsSupported) {
            registerReceiver(mBluetoothLeReceiver, getGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                setScanningProgress(mBluetoothLeService.isScanning());
            }
            ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
        }

        configureFontScale();
    }

    // Configures number of shown advertisement types
    private void configureFontScale() {

        float scale = getResources().getConfiguration().fontScale;
        if (lastScale != scale) {
            lastScale = scale;
            if (lastScale == Common.FONT_SCALE_LARGE) {
                Device.MAX_EXTRA_DATA = 2;
            } else if (lastScale == Common.FONT_SCALE_XLARGE) {
                Device.MAX_EXTRA_DATA = 1;
            } else {
                Device.MAX_EXTRA_DATA = 3;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bleIsSupported) {
            unregisterReceiver(mBluetoothLeReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.close();
        }
        Engine.getInstance().close();
        if (bleIsSupported && mBluetoothLeService != null) {
            unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_about:
            showAbout();
            break;
        default:
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        int pos = ((AdapterContextMenuInfo) menuInfo).position;
        Device device = (Device) devicesGrid.getItemAtPosition(pos);

        if (device.isConnected()) {
            menu.add(ContextMenu.NONE, Common.MENU_DISCONNECT, ContextMenu.NONE, getText(R.string.disconnect));
        } else {
            menu.add(ContextMenu.NONE, Common.MENU_CONNECT, ContextMenu.NONE, getText(R.string.connect));
        }

        if (device.hasAdvertDetails()) {
            menu.add(ContextMenu.NONE, Common.MENU_SCAN_RECORD_DETAILS, ContextMenu.NONE,
                    getText(R.string.advertisement_details));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        int selected = item.getItemId();
        int itemPos = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
        final Device device = (Device) devicesGrid.getItemAtPosition(itemPos);

        switch (selected) {
        case Common.MENU_CONNECT:
            mBluetoothLeService.connect(device);
            break;
        case Common.MENU_DISCONNECT:
            mBluetoothLeService.disconnect(device);
            break;
        case Common.MENU_SCAN_RECORD_DETAILS:
            // Displays more advertisement details which BLE device is sending
            Dialogs.showAlert(getText(R.string.advertisement_details_title), prepareAdvertisementText(device),
                    MainActivity.this, getText(android.R.string.ok), null, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }, null);
            break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((mProgressDialog != null) && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                mBluetoothLeService.close();
                ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mBluetoothLeService.close();
            ((DeviceAdapter) devicesGrid.getAdapter()).notifyDataSetChanged();
        } else {
            super.onBackPressed(); // allows standard use of backbutton for page
                                   // 1
        }
    }

    // Gets advertisement data line by line
    private String prepareAdvertisementText(Device device) {
        String advertisementData = "";
        for (String data : device.getAdvertData()) {
            advertisementData += data + "\n";
        }
        return advertisementData;
    }

}
