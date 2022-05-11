package it.unipi.sample.samples;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.XYPlot;
import com.kontakt.sample.R;

import it.unipi.sample.FilteredData;
import it.unipi.sample.service.BackgroundScanService;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import it.unipi.sample.service.*;

/**
 * This is an example of implementing a background scan using Android's Service component.
 */
public class BackgroundScanActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

  public static Intent createIntent(@NonNull Context context) {
    return new Intent(context, BackgroundScanActivity.class);
  }

  private Intent serviceIntent;
  private TextView statusText;

  private SensorManager sm;
  private Sensor s1;
  private Sensor s2;
  private static String TAG = "StepCounterExample";
  private int systemStepCount;
  private XYPlot plot;
  private FilteredData fd;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_background_scan);
    statusText = (TextView) findViewById(R.id.status_text);
    serviceIntent = new Intent(getApplicationContext(), BackgroundScanService.class);

    //Setup Toolbar
    setupToolbar();

    //Setup buttons
    setupButtons();

    // Contain and filter acceleration values
    fd = new FilteredData(this);

    // Setup sensors
    sensorSetup();
  }

  private void sensorSetup(){
    sm = (SensorManager)getSystemService(SENSOR_SERVICE);
    s1 = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    s2 = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    if(s1 == null || s2 == null)
    {
      Log.d(TAG, "Sensor(s) unavailable");
      finish(); // l'attività viene terminata
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
      systemStepCount++;
      TextView textView = (TextView) findViewById(R.id.step_counter_text);
      textView.setText(systemStepCount);
    } else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
      fd.addToQueue(event);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i(TAG, "Accuracy changed");
  }

  @Override
  protected void onPause() {
    unregisterReceiver(scanningBroadcastReceiver);
    super.onPause();
    sm.unregisterListener(this);
  }

  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {
    super.onPointerCaptureChanged(hasCapture);
  }

  @Override
  protected void onResume() {
    super.onResume();
    //Register Broadcast receiver that will accept results from background scanning
    IntentFilter intentFilter = new IntentFilter(BackgroundScanService.ACTION_DEVICE_DISCOVERED);
    registerReceiver(scanningBroadcastReceiver, intentFilter);

    sm.registerListener(this, s1, SensorManager.SENSOR_DELAY_GAME);
    sm.registerListener(this, s2, SensorManager.SENSOR_DELAY_GAME);
  }

  private void setupToolbar() {
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  private void setupButtons() {
    Button startScanButton = (Button) findViewById(R.id.start_scan_button);
    Button stopScanButton = (Button) findViewById(R.id.stop_scan_button);
    startScanButton.setOnClickListener(this);
    stopScanButton.setOnClickListener(this);
  }

  private void startBackgroundService() {
    startService(serviceIntent);
  }

  private void stopBackgroundService() {
    stopService(serviceIntent);
  }

  @Override
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
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
