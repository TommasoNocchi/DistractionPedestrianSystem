package it.unipi.sample.samples;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kontakt.sample.R;

import it.unipi.sample.service.BackgroundScanService;

import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * This is an example of implementing a background scan using Android's Service component.
 */
public class BackgroundScanActivity extends AppCompatActivity implements View.OnClickListener {

  public static Intent createIntent(@NonNull Context context) {
    return new Intent(context, BackgroundScanActivity.class);
  }

  private Intent serviceIntent;
  private TextView statusText;

  private static String TAG = "StepCounterExample";

  private static Context context;
  private ArrayList<RemoteBluetoothDevice> encountered_devs = new ArrayList<>();
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
    serviceIntent = new Intent(getApplicationContext(), BackgroundScanService.class);
    //Setup Toolbar
    setupToolbar();

    //Setup buttons
    setupButtons();

    Button scanButton = (Button) findViewById(R.id.scan_button);
    if(BackgroundScanService.isRunning)
      scanButton.setText("Stop monitoring");
    else
      scanButton.setText("Start monitoring");
  }



  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {
    super.onPointerCaptureChanged(hasCapture);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  private void setupToolbar() {
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  private void setupButtons() {
    Button startScanButton = (Button) findViewById(R.id.scan_button);
    startScanButton.setOnClickListener(this);
  }

  private void startBackgroundService() {
    startService(serviceIntent);
  }

  private void stopBackgroundService() {
    stopService(serviceIntent);
  }

  @Override
  public void onClick(View view) {
    if(view.getId() == R.id.scan_button){
      Button scanButton = (Button) findViewById(R.id.scan_button);
      if(scanButton.getText().equals("Start monitoring")){
        scanButton.setText("Stop monitoring");
        startBackgroundService();
      }
      else {
        scanButton.setText("Start monitoring");
        stopBackgroundService();
      }
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
      }
    };
    getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    //writeFileHistory(context);
  }
}
