package it.unipi.sample.samples;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;

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
  private int systemStepCount = 0;
  private XYPlot plot;
  private FilteredData fd;
  private int debug_counter = 0;
  private int debug_step_counter = 0;

  private boolean first_rilevation = true;

  private int last_rssi;
  private int RSSI_THRESHOLD = -100;
  private double USER_SPEED_THRESHOLD = 10;
  private long last_timestamp;

  private static Context context;

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if(ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
      //ask for permission
      requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
    }

    registerBroadcastReceiver();


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

    TextView textView = (TextView) findViewById(R.id.debug_text_view);
    textView.setText("ON_CREATE");
  }

  private void sensorSetup(){
    sm = (SensorManager)getSystemService(SENSOR_SERVICE);
    s1 = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    s2 = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    if(s1 == null || s2 == null)
    {
      Log.d(TAG, "Sensor(s) unavailable");
      finish();
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
      systemStepCount++;
      TextView textView = (TextView) findViewById(R.id.step_counter_text);
      textView.setText(String.format("%d", systemStepCount));

      TextView textView3 = (TextView) findViewById(R.id.debug_text_view2);
      textView3.setText("TYPE_STEP_DETECTOR" + debug_step_counter++);
    } else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
      fd.addToQueue(event);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    TextView textView = (TextView) findViewById(R.id.debug_text_view);
    textView.setText("ON_ACCURACY_CHANGED");
    Log.i(TAG, "Accuracy changed");
  }

  @Override
  protected void onPause() {
    TextView textView = (TextView) findViewById(R.id.debug_text_view);
    textView.setText("ON_PAUSE");
    unregisterReceiver(scanningBroadcastReceiver);
    super.onPause();
    sm.unregisterListener(this);
  }

  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {
    TextView textView = (TextView) findViewById(R.id.debug_text_view);
    textView.setText("ON_POINTERCAPTURECHANGED");
    super.onPointerCaptureChanged(hasCapture);
  }

  @Override
  protected void onResume() {
    TextView textView = (TextView) findViewById(R.id.debug_text_view);
    textView.setText("ON_RESUME");

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

  private void registerBroadcastReceiver() {

    final IntentFilter theFilter = new IntentFilter();
    /** System Defined Broadcast */
    theFilter.addAction(Intent.ACTION_SCREEN_ON);
    theFilter.addAction(Intent.ACTION_SCREEN_OFF);
    theFilter.addAction(Intent.ACTION_USER_PRESENT);
    BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String strAction = intent.getAction();
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if(strAction.equals(Intent.ACTION_USER_PRESENT) || strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON)  )
          if( myKM.inKeyguardRestrictedInputMode())
          {
            System.out.println("Screen off " + "LOCKED");
            stopBackgroundService();
          } else
          {
            System.out.println("Screen off " + "UNLOCKED");
          }
      }
    };
    getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
  }

  private boolean isTheUserWalkingTowardsBeacon(long timestamp, int rssi){
    // if the user is further wrt beacon compared its previous location the user is not walking
    // towards beacon
    if(rssi < last_rssi) return false;
    double user_speed = (rssi - last_rssi)/(timestamp - last_timestamp);

    if(user_speed > USER_SPEED_THRESHOLD)
      return true;
    return false;
  }

  private final BroadcastReceiver scanningBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      //Device discovered!
      int devicesCount = intent.getIntExtra(BackgroundScanService.EXTRA_DEVICES_COUNT, 0);
      RemoteBluetoothDevice device = intent.getParcelableExtra(BackgroundScanService.EXTRA_DEVICE);

      TextView textView3 = (TextView) findViewById(R.id.debug_text_view);
      textView3.setText("RSSI: " + device.getRssi());

      if(first_rilevation){
        first_rilevation = false;
        last_rssi = device.getRssi();
        last_timestamp = device.getTimestamp();
      }
      else{
        // I check if new device is nearest wrt last one
        if(device.getRssi() > last_rssi){
          last_rssi = device.getRssi();

          // I check if new device is too near
          if(last_rssi > RSSI_THRESHOLD){
            // I check user activity
            if(isTheUserWalkingTowardsBeacon(device.getTimestamp(), device.getRssi())){

            }
          }
        }
      }
      statusText.setText(String.format("Total discovered devices: %d\n\nLast scanned device:\n%s", devicesCount, device.toString()));
      stopBackgroundService();
      startBackgroundService();
    }
  };
}
