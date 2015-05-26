package com.prevent;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.Math;

/**
 * Shows only the most recent sensor reading
 */
public class NowFragment extends DisplayFragment {
    @Override
    protected void fetchLatestData() {
        SharedPreferences storage = getActivity()
            .getSharedPreferences(BluetoothLeService.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        temp = storage.getFloat(BluetoothLeService.RECENT_TEMP_DATA_KEY, 0.0f);
        humi = storage.getFloat(BluetoothLeService.RECENT_HUMI_DATA_KEY, 0.0f);
        vocs = storage.getFloat(BluetoothLeService.RECENT_VOCS_DATA_KEY, 0.0f);
        part = storage.getFloat(BluetoothLeService.RECENT_PART_DATA_KEY, 0.0f);
    }
}
