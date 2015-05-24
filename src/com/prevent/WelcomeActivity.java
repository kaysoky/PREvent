package com.prevent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Shows a static welcome page
 */
public class WelcomeActivity extends Activity {

    public void onLoginClick(View view) {
        Intent goToLoginPage = new Intent(this, DeviceScanActivity.class);
        startActivity(goToLoginPage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        
        // Start the background BLE service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(gattServiceIntent);
    }
}
