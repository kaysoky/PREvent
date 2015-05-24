package com.prevent;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

/**
 * Shows a static welcome page
 */
public class WelcomeActivity extends Activity {

    public void onContinueClick(View view) {
        // Check for a prior login
        if (getSharedPreferences(LoginActivity.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                .getBoolean(LoginActivity.AUTHENTICATION_CHECKED_KEY, false)) {
            Intent goToLoginPage = new Intent(this, DeviceScanActivity.class);
            startActivity(goToLoginPage);
        } else {
            onLoginClick(view);
        }
    }

    public void onLoginClick(View view) {
        Intent goToLoginPage = new Intent(this, LoginActivity.class);
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
