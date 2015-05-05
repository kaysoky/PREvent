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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.NodeAlreadyInTreeException;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bluegiga.BLEDemo.BluetoothLeService;
import com.bluegiga.BLEDemo.CharacteristicActivity;
import com.bluegiga.BLEDemo.Dialogs;
import com.bluegiga.BLEDemo.R;
import com.bluegiga.BLEDemo.datamodel.xml.Characteristic;
import com.bluegiga.BLEDemo.datamodel.xml.Service;

//ServiceCharacteristicAdapter - used to build up TreeView component in ServiceCharacteristicActivity
@SuppressLint("UseSparseArrays")
public class ServiceCharacteristicAdapter extends AbstractTreeViewAdapter<Integer> {

    private TreeBuilder<Integer> treeBuilder;
    private HashMap<Integer, BluetoothGattService> services;
    private HashMap<Integer, BluetoothGattCharacteristic> characteristics;
    private Device device;
    private Activity activity;

    public ServiceCharacteristicAdapter(Activity activity, TreeStateManager<Integer> treeStateManager,
            TreeBuilder<Integer> treeBuilder, int numberOfLevels, HashMap<Integer, BluetoothGattService> services,
            Device device) {
        super(activity, treeStateManager, numberOfLevels);

        this.treeBuilder = treeBuilder;
        this.services = services;
        this.device = device;
        this.activity = activity;
        this.characteristics = new HashMap<Integer, BluetoothGattCharacteristic>();

        resetTreeView();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getNewChildView(TreeNodeInfo<Integer> treeNodeInfo) {
        LinearLayout viewLayout;

        if (treeNodeInfo.getLevel() == 0) {
            viewLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.list_item_service, null);
        } else {
            viewLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.list_item_characteristic,
                    null);
        }
        return updateView(viewLayout, treeNodeInfo);
    }

    @Override
    public View updateView(View view, TreeNodeInfo<Integer> treeNodeInfo) {
        final LinearLayout viewLayout = (LinearLayout) view;
        if (treeNodeInfo.getLevel() == 0) {
            final TextView serviceNameView = (TextView) viewLayout.findViewById(R.id.serviceName);
            final TextView serviceUuidView = (TextView) viewLayout.findViewById(R.id.serviceUuid);

            UUID uuid = services.get(treeNodeInfo.getId()).getUuid();
            Service service = Engine.getInstance().getService(uuid);
            if (service != null) {
                serviceNameView.setText(service.getName().trim());
            } else {
                serviceNameView.setText(getActivity().getText(R.string.unknown_service));
            }
            serviceUuidView.setText(Common.getUuidText(uuid));
        } else {
            final TextView charactNameView = (TextView) viewLayout.findViewById(R.id.characteristicName);
            final TextView charactUuidView = (TextView) viewLayout.findViewById(R.id.characteristicUuid);
            final TextView charactPropertiesView = (TextView) viewLayout.findViewById(R.id.characteristicProperties);

            UUID uuid = characteristics.get(treeNodeInfo.getId()).getUuid();
            Characteristic charact = Engine.getInstance().getCharacteristic(uuid);
            if (charact != null) {
                charactNameView.setText(charact.getName().trim());
            } else {
                charactNameView.setText(getActivity().getText(R.string.unknown_characteristic));
            }
            charactUuidView.setText(Common.getUuidText(uuid));

            BluetoothGattCharacteristic bluetoothCharact = characteristics.get(treeNodeInfo.getId());
            charactPropertiesView.setText(getActivity().getText(R.string.properties_big_case) + " "
                    + Common.getProperties(getActivity(), bluetoothCharact.getProperties()));
        }

        return viewLayout;
    }

    @Override
    public void handleItemClick(final View view, final Object id) {
        final Integer index = (Integer) id;
        final TreeNodeInfo<Integer> info = getManager().getNodeInfo(index);
        if (info.isWithChildren()) {
            super.handleItemClick(view, id);
        } else if (info.getLevel() == 0) { // if user clicked on service

            resetTreeView();

            // sort services by uuids
            BluetoothGattService service = services.get(index);
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            Collections.sort(characteristics, new Comparator<BluetoothGattCharacteristic>() {

                @Override
                public int compare(BluetoothGattCharacteristic lhs, BluetoothGattCharacteristic rhs) {
                    return lhs.getUuid().compareTo(rhs.getUuid());
                }

            });

            // add characteristic items to service
            int charactIndex = services.size();
            for (BluetoothGattCharacteristic charact : characteristics) {
                this.characteristics.put(charactIndex, charact);

                if (!addCharacteristicRelation(charactIndex, index)) {
                    break;
                }
                charactIndex++;
            }

        } else { // if user clicked on characteristic
            Engine.getInstance().setLastCharacteristic(characteristics.get(index));
            Intent intent = new Intent(getActivity(), CharacteristicActivity.class);
            intent.putExtra(BluetoothLeService.DEVICE_ADDRESS, device.getAddress());
            getActivity().startActivity(intent);
        }
    }

    // Clears whole UI and adds only services
    private void resetTreeView() {
        int index = 0;
        treeBuilder.clear();
        characteristics.clear();

        for (int i = 0; i < services.size(); i++) {
            addServiceRelation(index);
            index++;
        }

    }

    private void showUnknownErrorMessage(DialogInterface.OnClickListener okClickListener) {
        Dialogs.showAlert(activity.getText(R.string.tree_view_unknown_error_title_text), activity
                .getText(R.string.tree_view_unknown_error_text), activity, activity.getText(android.R.string.ok),
                activity.getText(android.R.string.cancel), okClickListener, null);
    }

    private void showUuidsErrorMessage(DialogInterface.OnClickListener okClickListener) {
        Dialogs.showAlert(activity.getText(R.string.invalid_uuids_title_text), activity
                .getText(R.string.invalid_uuids_text), activity, activity.getText(android.R.string.ok), activity
                .getText(android.R.string.cancel), okClickListener, null);
    }

    private void addServiceRelation(int index) {
        try {
            treeBuilder.addRelation(null, index);
        } catch (NodeAlreadyInTreeException ex) {

            showUuidsErrorMessage(new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    dialog.dismiss();
                    activity.finish();
                }
            });
        } catch (Exception ex) {
            showUnknownErrorMessage(new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    dialog.dismiss();
                    activity.finish();
                }
            });
        }
    }

    private boolean addCharacteristicRelation(int index, int parentIndex) {
        try {
            treeBuilder.addRelation(parentIndex, index);
            return true;
        } catch (NodeAlreadyInTreeException ex) {

            showUuidsErrorMessage(new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    dialog.dismiss();
                    resetTreeView();
                }
            });

        } catch (Exception ex) {
            showUnknownErrorMessage(new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    dialog.dismiss();
                    activity.finish();
                }
            });
        }
        return false;
    }
}
