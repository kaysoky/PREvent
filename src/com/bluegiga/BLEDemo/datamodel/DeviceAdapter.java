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

import java.util.Vector;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bluegiga.BLEDemo.R;

// DeviceAdapter - used to build up device list in MainActivity
public class DeviceAdapter extends BaseAdapter {

    private Context context;
    private Vector<Device> devices;

    public DeviceAdapter(Context context, Vector<Device> devices) {
        this.context = context;
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int arg0) {
        return devices.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item_device, null);
        }

        RelativeLayout itemLayout = (RelativeLayout) convertView.findViewById(R.id.gridDeviceLayout);

        LinearLayout advertisementLayout = (LinearLayout) convertView.findViewById(R.id.advertisementDataLayout);
        TextView deviceName = (TextView) convertView.findViewById(R.id.deviceName);
        TextView deviceMacAddress = (TextView) convertView.findViewById(R.id.deviceMacAddress);
        TextView rssi = (TextView) convertView.findViewById(R.id.rssi);

        Device bluetoothDevice = devices.get(position);
        if (bluetoothDevice.isConnected()) {
            itemLayout.setBackgroundResource(R.drawable.round_corner_conn);
        } else {
            itemLayout.setBackgroundResource(R.drawable.round_corner_disc);
        }

        deviceName.setText(bluetoothDevice.getName());
        if (bluetoothDevice.getAddress() == null || bluetoothDevice.getAddress().isEmpty()) {
            deviceMacAddress.setText(context.getText(R.string.unknown_device));
        } else {
            deviceMacAddress.setText(bluetoothDevice.getAddress());
        }
        rssi.setText(Integer.toString(bluetoothDevice.getRssi()));

        advertisementLayout.removeAllViews();

        for (int i = 0; i < bluetoothDevice.getAdvertData().size(); i++) {
            String data = bluetoothDevice.getAdvertData().get(i);
            String[] advertiseData = data.split(":");
            advertisementLayout.addView(createAdvertiseView(bluetoothDevice, advertiseData[0], advertiseData[1]));

            if (i == Device.MAX_EXTRA_DATA - 1) {
                break;
            }
        }

        return convertView;
    }

    // Creates content view for advertise data of BLE device
    private TableRow createAdvertiseView(final Device blueetoothDevice, String label, String data) {

        TableRow tableRow = new TableRow(context);
        tableRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tableRow.setPadding(0, 0, 0, 0);

        TextView labelView = new TextView(context);

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT, 1.0f);

        labelView.setLayoutParams(params);
        labelView.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        labelView.setTextColor(context.getResources().getColor(R.color.BluegigaWhite));
        labelView.setTextSize(14);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);

        labelView.setText(label + ":");

        TableRow.LayoutParams dataParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT, 1.0f);

        final TextView dataText = new TextView(context);
        dataText.setLayoutParams(dataParams);
        dataText.setGravity(Gravity.BOTTOM | Gravity.LEFT);
        dataText.setTextColor(context.getResources().getColor(R.color.BluegigaWhite));
        dataText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        dataText.setSingleLine(true);
        dataText.setEllipsize(TextUtils.TruncateAt.END);

        dataText.setText(data);

        ViewTreeObserver vto = dataText.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Layout layout = dataText.getLayout();
                if (layout.getEllipsisCount(0) > 0) {
                    blueetoothDevice.setAdvertDetails(true);
                }
            }
        });

        tableRow.addView(labelView);
        tableRow.addView(dataText);
        return tableRow;
    }

}
