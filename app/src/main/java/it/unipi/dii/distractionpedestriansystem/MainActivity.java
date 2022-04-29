package it.unipi.dii.distractionpedestriansystem;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor s1;
    private Sensor s2;
    private static String TAG = "PedestrianDistractionSystem";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup sensors
        sensorSetup();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {

        }else if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            fd.addToQueue(sensorEvent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, s1, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, s2, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)  {
        Log.i(TAG, "Accuracy changed");
    }

    private void sensorSetup(){
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        s1 = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        s2 = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if(s1 == null || s2 == null) {
            Log.d(TAG, "Sensor(s) unavailable");
            finish();
        }
    }
}