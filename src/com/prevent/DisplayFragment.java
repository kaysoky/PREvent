package com.prevent;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.Math;

/**
 * Shows only the most recent sensor reading
 */
public abstract class DisplayFragment extends Fragment {
    protected TextView temp_view;
    protected TextView humi_view;
    protected TextView vocs_view;
    protected TextView part_view;

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
     * Should fetch the sensor data and update the text views
     */
    protected abstract void updateTextViews();
    
    /**
     * Interpolates between green and red based on the given value (0-1)
     */
    public int getAssociatedColor(float value) {
        double lerp = Math.max(0.0, Math.min(1.0, value));
        int green = ((int)(0xFF * lerp)) & 0xFF;
        int red = ((int)(0xFF * (1.0 - lerp))) & 0xFF;
        return (0xFF << 24) | green << 16 | red << 8;
    }

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
