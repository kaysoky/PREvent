package com.prevent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.provider.Settings;
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
    
    @Override
    protected void onResume() {
        super.onResume();
        
        LocationManager location = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!location.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final Context context = this;
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(getText(R.string.gps_fail_title));
            alertDialog.setMessage(getText(R.string.gps_fail_message));
            alertDialog.setPositiveButton(getText(R.string.ok_text), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                    finish();
                }
            });
            alertDialog.show();
        }
    }
}
