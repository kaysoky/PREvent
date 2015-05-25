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
    
    /**
     * Fetches the latest sensor data and updates the text views
     */
    @Override
    protected void updateTextViews() {
        SharedPreferences storage = getActivity()
            .getSharedPreferences(BluetoothLeService.SHARED_PREFERENCES_NAME, 
                Context.MODE_PRIVATE);
        float temp = storage.getFloat(BluetoothLeService.RECENT_TEMP_DATA_KEY, 0.0f);
        float humi = storage.getFloat(BluetoothLeService.RECENT_HUMI_DATA_KEY, 0.0f);
        float vocs = storage.getFloat(BluetoothLeService.RECENT_VOCS_DATA_KEY, 0.0f);
        float part = storage.getFloat(BluetoothLeService.RECENT_PART_DATA_KEY, 0.0f);
        temp_view.setText(getText(R.string.temp_text_label) + String.format("%.2f", temp) + "C");
        humi_view.setText(getText(R.string.humi_text_label) + String.format("%.2f", humi) + "%");
        vocs_view.setText(getText(R.string.vocs_text_label) + String.format("%.2f", vocs) + "%");
        part_view.setText(getText(R.string.part_text_label) + String.format("%.2f", part) + "%");
        temp_view.setBackgroundColor(getAssociatedColor(Math.abs(temp - 20) / 35.0f));
        humi_view.setBackgroundColor(getAssociatedColor(humi / 100.0f));
        vocs_view.setBackgroundColor(getAssociatedColor(vocs / 100.0f));
        part_view.setBackgroundColor(getAssociatedColor(part / 100.0f));
    }
}
