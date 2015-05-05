package com.example.amber.myfirstapp;

import android.content.Context;
import android.location.LocationManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Amber on 4/28/15.
 */
public class DataPack {
    String x;
    String y;
    String humidity;
    String temperature;
    String gas;
    String particulate;
    Date time;

    DataPack(String humidityIn, String temperatureIn, String gasIn, String particulateIn){
        //x = xIn;
        //y = yIn;
        humidity = humidityIn;
        temperature = temperatureIn;
        gas = gasIn;
        particulate = particulateIn;
        time = new Date();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    }

    void getLocation(){
        //LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    }

    void updateLocation(){

    }

}
