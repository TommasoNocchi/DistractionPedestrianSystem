package it.unipi.dii.distractionpedestriansystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import it.unipi.dii.distractionpedestriansystem.service.BackgroundScanService;


public class MainActivity extends AppCompatActivity implements SensorEventListener,  View.OnClickListener{

    private SensorManager sm;
    private Sensor s1;
    private Sensor s2;
    private static String TAG = "PedestrianDistractionSystem";


    private Intent serviceIntent;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        serviceIntent = new Intent(getApplicationContext(), BackgroundScanService.class);

        // Setup sensors
        sensorSetup();

        //Setup buttons
        setupButtons();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {

        }else if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Usati per LinearAcceleration and StepDetector (*)
        sm.registerListener(this, s1, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, s2, SensorManager.SENSOR_DELAY_GAME);

        //Register Broadcast receiver that will accept results from background scanning (**)
        IntentFilter intentFilter = new IntentFilter(BackgroundScanService.ACTION_DEVICE_DISCOVERED);
        registerReceiver(scanningBroadcastReceiver, intentFilter);
    }

    protected void onPause() {
        unregisterReceiver(scanningBroadcastReceiver);
        super.onPause();
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

    private void setupButtons() {
        Button startScanButton = (Button) findViewById(R.id.start_scan_button);
        Button stopScanButton = (Button) findViewById(R.id.stop_scan_button);
        startScanButton.setOnClickListener(this);
        stopScanButton.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_scan_button:
                startBackgroundService();
                break;
            case R.id.stop_scan_button:
                stopBackgroundService();
                break;
        }
    }

    private void startBackgroundService() {
        startService(serviceIntent);
    }

    private void stopBackgroundService() {
        stopService(serviceIntent);
    }

    private final BroadcastReceiver scanningBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Device discovered!
            int devicesCount = intent.getIntExtra(BackgroundScanService.EXTRA_DEVICES_COUNT, 0);
            RemoteBluetoothDevice device = intent.getParcelableExtra(BackgroundScanService.EXTRA_DEVICE);
            statusText.setText(String.format("Total discovered devices: %d\n\nLast scanned device:\n%s", devicesCount, device.toString()));
        }
    };
}