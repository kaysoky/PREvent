package com.prevent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Login screen used to check the username/password combination before uploading data
 */
public class LoginActivity extends Activity {
    private final static String TAG = LoginActivity.class.getSimpleName();

    private final static String AUTHENTICATION_ENDPOINT = "http://attu.cs.washington.edu:8000/auth/";

    private EditText usernameView;
    private EditText passwordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        usernameView = (EditText)findViewById(R.id.usernameInput);
        passwordView = (EditText)findViewById(R.id.passwordInput);
    }

    /**
     * Spawns a thread to check the login credentials
     */
    public void onloginSubmitClick(View view){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                submitLoginInfo();
            }
        });

        thread.start();
    }

    /**
     * Helper for making an HTTP GET to the authentication check endpoint
     */
    public void submitLoginInfo() {
        // Fetch the credentials
        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet HttpGet = new HttpGet(AUTHENTICATION_ENDPOINT);
            HttpGet.setHeader("Accept", "application/json");
            HttpGet.setHeader("Authorization", getB64Auth(username, password));

            HttpResponse httpResponse = httpclient.execute(HttpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                Log.i(TAG, "Login successful");
                Intent goToMainPage = new Intent(this, DeviceScanActivity.class);
                startActivity(goToMainPage);
            } else {
                Log.i(TAG, "Error logging in");
                showLoginErrorDialog();
            }
        } catch (IOException e) {
            Log.i(TAG, "Exception while logging in", e);
        }
    }

    /**
     * Helper for encoding the login credentials into HTTP Basic Authorization format
     */
    public String getB64Auth(String username, String password) {
        String source = username + ":" + password;
        return "Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * Helper for showing a dialog upon login failure
     */
    public void showLoginErrorDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getApplicationContext());

        alertDialogBuilder.setTitle(getText(R.string.login_fail_title));
        alertDialogBuilder
            .setMessage(getText(R.string.login_fail_message))
                .setCancelable(false)
                .setPositiveButton(getText(R.string.ok_text), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
