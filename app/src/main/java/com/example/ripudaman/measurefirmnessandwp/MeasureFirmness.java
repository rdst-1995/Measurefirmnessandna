package com.example.ripudaman.measurefirmnessandwp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Measure;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MeasureFirmness extends AppCompatActivity implements SensorEventListener {

    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 6;
    private SensorManager sensorManager;
    private TextView mTextAcc;
    private TextView firmnessRating;
    private TextView maxVal, bounce, freefall, height, speed, force, stoptime;
    private int count;
    private long timestamp;
    private double beginTime;
    private double endTime;
    private double savedTime;
    private double settleTime;
    private double savedMax;
    private double savedPreviousValue;
    private double savedMaxChange;
    private String[] entry;
    private List<String[]> entries = new ArrayList<>();
    private List<String[]> forceList = new ArrayList<>();
    private Button startButton, resetButton, pauseButton;
    private boolean wasClicked;
    private boolean begin;
    private boolean freeFallDetected;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    int Seconds, Minutes, MilliSeconds;

    Handler handler;
    Sensor accelerometer;
    Sensor gyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_firmness);

        count = 0;
        savedPreviousValue = -1.0;
        savedMax = -1.0;

        wasClicked = false;
        begin = false;
        freeFallDetected = false;

        handler = new Handler();

        mTextAcc = findViewById(R.id.labelAcc);
        maxVal = findViewById(R.id.max_value);
        bounce = findViewById(R.id.bounce);
        freefall = findViewById(R.id.freefall);
        height = findViewById(R.id.height);
        speed = findViewById(R.id.speed);
        force = findViewById(R.id.force);
        stoptime = findViewById(R.id.stoptime);
        firmnessRating = findViewById(R.id.firmness_rating);

        resetButton = (Button) findViewById(R.id.button6);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Reset time
                MillisecondTime = 0L;
                StartTime = 0L;
                TimeBuff = 0L;
                UpdateTime = 0L;
                Seconds = 0;
                Minutes = 0;
                MilliSeconds = 0;
                mTextAcc.setText("00:00:00");

                wasClicked = false;
                freeFallDetected = false;

                // Reset text values
                maxVal.setText("Maximum Acceleration Value");
                bounce.setText("Bounce Duration");
                freefall.setText("Free-Fall Duration");
                height.setText("Drop Height");
                speed.setText("Impact Speed");
                force.setText("Impact Force");
                stoptime.setText("Time the phone took to stop");

                // Reset variables
                count = 0;
                savedPreviousValue = -1.0;
                savedMax = -1.0;

                // Clear the entries in the data list and list of forces for a new recording
                entries.clear();
                forceList.clear();

                toastIt("RECORDING RESET");
            }
        });

        pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                TimeBuff += MillisecondTime;
                handler.removeCallbacks(runnable);
                resetButton.setEnabled(true);
                wasClicked = false;

                firmnessRating.setText(String.valueOf(getFirmnessRating()));

                toastIt("RECORDING PAUSED");
                toastIt("Size of list: " + String.valueOf(entries.size()));
            }
        });

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);
                resetButton.setEnabled(false);
                wasClicked = true;
                begin = true;
                freeFallDetected = false;

                entries.clear();

                toastIt("RECORDING STARTED");
            }
        });

        // Instantiate sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Instantiate accelerometer and gyroscope
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register the sensorManager to listen for accelerometer and gyroscope data
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(MeasureFirmness.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveOnClick(view);
                checkPermissions();
            }
        });
        fab.setImageResource(R.drawable.ic_baseline_save_alt_24px);

        Log.d("WASCLICKED", String.valueOf(wasClicked));
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        synchronized (this){

            // Static time for the rest of this events calculations
            String second = String.valueOf(Seconds);
            String millis = String.valueOf(MilliSeconds);
            String currentTime = second + "." + millis;

            double x;
            double y;
            double z;
            double change;
            double currentForce;
            double maxForce = -1;

            // Filter to get only accelerometer data
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];

                currentForce = getDynamicForce(getResultantVector(x, y, z));

                // Save values and perform data analysis when the user starts recording their test
                if (wasClicked){
                    String xVal = String.valueOf(x);
                    String yVal = String.valueOf(y);
                    String zVal = String.valueOf(z);
                    String time = String.valueOf(currentTime);

                    if (freefallMonitor(x, y, z)){
                        freeFallDetected = true;


                        // Set the begin time of the free fall
                        // begin resets to true when the pause or reset button are tapped
                        if (begin){
                            beginTime = Double.valueOf(currentTime);
                            begin = false;

                            Log.d("begintime", String.valueOf(beginTime));
                        }

                        // The end time will keep being updated until the last moment of detected free fall
                        endTime = Double.valueOf(currentTime);
                        Log.d("endtime", String.valueOf(endTime));
                    }

                    // Save the maximum acceleration value in the z-coordinate
                    if (z > savedMax){
                        savedMax = z;
                        savedTime = Double.valueOf(currentTime);

                        maxVal.setText(String.valueOf(savedMax));
                    }

                    Log.d("savedmax", String.valueOf(savedMax) + " | " + String.valueOf(savedTime));

                    if (currentForce > maxForce)
                    {
                        maxForce = currentForce;
                    }

                    change = Math.abs(z - savedPreviousValue);

                    // If there hasn't been much of a change with the last 20 data points,
                    // then the phone is done bouncing and its 'settle time' is set
                    if (change < 2.0){
                        count++;
                    }
                    else
                        count = 0;

                    if (count == 20){
                        settleTime = Double.valueOf(currentTime);

                        Log.d("settletime", String.valueOf(settleTime));
                    }

                    entry = new String[]{time, xVal, yVal, zVal};
                    entries.add(entry);

                    String[] forceEntry;
                    forceEntry = new String[]{time, String.valueOf(currentForce)};
                    forceList.add(forceEntry);

                    savedPreviousValue = z;

                    // Live feed of y-coordinate accelerometer values printed to logcat with the tag "VALUESHERE"
                    Log.d("VALUESHERE", zVal);
                }
            }
        }
    }

    /**
     * Save recorded data to a .csv file and export it to the phones storage
     * When the user taps the floating action button*/
    public void saveOnClick(View view){
        // Get the directory of the phones file storage, make a file name, then append the two to form the file path
        String baseDir = getApplicationContext().getFilesDir().getPath();
        String EXPORT_FILE = "export.csv";
        String filePath = baseDir + File.separator + EXPORT_FILE;

        File absolutePath = Environment.getExternalStorageDirectory();
        File file2 = new File(absolutePath, EXPORT_FILE);

        File pathfile = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath()
                + File.separator
                + "csvData");

        // Entry data to save to file
        File file = new File(String.valueOf(pathfile));
        CSVWriter writer;

        try{
            // Check to see if file exists
            if (file.exists() && !file.isDirectory()){
                FileWriter fileWriter = new FileWriter(file2, true);
                writer = new CSVWriter(fileWriter);
            }
            else{
                writer = new CSVWriter(new FileWriter(file2));
            }

            Log.d("FILEDEBUG", filePath + " | " + String.valueOf(pathfile) + " | " + file2);

            // Save data from list to csv file
            writer.writeAll(entries);
            writer.close();

            // Notify the user that the file saved successfully
            toastIt("File saved successfully!");

        }catch (Exception e){
            e.printStackTrace();
            toastIt("Save failed");
        }

        printList();
        printForceList();
    }

    /**
     * Utility method for making toasts*/
    public void toastIt(String message){
        Toast.makeText(MeasureFirmness.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Runnable for the timer
     * Updates the timer text*/
    private Runnable runnable = new Runnable(){
        @Override
        public void run(){
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);
            Minutes = Seconds / 60;
            Seconds = Seconds % 60;
            MilliSeconds = (int) (UpdateTime % 1000);

            mTextAcc.setText("" + String.format("%02d",Minutes) + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }
    };

    /**
     * Method for debugging the data list*/
    public void printList(){
        for (int i = 0; i < entries.size(); i++){
            Log.d("PRINTLIST", Arrays.toString(entries.get(i)));
        }
    }

    /**
     * Get mattress firmness rating
     * Called when the user presses pause or when the system stops recording automatically
     * Returns an integer rating on a scale of 1-10; 1 being the firmest, 10 being the softest*/
    public int getFirmnessRating(){

        int rating = 0;
        double freeFallDuration;
        double bounceDuration;
        double stoppageTime;
        double heightOfDrop;
        double impactSpeed;
        double impactForce;

        bounceDuration = getBounceDuration();
        freeFallDuration = getFreeFallDuration();
        heightOfDrop = calculateHeight(freeFallDuration);
        impactSpeed = calculateImpactSpeed(heightOfDrop);
        impactForce = calculateImpactForce(savedMax);
        stoppageTime = endTime - savedTime;

        Log.i("RATING", "MAX VALUE: " + String.valueOf(savedMax) +
                " | TIME OF PEAK: " + String.valueOf(savedTime) +
                " | BOUNCE DURATION: " + String.valueOf(bounceDuration) +
                " | FREE FALL DURATION: " + String.valueOf(freeFallDuration) +
                " | HEIGHT OF DROP: " + String.valueOf(heightOfDrop) +
                " | IMPACT SPEED: " + String.valueOf(impactSpeed) +
                " | IMPACT FORCE: " + String.valueOf(impactForce) +
                " | STOP TIME: " + String.valueOf(stoppageTime));

        // Only when the user properly dropped the phone so that free fall is detected...
        if (freeFallDetected){

            // Update display to show free fall statistics
            maxVal.setText(String.valueOf(savedMax));
            bounce.setText(String.valueOf(bounceDuration));
            freefall.setText(String.valueOf(freeFallDuration));
            height.setText(String.valueOf(heightOfDrop));
            speed.setText(String.valueOf(impactSpeed));
            force.setText(String.valueOf(impactForce));
            stoptime.setText(String.valueOf(stoppageTime));
        }
        else{
            maxVal.setText("No free fall detected");
        }

        // Determine the rating here

        return rating;
    }

    /**
     * Detects if the phone is in free fall by observing the root square of all three accelerometer vectors
     * If all three vectors approach zero, that means they are falling with gravity and thus, not feeling the gravity
     * Returns true while the phone is in free fall and data is being recorded, false otherwise.*/
    public boolean freefallMonitor(double x, double y, double z){

        double rootSquare;

        rootSquare = Math.sqrt(Math.pow(x ,2) + Math.pow(y ,2) + Math.pow(z ,2));

        if (rootSquare < 2.0){
            Log.d("FREEFALLING", "Free falling!");

            return true;
        }

        return false;
    }

    /**
     * Gets the bounce duration after initial impact of the phone with the test surface
     * settleTime is set when the the system doesn't detect a change greater than a certain threshold for more than 20 datapoints
     * saveTime is the time of the maximum acceleration (impact acceleration)*/
    public double getBounceDuration(){ return settleTime - savedTime; }

    /**
     * Function to calculate the duration of the phone's freefall
     * The values used for this calculation are determined by freeFallMonitor*/
    public double getFreeFallDuration(){
        return endTime - beginTime;
    }

    /**
     * Function to calcuate the height of the drop
     * Utilizes the phones drop duration for the calculation
     * The formula is: (1/2)*g*t^2   where t is the drop duration*/
    public double calculateHeight(double dropDuration){

        double height;
        height = 0.5 * 9.81 * Math.pow(dropDuration, 2);

        return height;
    }

    /**
     * Method to calculate the impact speed of the drop
     * Utilizes the height of the drop (ignoring air resistance)*/
    public double calculateImpactSpeed(double height){

        double impactSpeed;
        impactSpeed = Math.sqrt(2 * 9.81 * height);

        return impactSpeed;
    }

    /**
     * Method for calculating the force of impact of the drop
     * Utilizes Newtons second law, and the average smartphone weight
     * F = ma */
    public double calculateImpactForce(double maxAcceleration){
        double force;

        // Average smartphone weighs between 140 and 170 grams
        double mass = 0.160;

        // Newton's second law: F = ma
        force = mass * maxAcceleration;

        return force;
    }

    /**
     * Method for acquiring the resulting acceleration vector
     * The resultant vector is the root mean square of the three accelerometer values*/
    public double getResultantVector(double x, double y, double z){
        double vector;
        vector = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        return vector;
    }

    /**
     * Returns the current force exerted by the current acceleration resultant vector
     * Assumes the mass of the phone to be 160g*/
    public double getDynamicForce(double vector){
        double currentForce = 0.160 * vector;

        Log.d("DYNAMICFORCE", String.valueOf(currentForce));

        return currentForce;
    }

    /**
     * Prints the list of logged forces to logcat
     * The values are added dynamically along with the timestamp*/
    public void printForceList(){
        for (int i = 0; i < forceList.size(); i++){
            Log.d("FORCELIST", Arrays.toString(forceList.get(i)));
        }
    }

    public void checkPermissions(){
        if (ContextCompat.checkSelfPermission(MeasureFirmness.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MeasureFirmness.this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }
            else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MeasureFirmness.this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {
            // Permission already granted

            Log.d("PERMISSIONS", "Permission already granted");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(false);
                    resetButton.setEnabled(false);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

}

