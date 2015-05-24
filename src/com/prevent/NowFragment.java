package com.prevent;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Shows only the most recent sensor reading
 */
public class NowFragment extends Fragment {
    private TextView humidity;
    private TextView temp;
    private TextView particulate;
    private TextView voc;
    View rootview;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.now_fragment_layout, container, false);
        return rootview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        humidity = (TextView) getActivity().findViewById(R.id.humidityReading);
        humidity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("onHumidityClick called");
                Toast.makeText(getActivity(), "Humidity level is relatively low, please drink more water and keep hydrated", Toast.LENGTH_SHORT).show();
            }
        });

        temp = (TextView) getActivity().findViewById(R.id.tempReading);
        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Summer is coming, enjoy", Toast.LENGTH_SHORT).show();
            }
        });

        particulate = (TextView) getActivity().findViewById(R.id.particulateReading);
        particulate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Particulate density is high, please consider changing your area of activity", Toast.LENGTH_SHORT).show();
            }
        });

        voc = (TextView) getActivity().findViewById(R.id.vocReading);
        voc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "No hazardous gases detected", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
