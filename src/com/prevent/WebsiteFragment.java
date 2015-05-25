package com.prevent;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Holds an IFrame to the website
 */
public class WebsiteFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.website_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        WebView webView = (WebView)getActivity().findViewById(R.id.iframe);
        webView.loadUrl("http://attu.cs.washington.edu:8000/");
    }
}
