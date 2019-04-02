package com.example.ripudaman.measurefirmnessandwp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MeasureFirmness extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView mTextAcc;
    private double xValue, yValue, zValue;
    private double freeFallBeginTime;
    private double freeFallEndTime;
    private String[] entry;
    private List<String[]> entries = new ArrayList<>();
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    int Seconds, Minutes, MilliSeconds ;
    private Button startButton, resetButton, pauseButton;
    private boolean wasClicked;
    private boolean begin;

    Handler handler;
    Sensor accelerometer;
    Sensor gyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_firmness);

        wasClicked = false;
        begin = false;
        handler = new Handler();
        mTextAcc = (TextView) findViewById(R.id.labelAcc);

        resetButton = (Button) findViewById(R.id.button6);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MillisecondTime = 0L;
                StartTime = 0L;
                TimeBuff = 0L;
                UpdateTime = 0L;
                Seconds = 0;
                Minutes = 0;
                MilliSeconds = 0;

                mTextAcc.setText("00:00:00");
                wasClicked = false;

                entries.clear();

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

                getFirmnessRating();

                toastIt("RECORDING PAUSED");
                toastIt("Size of list: " + String.valueOf(entries.size()));
                toastIt("Firmness Rating: " + String.valueOf(getFirmnessRating()));
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

                toastIt("RECORDING STARTED");
            }
        });

        // Instantiate sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Instantiate accelerometer and gyroscope
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register the sensorManager to listen for accelerometer and gyroscope data
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, 1000000);
        sensorManager.registerListener(MeasureFirmness.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveOnClick(view);
            }
        });
        fab.setImageResource(R.drawable.ic_baseline_save_alt_24px);

        LineGraphSeries series = new LineGraphSeries<DataPoint>();
        GraphView graph = findViewById(R.id.graph);

        series.appendData(new DataPoint(Seconds, yValue), true, 100);
        graph.addSeries(series);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true);

        Log.d("WASCLICKED", String.valueOf(wasClicked));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        synchronized (this){

            // Filter to get only accelerometer data
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                xValue = sensorEvent.values[0];
                yValue = sensorEvent.values[1];
                zValue = sensorEvent.values[2];

                if (wasClicked){
                    String yVal = String.valueOf(yValue);
                    String tim = String.valueOf(sensorEvent.timestamp);

                    freefallMonitor(xValue, yValue, zValue);

                    entry = new String[]{tim, yVal};
                    entries.add(entry);

                    // Live feed of y-coordinate accelerometer values printed to logcat with the tag "VALUESHERE"
                    Log.d("VALUESHERE", yVal);
                }
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // Save recorded data to a .csv file and export it to the phones storage
    // When the user taps the floating action button
    public void saveOnClick(View view){
        // Get the directory of the phones file storage, make a file name, then append the two to form the file path
        String baseDir = getApplicationContext().getFilesDir().getPath();
        String EXPORT_FILE = "export.csv";
        String filePath = baseDir + File.separator + EXPORT_FILE;

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
                FileWriter fileWriter = new FileWriter(filePath, true);
                writer = new CSVWriter(fileWriter);
            }
            else{
                writer = new CSVWriter(new FileWriter(filePath));
            }

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
    }

    // A utility method to easily make toasts
    public void toastIt(String message){
        Toast.makeText(MeasureFirmness.this, message, Toast.LENGTH_SHORT).show();
    }

    // Runnable for the timer
    // Updates the timer text
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

    // Prints current list to the logcat with the tag "PRINTLIST"
    public void printList(){
        for (int i = 0; i < entries.size(); i++){
            Log.d("PRINTLIST", Arrays.toString(entries.get(i)));
        }
    }

    // Get mattress firmness rating
    // Called when the user presses pause or when the system stops recording automatically
    // Returns an integer rating on a scale of 1-10; 1 being the firmest, 10 being the softest
    public int getFirmnessRating(){
        int rating = 0;
        double maxPeak = 0.0;
        double lastPeakTime = 0.0;
        double timeOfMaxPeak = 0.0;
        double timeDiff = 0.0;
        double bounceDuration = 0.0;
        double val;
        double currentTime;

        // Gets the max peak value of the recording and its timestamp
        for (int i = 0; i < entries.size(); i++){

            // Get y-coordinate accelerometer value of list
            String[] vals = entries.get(i);
            val = Double.valueOf(vals[1]);
            currentTime = Double.valueOf(vals[0]);

            // Get the maximum peak value of y-coordinate acceleration of the drop (moment of impact) and its timestamp
            if (val > maxPeak){
                maxPeak = val;
                timeOfMaxPeak = currentTime;
            }

            // Get the last peak within reason (Any bounce once second after the drop is likely the user picking the phone up)
            if (val > 5.0 && ((currentTime - timeOfMaxPeak) < 1000)){
                lastPeakTime = currentTime;
            }
        }

        // Time difference between the last moment of detected free fall and the impact acceleration
        timeDiff = timeOfMaxPeak - freeFallEndTime;

        // Duration of the bounce
        bounceDuration = lastPeakTime - timeOfMaxPeak;

        // Here is the part of the code where determining the firmness rating would go
        // The rating is determined by a combination of the max y-coordinate acceleration value and the time difference
        // between the time of max acceleration and the end time of the phones free fall (when the phone impacts the surface)


        return rating;
    }

    // Detects if the phone is in free fall by observing the root square of all three accelerometer vectors
    // If all three vectors approach zero, that means they are falling with gravity and thus, not feeling the gravity
    // Returns true while the phone is in free fall and data is being recorded, false otherwise.
    public boolean freefallMonitor(double x, double y, double z){

        double rootSquare;

        rootSquare = Math.sqrt(Math.pow(x ,2) + Math.pow(y ,2) + Math.pow(z ,2));

        if (rootSquare < 2.0){
            Log.d("FREEFALLING", "Free falling!");

            // Set the begin time of the free fall
            // begin resets to true when the pause or reset button are tapped
            if (begin){
                freeFallBeginTime = Double.valueOf(Seconds + "." + MilliSeconds);
                begin = false;
            }

            // The end time will keep being updated until the last moment of detected free fall
            freeFallEndTime = Double.valueOf(Seconds + "." + MilliSeconds);
            return true;
        }

        return false;
    }
}

