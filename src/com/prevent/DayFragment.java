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

/**
 * Shows a 24-hour moving average of recorded sensor values
 */
public class DayFragment extends DisplayFragment {
    @Override
    protected void fetchLatestData() {
        SharedPreferences storage = getActivity()
            .getSharedPreferences(BluetoothLeService.SHARED_PREFERENCES_NAME, 
                Context.MODE_PRIVATE);
        temp = storage.getFloat(BluetoothLeService.CUMULATIVE_TEMP_DATA_KEY, 0.0f) / BluetoothLeService.MOVING_AVERAGE_SAMPLES;
        humi = storage.getFloat(BluetoothLeService.CUMULATIVE_HUMI_DATA_KEY, 0.0f) / BluetoothLeService.MOVING_AVERAGE_SAMPLES;
        vocs = storage.getFloat(BluetoothLeService.CUMULATIVE_VOCS_DATA_KEY, 0.0f) / BluetoothLeService.MOVING_AVERAGE_SAMPLES;
        part = storage.getFloat(BluetoothLeService.CUMULATIVE_PART_DATA_KEY, 0.0f) / BluetoothLeService.MOVING_AVERAGE_SAMPLES;
    }
}
