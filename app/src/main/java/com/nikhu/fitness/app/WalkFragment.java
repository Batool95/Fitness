package com.nikhu.fitness.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

import java.util.Date;

/**
 * Created by bujji on 01-05-2014.
 */
public class WalkFragment extends Fragment {

    public static final String TAG = "WalkFragment";

    // Steps counted in current session
    private long steps;
    // Value of the step counter sensor when the listener was registered.
    // (Total steps are calculated from this value.)
    private long stepCounterSteps = 0;
    // Steps counted by the step counter previously. Used to keep counter consistent across rotation
    // changes
    private long previousStepCounterSteps = 0;

    /* keep track of the last few events and show their delay from when the
    event occurred until it was received by the event listener.
    These variables keep track of the list of timestamps and the number of events.
    */

    // Batch sensor latency is specified in microseconds
    private static final int BATCH_LATENCY_5s = 5000000;
    private static final int BATCH_LATENCY_10s = 10000000;

    private static final String BUNDLE_STEPS = "steps";

    // Number of events to keep in queue and display on card
    private static final int EVENT_QUEUE_LENGTH = 10;
    // List of timestamps when sensor events occurred
    private float[] eventDelays = new float[EVENT_QUEUE_LENGTH];
    // number of events in event list
    private int eventLength = 0;
    // pointer to next entry in sensor event list
    private int eventData = 0;

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
                    Log.i(TAG, "Changing background to stop.");
                    btnStartStop.setBackgroundResource(R.drawable.stop);
                    registerEventListener();
                    state = 1;
                } else {
                    Log.i(TAG, "Changing background to start.");
                    btnStartStop.setBackgroundResource(R.drawable.start);
                    unregisterEventListener();
                    state = 0;
                }

            }
        });
        tvSteps = (TextView) rootView.findViewById(R.id.tvSteps);
        tvAgeOfEvents = (TextView) rootView.findViewById(R.id.tvAgeOfEvents);

        return rootView;
    }

    private boolean isKitkatWithStepSensor() {
        // Require at least Android KitKat
        int currentApiVersion = Build.VERSION.SDK_INT;
        Log.i(TAG, "Device current Android version: " + currentApiVersion);
        // Check that the device supports the step counter and detector sensors
        PackageManager packageManager = getActivity().getPackageManager();
        return currentApiVersion >= Build.VERSION_CODES.KITKAT && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
    }

    private void registerEventListener() {
        if (!isKitkatWithStepSensor()) {
            return;
        }
        // Get the default sensor for the sensor type from SensorManager
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
        // Get Step Counter Sensor
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Register the listener for this sensor in batch mode.
        // If the max delay is 0, events will be delivered in continuous mode without batching.
        final boolean batchMode = sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_5s);
        Log.i(TAG, "Event listener for step detector sensor registered with a max delay of "
                + BATCH_LATENCY_5s);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.i(TAG, "New step detected by STEP_COUNTER sensor.");
            // store the delay of this event
            recordDelay(event);
            final String delayString = getDelayString();

            /*
                A step counter event contains the total number of steps since the listener
                was first registered. We need to keep track of this initial value to calculate the
                number of steps taken, as the first value a listener receives is undefined.
                 */
            if (stepCounterSteps < 1) {
                // initial value
                stepCounterSteps = (int) event.values[0];
            }
            // Calculate steps taken based on first counter value received.
            steps = (int) event.values[0] - stepCounterSteps;

            // Add the number of steps previously taken, otherwise the counter would start at 0.
            // This is needed to keep the counter consistent across rotation changes.
            steps = steps + previousStepCounterSteps;

            Log.i(TAG, "Total step count: " + steps);

            tvSteps.setText((Long.toString(steps)));
            tvAgeOfEvents.setText(getDelayString());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /**
     * Resets the step counter by clearing all counting variables and lists.
     */
    private void resetCounter() {
        steps = 0;
        stepCounterSteps = 0;
        eventLength = 0;
        eventDelays = new float[EVENT_QUEUE_LENGTH];
        previousStepCounterSteps = 0;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Fragment is being restored, reinitialise its state with data from the bundle
        if (savedInstanceState != null) {
            Log.i(TAG, "Reinstating fragment state with data from bundle.");
            resetCounter();
            steps = savedInstanceState.getLong(BUNDLE_STEPS);

            // store the previous number of steps to keep  step counter count consistent
            previousStepCounterSteps = steps;
            // Register listeners again if in detector or counter states with restored delay
            registerEventListener();
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
        // Store all variables required to restore the state of the application
        outState.putLong(BUNDLE_STEPS, steps);
    }

    /**
     * Records the delay for the event.
     *
     * @param event
     */
    private void recordDelay(SensorEvent event) {
        // Calculate the delay from when event was recorded until it was received here in ms
        // Event timestamp is recorded in us accuracy, but ms accuracy is sufficient here
        eventDelays[eventData] = System.currentTimeMillis() - (event.timestamp / 1000000L);

        // Increment length counter
        eventLength = Math.min(EVENT_QUEUE_LENGTH, eventLength + 1);
        // Move pointer to the next (oldest) location
        eventData = (eventData + 1) % EVENT_QUEUE_LENGTH;
    }

    private final StringBuffer delayStringBuffer = new StringBuffer();

    /**
     * Returns a string describing the sensor delays recorded in
     * {@link #recordDelay(android.hardware.SensorEvent)}.
     *
     * @return
     */
    private String getDelayString() {
        // Empty the StringBuffer
        delayStringBuffer.setLength(0);

        // Loop over all recorded delays and append them to the buffer as a decimal
        for (int i = 0; i < eventLength; i++) {
            if (i > 0) {
                delayStringBuffer.append(", ");
            }
            final int index = (eventData + i) % EVENT_QUEUE_LENGTH;
            final float delay = eventDelays[index] / 1000f; // convert delay from ms into s
            delayStringBuffer.append(String.format("%1.1f", delay));
        }

        return delayStringBuffer.toString();
    }

    public void unregisterEventListener() {
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorEventListener);
        Log.i(TAG, "Sensor listener unregistered.");
        WorkoutDBAdapter workoutDBAdapter = new WorkoutDBAdapter(getActivity());
        workoutDBAdapter.saveCurrentSessionSteps(new Date(), (int) steps, (float) (steps * 1.5) ,steps * 2);
        Log.i(TAG, "Saved steps information to database.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the listener when the application is paused
        unregisterEventListener();
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
}
