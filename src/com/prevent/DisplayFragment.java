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
import java.lang.CharSequence;

/**
 * Shows only the most recent sensor reading
 */
public abstract class DisplayFragment extends Fragment {
    private TextView temp_view;
    private TextView humi_view;
    private TextView vocs_view;
    private TextView part_view;

    protected float temp;
    protected float humi;
    protected float vocs;
    protected float part;

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
     * Should fetch the sensor data into local variables
     */
    protected abstract void fetchLatestData();

    private void updateTextViews() {
        fetchLatestData();

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
        // We won't comment on temperature
        // That's not the point of this product
    }

    public void onHumidityClick(View view) {
        // Just advise the user to keep the sensors dry
        if (humi > 50) {
            Toast.makeText(getActivity(), getText(R.string.humidity_high_toast),
                Toast.LENGTH_SHORT).show();
        }
    }

    public void onVOCClick(View view) {
        CharSequence message = null;
        if (vocs < 20) {
            message = getText(R.string.vocs_low_toast);
        } else if (vocs < 60) {
            message = getText(R.string.vocs_medium_toast);
        } else {
            message = getText(R.string.vocs_high_toast);
        }
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    public void onParticulateClick(View view) {
        CharSequence message = null;
        if (part < 20) {
            message = getText(R.string.pm_low_toast);
        } else if (part < 60) {
            message = getText(R.string.pm_medium_toast);
        } else {
            message = getText(R.string.pm_high_toast);
        }
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }
}
