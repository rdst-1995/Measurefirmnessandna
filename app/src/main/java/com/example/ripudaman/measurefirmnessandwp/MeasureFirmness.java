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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MeasureFirmness extends AppCompatActivity implements SensorEventListener {

//    private final Handler handler = new Handler();
//    private Runnable timer1;
//    private Runnable timer2;
//    private LineGraphSeries<DataPoint> series1;
//    private LineGraphSeries series2;
    private SensorManager sensorManager;
    private TextView mTextAcc;
//    private double graph2LastXValue = 5.00;
    private long lastTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    private long time;
    private double yValue;
    private double zValue;
    private double xValue;
    private List<String> dataList = new ArrayList<>();
    private int count;
    private long currentTime;
    private int mSecond;
    private String[] entry;
    private List<String[]> entries = new ArrayList<>();
    private int entryElement = 0;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    Handler handler;
    int Seconds, Minutes, MilliSeconds ;
    private Button startButton, resetButton, pauseButton;
    private boolean wasClicked;

    Sensor accelerometer;
    Sensor gyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_firmness);

        wasClicked = false;

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

                toastIt("RECORDING PAUSED");
                toastIt("Size of list: " + String.valueOf(entries.size()));

                // getFirmnessRating();
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
            }
        });

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
            // Set current y-coordinate value

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                xValue = sensorEvent.values[0];
                yValue = sensorEvent.values[1];
                zValue = sensorEvent.values[2];

                if (wasClicked){
                    String yVal = String.valueOf(yValue);
                    String tim = String.valueOf(Seconds + "." + MilliSeconds);

                    getFirmnessRating(tim, xValue, yValue, zValue);

                    entry = new String[]{tim, yVal};
                    entries.add(entry);

                    Log.d("VALUESHERE", yVal);
                    //toastIt("Size of list is: " + String.valueOf(entries.size()));
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

    public void toastIt(String message){
        Toast.makeText(MeasureFirmness.this, message, Toast.LENGTH_SHORT).show();
    }

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

    public void printList(){
        for (int i = 0; i < entries.size(); i++){
            Log.d("PRINTLIST", Arrays.toString(entries.get(i)));
        }
    }

    public int getFirmnessRating(String time, double x, double y, double z){

        int rating = 0;
        double rootSquare;

        rootSquare = Math.sqrt(Math.pow(x ,2) + Math.pow(y ,2) + Math.pow(z ,2));

        if (rootSquare < 2.0){
            toastIt("Freefall detected");
        }

        // gets the y-coordinate acceleration values of recorded drop
        for (int i = 0; i < entries.size(); i++){


        }

        return rating;
    }
}

