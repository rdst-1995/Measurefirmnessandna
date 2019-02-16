package com.example.ripudaman.measurefirmnessandwp;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button button2;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                goToSecondActivity();
            }
        });

        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                goToThirdActivity();
            }
        });
    }

    private void goToSecondActivity() {
        Intent intent = new Intent(this, MeasureFirmness.class);

        startActivity(intent);
    }

    private void goToThirdActivity() {
        Intent intent = new Intent(this, MeasureNoise.class);

        startActivity(intent);
    }
}