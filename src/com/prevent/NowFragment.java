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
public class NowFragment extends Fragment {
    private TextView temp_view;
    private TextView humi_view;
    private TextView vocs_view;
    private TextView part_view;

    /**
     * Receives data from the BluetoothLeService
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                updateTextViews();
            }
        }
    };
    
    /**
     * Fetches the latest sensor data and updates the text views
     */
    private void updateTextViews() {
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
    
    /**
     * Interpolates between green and red based on the given value (0-1)
     */
    private int getAssociatedColor(float value) {
        double lerp = Math.max(0.0, Math.min(1.0, value));
        int green = ((int)(0xFF * lerp)) & 0xFF;
        int red = ((int)(0xFF * (1.0 - lerp))) & 0xFF;
        return (0xFF << 24) | green << 16 | red << 8;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.now_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        temp_view = (TextView) getActivity().findViewById(R.id.tempReading);
        humi_view = (TextView) getActivity().findViewById(R.id.humidityReading);
        vocs_view = (TextView) getActivity().findViewById(R.id.vocReading);
        part_view = (TextView) getActivity().findViewById(R.id.particulateReading);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver, BluetoothLeService.getGattUpdateIntentFilter());
        updateTextViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mGattUpdateReceiver);
    }

    /***** Click handlers *****/

    public void onTemperatureClick(View view) {
        // Toast.makeText(getActivity(), "Summer is coming, enjoy", Toast.LENGTH_SHORT).show();
    }

    public void onHumidityClick(View view) {
        // Toast.makeText(getActivity(), "Humidity level is relatively low, please drink more water and keep hydrated", Toast.LENGTH_SHORT).show();
    }

    public void onVOCClick(View view) {
        // Toast.makeText(getActivity(), "No hazardous gases detected", Toast.LENGTH_SHORT).show();
    }

    public void onParticulateClick(View view) {
        // Toast.makeText(getActivity(), "Particulate density is high, please consider changing your area of activity", Toast.LENGTH_SHORT).show();
    }
}
