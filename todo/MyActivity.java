package com.example.amber.myfirstapp;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;


public class MyActivity extends ActionBarActivity {
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void enterData (){
        setContentView(R.layout.input_data);
    }

    void showStat(){
        setContentView(R.layout.stat);
    }

    void showMap(){

    }

    void login(){
        EditText usrnameW = (EditText)findViewById(R.id.usernameInput);
        username = usrnameW.getText().toString();
        EditText passwordW = (EditText)findViewById(R.id.passwordInput);
        password = passwordW.getText().toString();
    }

    public void submitData(){
        EditText humidityW = (EditText)findViewById(R.id.humidityInput);
        String humidity = humidityW.getText().toString();
        EditText temperatureW = (EditText)findViewById(R.id.temperatureInput);
        String temperature = temperatureW.getText().toString();
        EditText gasW = (EditText)findViewById(R.id.gasInput);
        String gas = gasW.getText().toString();
        EditText particulateW = (EditText)findViewById(R.id.particulateInput);
        String particulate = particulateW.getText().toString();

        DataPack myData = new DataPack(humidity, temperature, gas, particulate);
        //convert to JSON ???


        HttpClient httpclient = new DefaultHttpClient();
        //HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpPost request = new HttpPost("http://attu.cs.washington.edu:8000");
            StringEntity params =new StringEntity("http://attu.cs.washington.edu:8000/data/', data='<JSON DATA>');
            request.addHeader("content-type", "application/json",
                    "Authorization": "Basic %s" % Base64.encode("<USERNAME>:<PASSWORD>")});

        } catch (ClientProtocolException e) {

        } catch (IOException e) {

        }

        //ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }
}

/*
  requests.post('<ENDPOINT>/data/', data="<JSON DATA>",
  headers={"Content-Type": "application/json",
    'Authorization': 'Basic %s' % base64.b64encode('<USERNAME>:<PASSWORD>')})


        {
        "id": 1,
        "userid": "admin",
        "timestamp": "2015-04-09T14:30:00Z",
        "xcoord": 1.0,
        "ycoord": 2.0,
        "humidity": 3.0,
        "temperature": 4.0,
        "gas": 5.0,
        "particulate": 6.0
    }
*/
