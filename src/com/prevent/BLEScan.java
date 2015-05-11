package com.prevent;

import android.app.Activity;
import android.os.Bundle;

public class BLEScan extends Activity {
    public static final int SCAN_PERIOD = 10000;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bluetooth LE is required by the Android Manifest
        // No check is necessary here

        setContentView(R.layout.main);
    }
}
