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

import java.util.ArrayList;

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
  private int debugStepCounter = 0;

  private boolean firstRilevation = true;

  private int last_rssi;
  private int RSSI_THRESHOLD = -100;
  private double USER_SPEED_THRESHOLD = 10;
  private int MEASURED_POWER = -69;
  private int ENVIROMENT_FACTOR_CONSTANT = 2; //Range 2-4: 2 = Low-strength
  private long last_timestamp;

  private boolean isInThePreAlert = false;
  private int last_step_count;

  private static Context context;
  private ArrayList<RemoteBluetoothDevice> encounteredDevs = new ArrayList<>(); // DA DECIDERE
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
      textView3.setText("TYPE_STEP_DETECTOR" + debugStepCounter++);

      isInThePreAlert = true;
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
        if(strAction.equals(Intent.ACTION_SCREEN_OFF)) {
          System.out.println("Screen off");
          stopBackgroundService();
        }
        if(strAction.equals(Intent.ACTION_SCREEN_ON) ){
          System.out.println("Screen on");
          startBackgroundService();
        }

        /*if(strAction.equals(Intent.ACTION_USER_PRESENT) && !myKM.isKeyguardLocked()){
          System.out.println("Device locked");
          stopBackgroundService();
        }
        else
          System.out.println("Device unlocked");

         */

      }
    };

    getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
  }

  private boolean isTheUserWalkingTowardsBeacon(long timestamp, int rssi){

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
      int deviceRSSI = device.getRssi();


      TextView textView3 = (TextView) findViewById(R.id.debug_text_view);
      textView3.setText("RSSI: " + deviceRSSI);

      if(firstRilevation){
        firstRilevation = false;
        last_rssi = deviceRSSI;
        last_timestamp = device.getTimestamp();
        last_step_count = systemStepCount;
      }
      else{
        // I check if new device is nearest wrt last one
        if(deviceRSSI > last_rssi){ //(FORSE si può levare perch* c'è): ASCOLTARE AUDIO MINUTO 19:00 13/05/22 CHE MOTIVA DI TENERE QUESTO IF
          // I check if new device is too near
          if(deviceRSSI > RSSI_THRESHOLD){ //PRE-ALERT

            /**
            //Soluzione 1 (PER ORA NON SI GESTISCE IL CASO SE ENTRA NEL PRE ALERT MA POI CI ESCE, QUINDI RILEVA UNO STEP COUNT E MANDA ERRONAMENTE L'ALERT)
            isInThePreAlert=false;
            while(!isInThePreAlert && deviceRSSI > RSSI_THRESHOLD){
              //rimane qui
            }
            //quando esce mandare ALERT
            */


            if(systemStepCount> last_step_count){ //Sta camminando

              //ALERT mettere metri dal possibile pericolo per vedere che si sta avvicinando sempre di più
              
              
              //Aggiungo in array locale dell'applicazione così da supportare una possibile implentazione di un log/history

              last_step_count = systemStepCount;
            }

            // I check user activity
            if(isTheUserWalkingTowardsBeacon(device.getTimestamp(), deviceRSSI)){

            }
          }
          last_rssi = deviceRSSI;
        }
      }
      
      statusText.setText(String.format("Total discovered devices: %d\n\nLast scanned device:\n%s", devicesCount, device.toString()));
      stopBackgroundService();
      startBackgroundService();
    }
  };

  private double fromRSSItoMeter(int deviceRSSI){
    return Math.pow(10, (MEASURED_POWER-deviceRSSI)/(10*ENVIROMENT_FACTOR_CONSTANT));
  }
}
