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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.NodeAlreadyInTreeException;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bluegiga.BLEDemo.datamodel.Common;
import com.bluegiga.BLEDemo.datamodel.Consts;
import com.bluegiga.BLEDemo.datamodel.Device;
import com.bluegiga.BLEDemo.datamodel.Engine;
import com.bluegiga.BLEDemo.datamodel.ServiceCharacteristicAdapter;

// ServiceCharacteristicActivity - displays all discovered services on BLE device and characteristics related to
// It uses TreeView component where first level shows services and second level shows characteristics related to service
@SuppressLint("UseSparseArrays")
public class ServiceCharacteristicActivity extends Activity {

    private static IntentFilter bleIntentFilter;

    private TreeViewList treeView;
    private TreeStateManager<Integer> manager = null;

    private ServiceCharacteristicAdapter serviceCharacteristicAdapter;
    private TreeBuilder<Integer> treeBuilder = new TreeBuilder<Integer>(manager);
    private BluetoothLeService mBluetoothLeService;

    private Device device;

    // Implements receive methods that handle a specific intent actions from
    // mBluetoothLeService When ACTION_GATT_DISCONNECTED action is received and
    // device address equals current device, activity closes When
    // ACTION_GATT_SERVICES_DISCOVERED action is received, whole tree adapter is
    // rebuild
    private final BroadcastReceiver mBluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            String deviceAddress = intent.getExtras().getString(BluetoothLeService.DEVICE_ADDRESS);
            if (action.equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)) {
                if (deviceAddress.equals(device.getAddress())) {
                    finish();
                }
            } else if (action.equals(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)) {
                if (deviceAddress.equals(device.getAddress())) {
                    displayServices();
                }
            }
        }
    };

    // Implements callback method for service connection
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String address = getIntent().getExtras().getString(Consts.DEVICE_ADDRESS);
        device = Engine.getInstance().getDevice(address);

        manager = new InMemoryTreeStateManager<Integer>();
        treeBuilder = new TreeBuilder<Integer>(manager);

        registerReceiver(mBluetoothLeReceiver, getGattUpdateIntentFilter());

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_services_characteristics);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);

        final TextView titleBarView = (TextView) findViewById(R.id.titleBar);
        titleBarView.setText(getText(R.string.services));

        configureBanner();
        configureTreeView();
        configureLogo();

        displayServices();
    }

    // Configure tree view - deletes left side "+" signs
    private void configureTreeView() {
        treeView = (TreeViewList) findViewById(R.id.mainTreeView);
        treeView.getCollapsedDrawable().setAlpha(0);
        treeView.getExpandedDrawable().setAlpha(0);
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

    // Clears and fills tree adapter with services discovered on device
    private void displayServices() {
        int index = 0;
        treeBuilder.clear();
        ArrayList<BluetoothGattService> services = (ArrayList<BluetoothGattService>) device.getBluetoothGatt()
                .getServices();

        Collections.sort(services, new Comparator<BluetoothGattService>() {

            @Override
            public int compare(BluetoothGattService lhs, BluetoothGattService rhs) {
                return lhs.getUuid().compareTo(rhs.getUuid());
            }

        });

        LinkedHashMap<Integer, BluetoothGattService> servicesMap = new LinkedHashMap<Integer, BluetoothGattService>();
        for (BluetoothGattService service : services) {

            try {
                treeBuilder.addRelation(null, Integer.valueOf(index));
            } catch (NodeAlreadyInTreeException ex) {

                Dialogs.showAlert(this.getText(R.string.invalid_uuids_title_text), this
                        .getText(R.string.invalid_uuids_text), this, this.getText(android.R.string.ok), this
                        .getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                        ServiceCharacteristicActivity.this.finish();
                    }
                }, null);
            } catch (Exception ex) {
                Dialogs.showAlert(this.getText(R.string.tree_view_unknown_error_title_text), this
                        .getText(R.string.tree_view_unknown_error_text), this, this.getText(android.R.string.ok), this
                        .getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                        ServiceCharacteristicActivity.this.finish();
                    }
                }, null);
            }

            servicesMap.put(Integer.valueOf(index), service);
            index++;
        }

        serviceCharacteristicAdapter = new ServiceCharacteristicAdapter(this, manager, treeBuilder, 2, servicesMap,
                device);
        treeView.setAdapter(serviceCharacteristicAdapter);
    }

    // Returns intent filter for receiving specific action from
    // mBluetoothLeService:
    // - BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED - services were
    // discovered
    // - BluetoothLeService.ACTION_GATT_DISCONNECTED - device disconnected
    // This method is used when registerReceiver method is called
    private static IntentFilter getGattUpdateIntentFilter() {
        if (bleIntentFilter == null) {
            bleIntentFilter = new IntentFilter();
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
            bleIntentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        }
        return bleIntentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBluetoothLeReceiver, getGattUpdateIntentFilter());
        if (!device.isConnected()) {
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
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
}
