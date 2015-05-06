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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.bluegiga.BLEDemo.datamodel.Common;
import com.bluegiga.BLEDemo.datamodel.Consts;
import com.bluegiga.BLEDemo.datamodel.Converters;
import com.bluegiga.BLEDemo.datamodel.Device;
import com.bluegiga.BLEDemo.datamodel.Engine;
import com.bluegiga.BLEDemo.datamodel.Unit;
import com.bluegiga.BLEDemo.datamodel.xml.Bit;
import com.bluegiga.BLEDemo.datamodel.xml.Characteristic;
import com.bluegiga.BLEDemo.datamodel.xml.Descriptor;
import com.bluegiga.BLEDemo.datamodel.xml.Enumeration;
import com.bluegiga.BLEDemo.datamodel.xml.Field;
import com.bluegiga.BLEDemo.datamodel.xml.Service;
import com.bluegiga.BLEDemo.datamodel.xml.ServiceCharacteristic;

// CharacteristicActivity - displays and manages characteristic value
// It is main activity where user can read and write characteristic data
public class CharacteristicActivity extends Activity {

    final private int REFRESH_INTERVAL = 500; // miliseconds

    final private String TYPE_FLOAT = "FLOAT";
    final private String TYPE_SFLOAT = "SFLOAT";
    final private String TYPE_FLOAT_32 = "float32";
    final private String TYPE_FLOAT_64 = "float64";

    private static IntentFilter bleIntentFilter;

    private BluetoothGattCharacteristic mBluetoothCharact;
    private Characteristic mCharact;
    private BluetoothLeService mBluetoothLeService;
    private Service mService;
    private List<BluetoothGattDescriptor> mDescriptors;
    private Iterator<BluetoothGattDescriptor> iterDescriptor;
    private BluetoothGattDescriptor lastDescriptor;
    private boolean readable = false;
    private boolean writeable = false;
    private boolean notify = false;
    private boolean isRawValue = false;
    private boolean parseProblem = false;
    private int offset = 0; // in bytes
    private int currRefreshInterval = REFRESH_INTERVAL; // in seconds
    private byte[] value;
    private Device mDevice;

    private LinearLayout valuesLayout;
    private EditText hexEdit;
    private EditText asciiEdit;
    private EditText decimalEdit;
    private TextView hex;
    private TextView ascii;
    private TextView decimal;

    private int defaultMargin;

    // Implements callback method for service connection
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // If characteristic is readable, call read
            // characteristic method on mBluetoothLeService
            if (readable) {
                mBluetoothLeService.readCharacteristic(mDevice, mBluetoothCharact);
            } else { // Another case prepare empty data and show UI
                if (!isRawValue) {
                    prepareValueData();
                }
                loadValueViews();
            }
            // If characteristic is notify, set notification on it
            if (notify) {
                setNotification();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Implements receive methods that handle a specific intent actions from
    // mBluetoothLeService When ACTION_GATT_DISCONNECTED action is received and
    // device address equals current device, activity closes When
    // ACTION_DATA_AVAILABLE action is received, refresh ativity UI When
    // ACTION_DATA_WRITE action is received, and characteristic uuid equals
    // current activity, show appropriate toast message
    private final BroadcastReceiver mBluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {

                String uuidCharact = intent.getExtras().getString(BluetoothLeService.UUID_CHARACTERISTIC);
                // If time from last update was elapsed then UI is updated and
                // timer is reset
                if (currRefreshInterval >= REFRESH_INTERVAL) {
                    if (uuidCharact.equals(mBluetoothCharact.getUuid().toString())) {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if (currRefreshInterval >= REFRESH_INTERVAL) {
                                    currRefreshInterval = 0;
                                    offset = 0;
                                    value = mBluetoothCharact.getValue();
                                    loadValueViews();
                                }
                            }
                        });
                    }
                }

            } else if (action.equals(BluetoothLeService.ACTION_DATA_WRITE)
                    && intent.getExtras().getString(BluetoothLeService.UUID_CHARACTERISTIC).equals(
                            mBluetoothCharact.getUuid().toString())) {

                final int status = intent.getIntExtra(BluetoothLeService.GATT_STATUS, 0);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Toast.makeText(CharacteristicActivity.this, getText(R.string.characteristic_write_success),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CharacteristicActivity.this, getText(R.string.characteristic_write_fail),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } else if (intent.getAction().equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)) {
                String deviceAddress = intent.getExtras().getString(BluetoothLeService.DEVICE_ADDRESS);
                if (deviceAddress.equals(mDevice.getAddress())) {
                    finish();
                }
            } else if (intent.getAction().equals(BluetoothLeService.ACTION_DESCRIPTOR_WRITE)) {
                UUID descriptorUuid = (UUID) intent.getExtras().get(BluetoothLeService.UUID_DESCRIPTOR);
                if (Common.equalsUUID(descriptorUuid, lastDescriptor.getUuid())) {
                    writeNextDescriptor();
                }
            }
        }
    };

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_characteristic);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);

        defaultMargin = getResources().getDimensionPixelSize(R.dimen.characteristic_text_left_margin);

        valuesLayout = (LinearLayout) findViewById(R.id.valuesLayout);

        final TextView titleBarView = (TextView) findViewById(R.id.titleBar);
        titleBarView.setText(getText(R.string.characteristics));

        String address = getIntent().getExtras().getString(BluetoothLeService.DEVICE_ADDRESS);
        mDevice = Engine.getInstance().getDevice(address);

        mBluetoothCharact = Engine.getInstance().getLastCharacteristic();

        mCharact = Engine.getInstance().getCharacteristic(mBluetoothCharact.getUuid());
        mService = Engine.getInstance().getService(mBluetoothCharact.getService().getUuid());

        mDescriptors = new ArrayList<BluetoothGattDescriptor>();

        setProperties();

        updateBall();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        configureWriteable();

        final TextView serviceNameView = (TextView) findViewById(R.id.serviceName);
        if (mService != null) {
            serviceNameView.setText(mService.getName().trim());
        } else {
            serviceNameView.setText(getText(R.string.unknown_service));
        }

        final TextView charactNameView = (TextView) findViewById(R.id.characteristicName);
        if (mCharact != null) {
            charactNameView.setText(mCharact.getName());
        } else {
            charactNameView.setText(getText(R.string.unknown_characteristic));
        }

        final TextView charactUuidView = (TextView) findViewById(R.id.uuid);
        charactUuidView.setText(getText(R.string.uuid) + " 0x"
                + Common.convert128to16UUID(mBluetoothCharact.getUuid().toString()));

        final TextView charactPropertiesName = (TextView) findViewById(R.id.properties);
        charactPropertiesName.setText(Common.getProperties(this, mBluetoothCharact.getProperties()));

        configureLogo();
        configureBanner();
    }

    // Sets property members for characteristics
    private void setProperties() {
        if (Common.isSetProperty(Common.PropertyType.READ, mBluetoothCharact.getProperties())) {
            readable = true;
        }

        if (Common.isSetProperty(Common.PropertyType.WRITE, mBluetoothCharact.getProperties())
                || Common.isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, mBluetoothCharact.getProperties())) {
            writeable = true;
        }

        if (Common.isSetProperty(Common.PropertyType.NOTIFY, mBluetoothCharact.getProperties())
                || Common.isSetProperty(Common.PropertyType.INDICATE, mBluetoothCharact.getProperties())) {
            notify = true;
        }

        if (mCharact == null || mCharact.getFields() == null) {
            isRawValue = true;
        }
    }

    // Configures characteristic if it is writeable
    private void configureWriteable() {
        if (writeable) {
            ImageButton writeButton = (ImageButton) findViewById(R.id.actionButton);
            writeButton.setVisibility(View.VISIBLE);
            writeButton.setBackground(getResources().getDrawable(R.drawable.ic_action_save));
            writeButton.setOnClickListener(new android.view.View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    writeValueToCharacteristic();
                }
            });
        }
    }

    private void writeValueToCharacteristic() {
        if (isRawValue || parseProblem) {
            EditText hexEdit = (EditText) findViewById(R.id.hexEdit);
            String hex = hexEdit.getText().toString().replaceAll("\\s+", "");
            byte newValue[] = hexToByteArray(hex);
            mBluetoothCharact.setValue(newValue);
        } else {
            mBluetoothCharact.setValue(value);
        }
        mBluetoothLeService.writeCharacteristic(mDevice, mBluetoothCharact);
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

    // Displays about dialog
    private void showAbout() {
        Intent myIntent = new Intent(getApplicationContext(), AboutActivity.class);
        startActivity(myIntent);
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

    // Count time that is used to preventing from very fast refreshing view
    private void updateBall() {
        Timer timer = new Timer();
        TimerTask updateBall = new TimerTask() {

            @Override
            public void run() {
                currRefreshInterval += REFRESH_INTERVAL;
            }
        };
        timer.scheduleAtFixedRate(updateBall, 0, REFRESH_INTERVAL);
    }

    // Sets notification on characteristic data changes
    protected void setNotification() {
        mBluetoothLeService.setCharacteristicNotification(mDevice, mBluetoothCharact, true);

        ArrayList<Descriptor> descriptors = getCharacteristicDescriptors();

        if (descriptors != null) {
            for (BluetoothGattDescriptor blDescriptor : mBluetoothCharact.getDescriptors()) {
                if (isDescriptorAvailable(descriptors, blDescriptor)) {
                    mDescriptors.add(blDescriptor);
                }
            }
        } else {
            mDescriptors = new ArrayList<BluetoothGattDescriptor>(mBluetoothCharact.getDescriptors());
        }

        iterDescriptor = mDescriptors.iterator();
        writeNextDescriptor();

    }

    // Gets all characteristic descriptors
    private ArrayList<Descriptor> getCharacteristicDescriptors() {
        if (mService == null && mCharact == null) {
            return null;
        }

        ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>();

        for (ServiceCharacteristic charact : mService.getCharacteristics()) {
            if (charact.getType().equals(mCharact.getType())) {
                for (Descriptor descriptor : charact.getDescriptors()) {
                    descriptors.add(Engine.getInstance().getDescriptorByType(descriptor.getType()));
                }
            }
        }

        return descriptors;
    }

    // Checks if given descriptor is available in this characteristic
    private boolean isDescriptorAvailable(ArrayList<Descriptor> descriptors, BluetoothGattDescriptor blDescriptor) {
        for (Descriptor descriptor : descriptors) {
            if (Common.equalsUUID(descriptor.getUuid(), blDescriptor.getUuid())) {
                return true;
            }
        }
        return false;
    }

    // Writes next descriptor in order to enable notification or indication
    protected void writeNextDescriptor() {
        if (iterDescriptor.hasNext()) {
            lastDescriptor = iterDescriptor.next();

            if (lastDescriptor.getCharacteristic() == mBluetoothCharact) {
                lastDescriptor.setValue(Common.isSetProperty(Common.PropertyType.NOTIFY, mBluetoothCharact
                        .getProperties()) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothLeService.writeDescriptor(mDevice, lastDescriptor);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBluetoothLeReceiver, getGattUpdateIntentFilter());
        if (!mDevice.isConnected()) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothLeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notify) {
            mBluetoothLeService.setCharacteristicNotification(mDevice, mBluetoothCharact, false);
        }
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    // Returns intent filter for receiving specific action from
    // mBluetoothLeService:
    // - BluetoothLeService.ACTION_DATA_AVAILABLE - new data is available
    // - BluetoothLeService.ACTION_DATA_WRITE - data wrote
    // - BluetoothLeService.ACTION_DESCRIPTOR_WRITE - descriptor wrote
    // - BluetoothLeService.ACTION_GATT_DISCONNECTED - device disconnected
    // This method is used when registerReceiver method is called
    private static IntentFilter getGattUpdateIntentFilter() {
        if (bleIntentFilter == null) {
            bleIntentFilter = new IntentFilter();
            bleIntentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_DESCRIPTOR_WRITE);
        }
        return bleIntentFilter;
    }

    // Builds activity UI based on characteristic content
    private void loadValueViews() {
        valuesLayout.removeAllViews();
        if (!isRawValue) {
            if (parseProblem || !addNormalValue()) {
                addInvalidValue();
            }
        } else {
            addRawValue();
        }
    }

    // Builds activity UI in tree steps:
    // a) add views based on characteristic content without setting values
    // b) add problem info view
    // c) add raw views (hex, ASCII, decimal) with setting values
    private void addInvalidValue() {
        valuesLayout.removeAllViews();
        addNormalValue();
        addProblemInfoView();
        addRawValue();
    }

    // Only called when characteristic is standard Bluetooth characteristic
    // Build activity UI based on characteristic content and also take account
    // of field requirements
    private boolean addNormalValue() {
        for (int i = 0; i < mCharact.getFields().size(); i++) {
            try {
                Field field = mCharact.getFields().get(i);
                addField(field);
            } catch (Exception ex) {
                Log.i("CharacteristicUI", String.valueOf(i));
                Log.i("Characteristic fields size", String.valueOf(mCharact.getFields().size()));
                Log.i("Characteristic value", Converters.getDecimalValue(value));
                parseProblem = true;
                return false;
            }
        }
        return true;
    }

    // Add single field
    private void addField(Field field) {
        if (isFieldPresent(field)) {
            if (field.getReferenceFields().size() > 0) {
                for (Field subField : field.getReferenceFields()) {
                    addField(subField);
                }
            } else {
                if (field.getBitfield() != null) {
                    addBitfield(field);
                } else if (field.getEnumerations() != null && field.getEnumerations().size() > 0) {
                    addEnumeration(field);
                } else {
                    addValue(field);
                }
            }
        }
    }

    // Initializes byte array with empty characteristic content
    private void prepareValueData() {
        int size = characteristicSize();
        if (size != 0) {
            value = new byte[size];
        }
    }

    // Returns characteristic size in bytes
    private int characteristicSize() {
        int size = 0;
        for (Field field : mCharact.getFields()) {
            size += fieldSize(field);
        }
        return size;
    }

    // Returns only one field size in bytes
    private int fieldSize(Field field) {

        String format = field.getFormat();
        if (format != null) {
            return Engine.getInstance().getFormat(format);
        } else if (field.getReferenceFields().size() > 0) {
            int subFieldsSize = 0;
            for (Field subField : field.getReferenceFields()) {
                subFieldsSize += fieldSize(subField);
            }
            return subFieldsSize;
        } else {
            return 0;
        }
    }

    // Checks if field is present based on it's requirements and bitfield
    // settings
    private boolean isFieldPresent(Field field) {
        if (parseProblem) {
            return true;
        }
        if (field.getRequirement() == null || field.getRequirement().equals(Consts.REQUIREMENT_MANDATORY)) {
            return true;
        } else {
            for (Field bitField : getBitFields()) {
                for (Bit bit : bitField.getBitfield().getBits()) {
                    for (Enumeration enumeration : bit.getEnumerations()) {
                        if (enumeration.getRequires() != null
                                && field.getRequirement().equals(enumeration.getRequires())) {
                            boolean fieldPresent = checkRequirement(bitField, enumeration, bit);
                            return fieldPresent;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Checks requirement on exactly given bitfield, enumeration and bit
    private boolean checkRequirement(Field bitField, Enumeration enumeration, Bit bit) {
        int formatLength = Engine.getInstance().getFormat(bitField.getFormat());
        int off = getFieldOffset(bitField);
        int val = readInt(off, formatLength);
        int enumVal = readEnumInt(bit.getIndex(), bit.getSize(), val);
        return (enumVal == enumeration.getKey() ? true : false);
    }

    /*
     * 
     * --- VALUE SETTERS & GETTERS SECTION ---
     */

    // Converts string given in hexadecimal system to byte array
    private byte[] hexToByteArray(String hex) {
        byte byteArr[] = new byte[hex.length() / 2];
        for (int i = 0; i < byteArr.length; i++) {
            int temp = Integer.parseInt(hex.substring(i * 2, (i * 2) + 2), 16);
            byteArr[i] = (byte) (temp & 0xFF);
        }
        return byteArr;
    }

    // Converts string given in decimal system to byte array
    private byte[] decToByteArray(String dec) {
        if (dec.length() == 0) {
            return new byte[] {};
        }
        String decArray[] = dec.split(" ");
        byte byteArr[] = new byte[decArray.length];

        for (int i = 0; i < decArray.length; i++) {
            try {
                byteArr[i] = (byte) (Integer.parseInt(decArray[i]));
            } catch (NumberFormatException e) {
                return new byte[] { 0 };
            }
        }
        return byteArr;
    }

    // Converts int to byte array
    private byte[] intToByteArray(int newVal, int formatLength) {
        byte val[] = new byte[formatLength];
        for (int i = 0; i < formatLength; i++) {
            val[i] = (byte) (newVal & 0xff);
            newVal >>= 8;
        }
        return val;
    }

    // Checks if decimal input value is valid
    private boolean isDecValueValid(String decValue) {
        char value[] = decValue.toCharArray();
        int valLength = value.length;
        boolean valid = false;
        if (decValue.length() < 4) {
            valid = true;
        } else {
            valid = value[valLength - 1] == ' ' || value[valLength - 2] == ' ' || value[valLength - 3] == ' '
                    || value[valLength - 4] == ' ';
        }
        return valid;
    }

    // Reads integer value for given offset and field size
    private int readInt(int offset, int size) {
        int val = 0;
        for (int i = 0; i < size; i++) {
            val <<= 8;
            val |= value[offset + i];
        }
        return val;
    }

    // Reads next enumeration value for given enum length
    private int readNextEnum(int formatLength) {
        int result = 0;
        for (int i = 0; i < formatLength; i++) {
            result |= value[offset];
            if (i < formatLength - 1) {
                result <<= 8;
            }
        }
        offset += formatLength;
        return result;
    }

    // Reads next value for given format
    private String readNextValue(String format) {
        if (value == null) {
            return "";
        }

        int formatLength = Engine.getInstance().getFormat(format);

        String result = "";
        // If field length equals 0 then reads from offset to end of
        // characteristic data
        if (formatLength == 0) {
            result = new String(Arrays.copyOfRange(value, offset, value.length));
            offset += value.length;
        } else {
            // If format type is kind of float type then reads float value
            // else reads value as integer
            if (format.equals(TYPE_SFLOAT) || format.equals(TYPE_FLOAT) || format.equals(TYPE_FLOAT_32)
                    || format.equals(TYPE_FLOAT_64)) {
                double fValue = readFloat(format, formatLength);
                result = String.valueOf(fValue);
            } else {
                for (int i = offset; i < offset + formatLength; i++) {
                    result += (int) (value[i] & 0xff);
                }
            }
            offset += formatLength;
        }
        return result;
    }

    // Reads float value for given format
    private double readFloat(String format, int formatLength) {
        double result = 0.0;
        if (format.equals(TYPE_SFLOAT)) {
            result = Common.readSfloat(value, offset, formatLength - 1);
        } else if (format.equals(TYPE_FLOAT)) {
            result = Common.readFloat(value, offset, formatLength - 1);
        } else if (format.equals(TYPE_FLOAT_32)) {
            result = Common.readFloat32(value, offset, formatLength);
        } else if (format.equals(TYPE_FLOAT_64)) {
            result = Common.readFloat64(value, offset, formatLength);
        }
        return result;
    }

    // Reads enum for given value
    private int readEnumInt(int index, int size, int val) {
        int result = 0;
        for (int i = 0; i < size; i++) {
            result <<= 8;
            result |= ((val >> (index + i)) & 0x1);
        }
        return result;
    }

    // Sets value from offset position
    private void setValue(int off, byte[] val) {
        for (int i = off; i < val.length; i++) {
            value[i] = val[i];
        }
    }

    // Gets field offset in bytes
    private int getFieldOffset(Field searchField) {
        foundField = false;
        int off = 0;
        for (Field field : mCharact.getFields()) {
            off += getOffset(field, searchField);
        }
        foundField = true;

        return off;
    }

    private boolean foundField = false;

    // Gets field offset when field has references to other fields
    private int getOffset(Field field, Field searchField) {
        int off = 0;
        if (field == searchField) {
            foundField = true;
            return off;
        }
        if (!foundField && isFieldPresent(field)) {
            if (field.getReferenceFields().size() > 0) {
                for (Field subField : field.getReferenceFields()) {
                    off += getOffset(subField, searchField);
                }
            } else {
                if (field.getFormat() != null) {
                    off += Engine.getInstance().getFormat(field.getFormat());
                }
            }
        }
        return off;
    }

    // Gets all bit fields for this characteristic
    private ArrayList<Field> getBitFields() {
        ArrayList<Field> bitFields = new ArrayList<Field>();
        for (Field field : mCharact.getFields()) {
            bitFields.addAll(getBitField(field));
        }
        return bitFields;
    }

    // Gets bit field when field has references to other fields
    private ArrayList<Field> getBitField(Field field) {
        ArrayList<Field> bitFields = new ArrayList<Field>();
        if (field.getBitfield() != null) {
            bitFields.add(field);
        } else if (field.getReferenceFields().size() > 0) {
            for (Field subField : field.getReferenceFields()) {
                bitFields.addAll(getBitField(subField));
            }
        }
        return bitFields;
    }

    /*
     * 
     * --- UI SECTION
     */

    // Builds activity UI if characteristic is not standard characteristic (from
    // Bluetooth specifications)
    private void addRawValue() {
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.characteristic_value, null);

        hex = (TextView) view.findViewById(R.id.hex);
        ascii = (TextView) view.findViewById(R.id.ascii);
        decimal = (TextView) view.findViewById(R.id.decimal);

        hexEdit = (EditText) view.findViewById(R.id.hexEdit);
        asciiEdit = (EditText) view.findViewById(R.id.asciiEdit);
        decimalEdit = (EditText) view.findViewById(R.id.decimalEdit);

        TextWatcher hexWatcher = getHexTextWatcher();
        TextWatcher decWatcher = getDecTextWatcher();
        TextWatcher asciiWatcher = getAsciiTextWatcher();

        OnFocusChangeListener hexListener = getHexFocusChangeListener();

        hexEdit.setOnFocusChangeListener(hexListener);
        WriteCharacteristic commiter = new WriteCharacteristic();
        hexEdit.setOnEditorActionListener(commiter);
        asciiEdit.setOnEditorActionListener(commiter);
        decimalEdit.setOnEditorActionListener(commiter);

        hexEdit.addTextChangedListener(hexWatcher);
        asciiEdit.addTextChangedListener(asciiWatcher);
        decimalEdit.addTextChangedListener(decWatcher);

        if (writeable) {
            hex.setVisibility(View.INVISIBLE);
            ascii.setVisibility(View.INVISIBLE);
            decimal.setVisibility(View.INVISIBLE);

            hexEdit.setText(Converters.getHexValue(value));
            asciiEdit.setText(Converters.getAsciiValue(value));
            decimalEdit.setText(Converters.getDecimalValue(value));

        } else {
            hexEdit.setVisibility(View.INVISIBLE);
            asciiEdit.setVisibility(View.INVISIBLE);
            decimalEdit.setVisibility(View.INVISIBLE);

            hex.setText(Converters.getHexValue(value));
            ascii.setText(Converters.getAsciiValue(value));
            decimal.setText(Converters.getDecimalValue(value));
        }

        valuesLayout.addView(view);
    }

    // Gets text watcher for hex edit view
    private TextWatcher getHexTextWatcher() {
        TextWatcher watcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (hexEdit.hasFocus()) {
                    int textLength = hexEdit.getText().toString().length();

                    byte newValue[];
                    if (textLength % 2 == 1) {
                        String temp = hexEdit.getText().toString();
                        temp = temp.substring(0, textLength - 1) + "0" + temp.charAt(textLength - 1);
                        newValue = hexToByteArray(temp.replaceAll("\\s+", ""));
                    } else {
                        newValue = hexToByteArray(hexEdit.getText().toString().replaceAll("\\s+", ""));
                    }
                    asciiEdit.setText(Converters.getAsciiValue(newValue));
                    decimalEdit.setText(Converters.getDecimalValue(newValue));
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        return watcher;
    }

    // Gets text watcher for decimal edit view
    private TextWatcher getDecTextWatcher() {
        TextWatcher watcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (decimalEdit.hasFocus()) {
                    if (isDecValueValid(decimalEdit.getText().toString())) {
                        byte newValue[] = decToByteArray(decimalEdit.getText().toString());
                        hexEdit.setText(Converters.getHexValue(newValue));
                        asciiEdit.setText(Converters.getAsciiValue(newValue));
                    } else {
                        decimalEdit.setText(decimalEdit.getText().toString().substring(0,
                                decimalEdit.getText().length() - 1));
                        decimalEdit.setSelection(decimalEdit.getText().length());
                        Toast.makeText(CharacteristicActivity.this, R.string.invalid_dec_value, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        return watcher;
    }

    // Gets text watcher for ascii edit view
    private TextWatcher getAsciiTextWatcher() {
        TextWatcher watcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (asciiEdit.hasFocus()) {
                    byte newValue[] = asciiEdit.getText().toString().getBytes();
                    hexEdit.setText(Converters.getHexValue(newValue));
                    decimalEdit.setText(Converters.getDecimalValue(newValue));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        return watcher;
    }

    // Gets focus listener for hex edit view
    private OnFocusChangeListener getHexFocusChangeListener() {
        OnFocusChangeListener listener = new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    hexEdit.setText(hexEdit.getText().toString().replaceAll("\\s+", ""));
                } else {
                    int textLength = hexEdit.getText().toString().length();
                    String hexValue;
                    if (textLength % 2 == 1) {
                        String temp = hexEdit.getText().toString();
                        hexValue = temp.substring(0, textLength - 1) + "0" + temp.charAt(textLength - 1);
                    } else {
                        hexValue = hexEdit.getText().toString();
                    }
                    byte value[] = hexToByteArray(hexValue);
                    hexEdit.setText(Converters.getHexValue(value));
                }
            }
        };
        return listener;
    }

    // Adds views related to single field value
    private void addValue(final Field field) {

        RelativeLayout parentLayout = new RelativeLayout(this);
        LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0, defaultMargin, 0, defaultMargin);
        parentLayout.setLayoutParams(parentParams);

        LinearLayout valueLayout = addValueLayout();
        TextView fieldNameView = addValueFieldName(field.getName(), valueLayout.getId());
        TextView fieldUnitView = addValueUnit(field);

        if (!parseProblem && field.getReference() == null) {

            String format = field.getFormat();
            String val = readNextValue(format);

            if (writeable) {
                EditText fieldValueEdit = addValueEdit(field, val);

                valueLayout.addView(fieldValueEdit);
            } else {
                TextView fieldValueView = addValueText(val);

                valueLayout.addView(fieldValueView);
            }
        }

        valueLayout.addView(fieldUnitView);

        parentLayout.addView(valueLayout);
        parentLayout.addView(fieldNameView);

        valuesLayout.addView(parentLayout);
        valuesLayout.addView(addHorizontalLine(convertPxToDp(1)));
    }

    // Adds parent layout for normal value
    private LinearLayout addValueLayout() {
        LinearLayout valueLayout = new LinearLayout(this);
        RelativeLayout.LayoutParams valueLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        valueLayoutParams.setMargins(0, defaultMargin, defaultMargin, 0);
        valueLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        valueLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        valueLayout.setLayoutParams(valueLayoutParams);
        valueLayout.setOrientation(LinearLayout.HORIZONTAL);
        valueLayout.setId(2);

        return valueLayout;
    }

    // Adds unit text view
    private TextView addValueUnit(Field field) {
        TextView fieldUnitView = new TextView(this);
        LinearLayout.LayoutParams fieldUnitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldUnitParams.setMargins(defaultMargin, 0, 0, 0);
        fieldUnitView.setLayoutParams(fieldUnitParams);
        fieldUnitView.setTextColor(getResources().getColor(R.color.BluegigaDarkGrey));
        fieldUnitView.setTextSize(16);

        Unit unit = Engine.getInstance().getUnit(field.getUnit());
        if (unit != null) {
            if (unit.getSymbol() != "") {
                fieldUnitView.setText(unit.getSymbol());
            } else {
                fieldUnitView.setText(unit.getFullName());
            }
        }

        return fieldUnitView;
    }

    // Adds value edit view
    private EditText addValueEdit(final Field field, String value) {
        final EditText fieldValueEdit = new EditText(this);
        LinearLayout.LayoutParams fieldValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        fieldValueEdit.setLayoutParams(fieldValueParams);
        fieldValueEdit.setTextColor(getResources().getColor(R.color.BluegigaDarkGrey));
        fieldValueEdit.setTextSize(16);
        fieldValueEdit.setText(value);

        int formatLength = Engine.getInstance().getFormat(field.getFormat());
        final byte[] valArr = new byte[formatLength];

        fieldValueEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Arrays.fill(valArr, (byte) 0);
                byte[] newVal = fieldValueEdit.getText().toString().getBytes();

                for (int i = 0; i < valArr.length; i++) {
                    if (i < newVal.length) {
                        valArr[i] = newVal[i];
                    }
                }
                int off = getFieldOffset(field);

                setValue(off, valArr);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return fieldValueEdit;
    }

    // Adds value text view
    private TextView addValueText(String value) {
        TextView fieldValueView = new TextView(this);
        LinearLayout.LayoutParams fieldValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        fieldValueView.setLayoutParams(fieldValueParams);
        fieldValueView.setTextColor(getResources().getColor(R.color.BluegigaDarkGrey));
        fieldValueView.setTextSize(16);

        fieldValueView.setText(value);
        return fieldValueView;
    }

    // Adds TextView with field name
    private TextView addValueFieldName(String name, int leftViewId) {
        TextView fieldNameView = new TextView(this);

        fieldNameView.setText(name);
        fieldNameView.setTextColor(getResources().getColor(R.color.BluegigaDarkGrey));
        fieldNameView.setTextSize(18);

        RelativeLayout.LayoutParams fieldNameParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        fieldNameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        fieldNameParams.addRule(RelativeLayout.LEFT_OF, leftViewId);
        fieldNameParams.addRule(RelativeLayout.CENTER_VERTICAL);
        fieldNameParams.setMargins(defaultMargin, 0, defaultMargin, 0);

        fieldNameView.setLayoutParams(fieldNameParams);

        return fieldNameView;
    }

    // Adds views related to bitfield value
    // Each bit is presented as CheckBox view
    private void addBitfield(Field field) {

        valuesLayout.addView(addFieldName(field.getName()));

        if (field.getReference() == null) {

            String format = field.getFormat();
            final int formatLength = Engine.getInstance().getFormat(format);
            final int off = getFieldOffset(field);
            final int fieldValue = readNextEnum(formatLength);

            for (final Bit bit : field.getBitfield().getBits()) {
                RelativeLayout parentLayout = new RelativeLayout(this);
                parentLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                LinearLayout bitsLayout = new LinearLayout(this);
                bitsLayout.setId(1);
                RelativeLayout.LayoutParams bitsLayoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                bitsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                bitsLayout.setLayoutParams(bitsLayoutParams);
                bitsLayout.setOrientation(LinearLayout.HORIZONTAL);

                TextView bitNameView = new TextView(this);
                RelativeLayout.LayoutParams bitNameParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                bitNameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                bitNameParams.addRule(RelativeLayout.LEFT_OF, bitsLayout.getId());
                bitNameParams.setMargins(defaultMargin, 0, 0, 0);
                bitNameView.setLayoutParams(bitNameParams);
                bitNameView.setText(bit.getName());
                bitNameView.setTextColor(getResources().getColor(R.color.BluegigaDarkGrey));

                for (int i = 0; i < Math.pow(2, bit.getSize() - 1); i++) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

                    if (!parseProblem) {
                        checkBox.setEnabled(writeable);
                        checkBox.setChecked(Common.isBitSet(bit.getIndex() + i, fieldValue));

                        final int whichBit = i;

                        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                int newVal = Common.toggleBit(bit.getIndex() + whichBit, fieldValue);
                                byte[] val = intToByteArray(newVal, formatLength);
                                setValue(off, val);
                            }

                        });
                    } else {
                        checkBox.setEnabled(false);
                    }

                    bitsLayout.addView(checkBox);
                }

                parentLayout.addView(bitNameView);
                parentLayout.addView(bitsLayout);

                valuesLayout.addView(parentLayout);
            }
        }
        valuesLayout.addView(addHorizontalLine(convertPxToDp(1)));
    }

    // Adds views related to enumeration value
    // Each enumeration is presented as Spinner view
    private void addEnumeration(final Field field) {
        valuesLayout.addView(addFieldName(field.getName()));
        if (field.getReference() == null) {

            ArrayList<String> enumerationArray = new ArrayList<String>();

            for (Enumeration en : field.getEnumerations()) {
                enumerationArray.add(en.getValue());
            }

            Spinner spinner = new Spinner(this);
            LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            spinnerParams.setMargins(defaultMargin, 0, defaultMargin, 0);
            spinner.setLayoutParams(spinnerParams);
            spinner.setMinimumHeight(convertPxToDp(40));

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item,
                    R.id.spinnerText, enumerationArray);

            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setBackground(getResources().getDrawable(R.drawable.spinner_selector));
            spinner.setPadding(defaultMargin, 0, defaultMargin, 0);
            if (!parseProblem) {
                spinner.setEnabled(writeable);

                int off = getFieldOffset(field);
                int formatLength = Engine.getInstance().getFormat(field.getFormat());

                int pos = 0;
                int val = readInt(off, formatLength);

                if (val != 0) {
                    // value was read or notified
                    for (Enumeration en : field.getEnumerations()) {
                        if (en.getKey() == val) {
                            break;
                        }
                        pos++;
                    }
                }
                if (pos >= spinner.getAdapter().getCount()) {
                    pos = 0;
                }
                spinner.setSelection(pos);

                spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        int key = field.getEnumerations().get(position).getKey();
                        int off = getFieldOffset(field);
                        int formatLength = Engine.getInstance().getFormat(field.getFormat());
                        byte val[] = intToByteArray(key, formatLength);
                        setValue(off, val);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // your code here
                    }

                });
            } else {
                spinner.setEnabled(false);
            }
            valuesLayout.addView(spinner);
        }

        valuesLayout.addView(addHorizontalLine(convertPxToDp(1)));

    }

    // Adds TextView with error info
    // Called when characteristic parsing error occured
    private void addProblemInfoView() {
        TextView problemTextView = new TextView(this);
        LinearLayout.LayoutParams fieldValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldValueParams.setMargins(defaultMargin, 0, 0, 0);

        problemTextView.setLayoutParams(fieldValueParams);
        problemTextView.setTextColor(getResources().getColor(R.color.BluegigaBlue));
        problemTextView.setTypeface(Typeface.DEFAULT_BOLD);
        problemTextView.setTextSize(20);

        problemTextView.setText(getText(R.string.parse_problem));
        valuesLayout.addView(problemTextView);
        valuesLayout.addView(addHorizontalLine(convertPxToDp(1)));
    }

    // Adds TextView with field name
    private View addFieldName(String name) {
        TextView fieldNameView = new TextView(this);
        LinearLayout.LayoutParams fieldNameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldNameParams.setMargins(defaultMargin, 0, 0, 0);
        fieldNameView.setLayoutParams(fieldNameParams);
        fieldNameView.setText(name);
        fieldNameView.setTextColor(getResources().getColor(R.color.BluegigaDarkGrey));
        fieldNameView.setTextSize(18);
        return fieldNameView;
    }

    // Adds horizontal line to separate UI sections
    private View addHorizontalLine(int height) {
        View horizontalLine = new View(this);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                height);
        lineParams.setMargins(0, convertPxToDp(5), 0, 0);
        horizontalLine.setLayoutParams(lineParams);
        horizontalLine.setBackgroundColor(getResources().getColor(R.color.BluegigaDarkGrey));
        return horizontalLine;
    }

    // Converts pixels to 'dp' unit
    private int convertPxToDp(int sizeInPx) {
        float scale = getResources().getDisplayMetrics().density;
        int sizeInDp = (int) (sizeInPx * scale + 0.5f);
        return sizeInDp;
    }

    class WriteCharacteristic implements OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                writeValueToCharacteristic();
                return true;
            }
            return false;
        }
    }
}
