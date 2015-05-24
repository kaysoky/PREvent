package com.prevent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
    public final static String SHARED_PREFERENCES_NAME = "login_preferences";
    public final static String AUTHENTICATION_PREFERENCE_KEY = "basic_credentials_b64";
    public final static String AUTHENTICATION_CHECKED_KEY = "authentication_passed";

    private Activity context;
    private EditText usernameView;
    private EditText passwordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        context = this;
        usernameView = (EditText)findViewById(R.id.usernameInput);
        passwordView = (EditText)findViewById(R.id.passwordInput);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check for an internet connection
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnectedOrConnecting()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Spawns a thread to check the login credentials
     */
    public void onloginSubmitClick(View view){
        // Fetch the credentials
        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();
        String credentials = getB64Auth(username, password);
        new CredentialCheckTask().execute(credentials);

        SharedPreferences.Editor editor = 
            getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
        editor.putString(AUTHENTICATION_PREFERENCE_KEY, credentials);
        editor.commit();
    }

    /**
     * Helper for encoding the login credentials into HTTP Basic Authorization format
     */
    public String getB64Auth(String username, String password) {
        String source = username + ":" + password;
        return "Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * Used to offload the network IO onto a thread
     */
    private class CredentialCheckTask extends AsyncTask<String, Integer, Boolean> {
        protected Boolean doInBackground(String... credentials) {
            if (credentials.length != 1) {
                return false;
            }

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet HttpGet = new HttpGet(AUTHENTICATION_ENDPOINT);
                HttpGet.setHeader("Accept", "application/json");
                HttpGet.setHeader("Authorization", credentials[0]);

                HttpResponse httpResponse = httpclient.execute(HttpGet);
                return httpResponse.getStatusLine().getStatusCode() == 200;
            } catch (IOException e) {
                Log.i(TAG, "Exception while logging in", e);
            }

            return false;
        }

        protected void onPostExecute(Boolean result) {
            SharedPreferences.Editor editor = 
                context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();

            if (result) {
                Log.i(TAG, "Login successful");

                // Make a note that the login succeeded
                editor.putBoolean(AUTHENTICATION_CHECKED_KEY, true);

                Intent goToMainPage = new Intent(context, NavigationActivity.class);
                startActivity(goToMainPage);
            } else {
                Log.i(TAG, "Error logging in");

                // Make a note that the login failed
                editor.putBoolean(AUTHENTICATION_CHECKED_KEY, false);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
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

            editor.commit();
        }
    }
}
