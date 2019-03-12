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
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

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
    private List<String> dataList = new ArrayList<>();
    private int count;
    private long currentTime;
    private ToggleButton toggleButton;
    private int mSecond;

    Sensor accelerometer;
    Sensor gyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_firmness);

        yValue = 0;
        mSecond = 0;
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);


        // Text label that currently displays the x-coordinate of accelerometer
        mTextAcc = findViewById(R.id.labelAcc);

        // Instantiate sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Instantiate accelerometer and gyroscope
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register the sensorManager to listen for accelerometer data
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveOnClick(view);
            }
        });

        updateTime();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        synchronized (this){
            // Time since last data recording
            time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastTime;

            // Set current y-coordinate value
            yValue = sensorEvent.values[1];

            lastTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

            GraphView graph = findViewById(R.id.graph);

            if (toggleButton.isChecked()){
                LineGraphSeries series = new LineGraphSeries<DataPoint>();
                currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

                String str = String.valueOf(yValue);
                saveData(str);

                series.appendData(new DataPoint(mSecond, yValue), true, 100);
                graph.addSeries(series);
                count++;
            }

            graph.getViewport().setScalable(true);
            graph.getViewport().setScrollable(true);
            graph.getViewport().setScalableY(true);
            graph.getViewport().setScrollableY(true);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        toggleButton.setChecked(false);
        mSecond = 0;
        count = 0;
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
        toggleButton.setChecked(false);
        mSecond = 0;
        count = 0;
    }

    public void saveOnClick(View view){
        // Get the directory of the phones file storage, make a file name, then append the two to form the file path
        String baseDir = getApplicationContext().getFilesDir().getPath();
        String EXPORT_FILE = "export.csv";
        String filePath = baseDir + File.separator + EXPORT_FILE;

        // Entry data to save to file
        String entry[] = new String[100];
        File file = new File(filePath);
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
            for (int i = 0; i < 100; i++){
                entry[i] = dataList.get(i);
                writer.writeNext(entry);
            }
            writer.close();

            // Notify the user that the file saved successfully
            toastIt("File saved successfully!");

        }catch (Exception e){
            e.printStackTrace();
            toastIt("Save failed");
        }
    }

    public void toastIt(String message){
        Toast.makeText(MeasureFirmness.this, message, Toast.LENGTH_SHORT).show();
    }

    public void saveData(String data){

        dataList.add(data);

        if (dataList.size() % 10 == 0)
            toastIt("Data saved!");

        if(dataList.size() == 100)
            toastIt("100 data points saved, current time: " + mSecond);
    }

    public void toggleClick(View v){
        if(toggleButton.isChecked())
            toastIt("RECORDING STARTED");
        else if (!toggleButton.isChecked())
            toastIt("RECORDING STOPPED");
    }

    public void updateTime(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run(){
                try{
                    Calendar c = Calendar.getInstance();
                    mSecond = c.get(Calendar.SECOND);
                    mTextAcc.setText(String.valueOf(mSecond));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, 0, 1000);
    }
}

