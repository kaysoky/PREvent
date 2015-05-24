package com.prevent;

import android.app.Activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;

public class NavigationActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private final static String TAG = NavigationActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
            getFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
            (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Fragment objFragment = null;
        String title = null;
        switch (position){
            case 0:
                objFragment = new NowFragment();
                title = getString(R.string.title_now_section);
                break;
            case 1:
                objFragment = new NowFragment();
                title = getString(R.string.title_day_section);
                break;
            case 2:
                objFragment = new NowFragment();
                title = getString(R.string.title_cumulative_section);
                break;
            case 3:
                objFragment = new NowFragment();
                title = getString(R.string.title_website_section);
                break;
            default:
                Log.e(TAG, "Invalid navigation drawer item selected");
                break;
        }

        // Replace the content container with the selected fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
            .replace(R.id.container, objFragment)
            .commit();
        getActionBar().setTitle(title);
    }
}
