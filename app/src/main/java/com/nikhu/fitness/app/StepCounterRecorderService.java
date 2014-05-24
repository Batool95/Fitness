package com.nikhu.fitness.app;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class StepCounterRecorderService extends Service implements SensorEventListener {

    public static final String TAG = "StepCounterRecorderService";

    // Batch sensor latency is specified in microseconds
    private static final int BATCH_LATENCY_5s = 5000000;
    private static final int BATCH_LATENCY_10s = 10000000;

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_START = "com.nikhu.fitness.app.action.START";
    public static final String ACTION_STOP = "com.nikhu.fitness.app.action.STOP";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "com.nikhu.fitness.app.BROADCAST";

    public static final String STEPS = "steps";
    public static final String AGE_OF_EVENTS = "ageOfEvents";


    // Number of events to keep in queue and display on card
    private static final int EVENT_QUEUE_LENGTH = 10;
    // List of timestamps when sensor events occurred
    private float[] eventDelays = new float[EVENT_QUEUE_LENGTH];
    // number of events in event list
    private int eventLength = 0;
    // pointer to next entry in sensor event list
    private int eventData = 0;

    // Steps counted in current session
    private long steps;
    // Value of the step counter sensor when the listener was registered.
    // (Total steps are calculated from this value.)
    private long stepCounterSteps = 0;
    // Steps counted by the step counter previously. Used to keep counter consistent across rotation
    // changes
    private long previousStepCounterSteps = 0;

    private final StringBuffer delayStringBuffer = new StringBuffer();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Tell the user service started.
        Toast.makeText(this, "Service started.", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Starting service.");
        handleActionStart(this);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handleActionStop(this);
        super.onDestroy();
        // Tell the user we stopped.
        Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Stopping service.");
    }


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStart(Context context) {
        if (!isKitkatWithStepSensor(context)) {
            return;
        }
        resetCounter();
        // Get the default sensor for the sensor type from SensorManager
        SensorManager sensorManager = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
        // Get Step Counter Sensor
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Register the listener for this sensor in batch mode.
        // If the max delay is 0, events will be delivered in continuous mode without batching.
        final boolean batchMode = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_10s);
        Log.i(TAG, "Event listener for step detector sensor registered with a max delay of "
                + BATCH_LATENCY_10s);
    }

    private void handleActionStop(Context context) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
            sensorManager.unregisterListener(this);
            Log.i(TAG, "Sensor listener unregistered.");
            WorkoutDBAdapter workoutDBAdapter = new WorkoutDBAdapter(context);
            workoutDBAdapter.saveCurrentSessionSteps(new Date(), (int) steps,  convertToCalories(steps) , convertToDistance(steps));
            Log.i(TAG, "Saved steps information to database.");
    }

    private boolean isKitkatWithStepSensor(Context context) {
        // Require at least Android KitKat
        int currentApiVersion = Build.VERSION.SDK_INT;
        Log.i(TAG, "Device current Android version: " + currentApiVersion);
        // Check that the device supports the step counter and detector sensors
        PackageManager packageManager = context.getPackageManager();
        return currentApiVersion >= Build.VERSION_CODES.KITKAT && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "New step detected by STEP_COUNTER sensor.");
        // Tell the user service started.
        Toast.makeText(this, "onSensorChanged", Toast.LENGTH_SHORT).show();
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

         /*
     * Creates a new Intent containing a Uri object
     * BROADCAST_ACTION is a custom Intent action
     */
        Intent localIntent =
                new Intent(BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putExtra(STEPS, steps)
                .putExtra(AGE_OF_EVENTS, delayStringBuffer.toString());
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

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





    private float convertToCalories(long steps) {
        return (float) steps / 20;
    }

    private float convertToDistance(long steps) {
        return (float) (steps*(1/2.5)*(1/1000));
    }


}
