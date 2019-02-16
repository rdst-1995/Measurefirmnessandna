package com.example.ripudaman.measurefirmnessandwp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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

    Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_firmness);

        // Text label that currently displays the x-coordinate of accelerometer
        mTextAcc = findViewById(R.id.labelAcc);

        // Instantiate sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Instantiate accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Register the sensorManager to listen for accelerometer data
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // TODO: implement floating action button for exporting data
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        synchronized (this){
            float lux = sensorEvent.values[1];
            mTextAcc.setText(String.valueOf(lux));

            // Update time
            time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastTime;

            // Set current y-coordinate value
            yValue = sensorEvent.values[1];



            //LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                    //new DataPoint(sensorEvent.values[0], sensorEvent.values[1])
            //});
            GraphView graph = findViewById(R.id.graph);

            LineGraphSeries series = new LineGraphSeries<DataPoint>();

            series.appendData(new DataPoint(time, sensorEvent.values[1]), true,100000000);
            //series.resetData()
            graph.addSeries(series);
            graph.getViewport().setScalable(true);
            graph.getViewport().setScrollable(true);
            graph.getViewport().setScalableY(true);
            graph.getViewport().setScrollableY(true);
//            graph.getViewport().setXAxisBoundsManual(true);
//            graph.getViewport().setMinX(0);
//            graph.getViewport().setMaxX(100);
//            graph.getViewport().setMinY(-10.00);
//            graph.getViewport().setMaxY(10.00);

            lastTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        sensorManager.registerListener(MeasureFirmness.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

//        timer1 = new Runnable(){
//            @Override
//            public void run(){
//                series1.resetData();
//                handler.postDelayed(this, 300);
//            }
//        };
//        handler.postDelayed(timer1, 300);
//
//        timer2 = new Runnable(){
//            @Override
//            public void run(){
//                graph2LastXValue += 1.00;
//                series2.appendData(new DataPoint(graph2LastXValue, , true, 1000));
//                handler.postDelayed(this, 300);
//            }
//        };
//        handler.postDelayed(timer2, 300);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
//        handler.removeCallbacks(timer1);
//        handler.removeCallbacks(timer2);
    }
}
