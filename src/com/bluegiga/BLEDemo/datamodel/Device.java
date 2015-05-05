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
package com.bluegiga.BLEDemo.datamodel;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

// Device - it's wrapper for BLE device object
public class Device {
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private boolean connected;
    private boolean hasAdvertDetails;
    private int rssi;
    private ArrayList<String> advertData;

    public static int MAX_EXTRA_DATA = 3;

    public Device() {

    }

    public Device(BluetoothDevice bluetoothDevice, int rssi, ArrayList<String> advertisements) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.connected = false;
        this.hasAdvertDetails = false;
        this.bluetoothGatt = null;
        this.advertData = advertisements;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public void setAdvertData(ArrayList<String> advertisements) {
        this.advertData = advertisements;
    }

    public ArrayList<String> getAdvertData() {
        return advertData;
    }

    public boolean hasAdvertDetails() {
        return (hasAdvertDetails || advertData.size() > MAX_EXTRA_DATA);
    }

    public void setAdvertDetails(boolean hasAdvertDetails) {
        this.hasAdvertDetails = hasAdvertDetails;
    }

}
