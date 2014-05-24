package com.nikhu.fitness.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by bujji on 01-05-2014.
 */
public class WalkFragment extends Fragment {

    private static final String TAG = "WalkFragment";

    private TextView tvSteps;
    private TextView tvAgeOfEvents;
    private int state = 0;

    private SupportMapFragment supportMapFragment = new SupportMapFragment() {
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            GoogleMap googleMap = supportMapFragment.getMap();
            if (googleMap != null) {
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(12.9667, 77.5667)));
                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                // Enabling MyLocation Layer of Google Map
                googleMap.setMyLocationEnabled(true);
                // Getting LocationManager object from System Service LOCATION_SERVICE
                LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                // Creating a criteria object to retrieve provider
                Criteria criteria = new Criteria();
                // Getting the name of the best provider
                String provider = locationManager.getBestProvider(criteria, true);
                // Getting Current Location
                Location location = locationManager.getLastKnownLocation(provider);

                if(location!=null) {
                    // Getting latitude of the current location
                    double latitude = location.getLatitude();
                    // Getting longitude of the current location
                    double longitude = location.getLongitude();
                    Log.i(TAG, "Last known location: Latitude:" + latitude + "; longitude:" + longitude);

                    // Creating a LatLng object for the current location
                    LatLng position = new LatLng(latitude, longitude);
                    googleMap.addMarker(new MarkerOptions().position(position).title("Start"));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(position));
                }
            }
        }
    };

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = layoutInflater.inflate(R.layout.fragment_walk, container, false);
        FragmentManager fm =  getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.map, supportMapFragment);
        ft.commit();

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        final SupportMapFragment supportMapFragment = (SupportMapFragment) fragmentManager
                .findFragmentById(R.id.map);
       Log.i(TAG, "Setup button listeners");

        final ImageButton btnStartStop = (ImageButton) rootView.findViewById(R.id.btnStartStop);
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state ==0) {
                    Log.i(TAG, "Changing background to start.");
                    btnStartStop.setBackgroundResource(R.drawable.stop);
                    Intent intent = new Intent(getActivity(), StepCounterRecorderService.class);
                    getActivity().startService(intent);
                    state = 1;
                } else {
                    Log.i(TAG, "Changing background to stop.");
                    btnStartStop.setBackgroundResource(R.drawable.start);
                    Intent intent = new Intent(getActivity(), StepCounterRecorderService.class);
                    if (intent != null) {
                        getActivity().stopService(intent);
                    }
                    state = 0;
                }

            }
        });

        // Instantiates a new DownloadStateReceiver
        StepCounterBroadcastReceiver stepCounterBroadcastReceiver =
                new StepCounterBroadcastReceiver();
        // The filter's action is BROADCAST_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(
                StepCounterRecorderService.BROADCAST_ACTION);

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                stepCounterBroadcastReceiver,
                mStatusIntentFilter);

        tvSteps = (TextView) rootView.findViewById(R.id.tvSteps);
        tvAgeOfEvents = (TextView) rootView.findViewById(R.id.tvAgeOfEvents);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Fragment is being restored, reinitialise its state with data from the bundle
        if (savedInstanceState != null) {
            Log.i(TAG, "Reinstating fragment state with data from bundle.");
            //steps = savedInstanceState.getLong(BUNDLE_STEPS);
            // store the previous number of steps to keep  step counter count consistent
            //previousStepCounterSteps = steps;
            // Register listeners again if in detector or counter states with restored delay
        }
    }

    /**
     * Records the state of the application into the {@link android.os.Bundle}.
     *
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class StepCounterBroadcastReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private StepCounterBroadcastReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            tvSteps.setText(Long.toString(intent.getExtras().getLong(StepCounterRecorderService.STEPS)));
            tvAgeOfEvents.setText(intent.getExtras().getString(StepCounterRecorderService.AGE_OF_EVENTS));
        }
    }
}