package it.unipi.sample;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.kontakt.sample.R;

import it.unipi.sample.samples.BackgroundScanActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  public static final int REQUEST_CODE_PERMISSIONS = 100;

  private LinearLayout buttonsLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupButtons();
    checkPermissions();
    int reqCode = 1;
    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
  }


  //Setting up buttons and listeners.
  private void setupButtons() {
    buttonsLayout = findViewById(R.id.buttons_layout);

    final Button backgroundScanButton = findViewById(R.id.button_scan_background);

    backgroundScanButton.setOnClickListener(this);
  }

  //Since Android Marshmallow starting a Bluetooth Low Energy scan requires permission from location group.
  private void checkPermissions() {
    String[] requiredPermissions = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
            : new String[]{ Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION };
    if(isAnyOfPermissionsNotGranted(requiredPermissions)) {
      ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS);
    }
  }
  /*
  private void checkPermissions() {
    String[] requiredPermissions = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
            : new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION };
    if(isAnyOfPermissionsNotGranted(requiredPermissions)) {
      ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS);
    }
  }
   */

  private boolean isAnyOfPermissionsNotGranted(String[] requiredPermissions){
    for(String permission: requiredPermissions){
      int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, permission);
      if(PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult){
        return true;
      }
    }
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      if (REQUEST_CODE_PERMISSIONS == requestCode) {
        Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
      }
    } else {
      disableButtons();
      Toast.makeText(this, "Location permissions are mandatory to use BLE features on Android 6.0 or higher", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.button_scan_background:
        startActivity(BackgroundScanActivity.createIntent(this));
        break;
    }
  }

  private void disableButtons() {
    for (int i = 0; i < buttonsLayout.getChildCount(); i++) {
      buttonsLayout.getChildAt(i).setEnabled(false);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  protected void onResume() {
    super.onResume();

    if (!Settings.canDrawOverlays(this))
      // Launch service right away - the user has already previously granted permission
      checkDrawOverlayPermission();
  }
  private final static int REQUEST_CODE = 10101;

  @RequiresApi(api = Build.VERSION_CODES.M)
  private void checkDrawOverlayPermission() {

    // Checks if app already has permission to draw overlays
    if (!Settings.canDrawOverlays(this)) {

      // If not, form up an Intent to launch the permission request
      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));

      // Launch Intent, with the supplied request code
      startActivityForResult(intent, REQUEST_CODE);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // Check if a request code is received that matches that which we provided for the overlay draw request
    if (requestCode == REQUEST_CODE) {
      // Double-check that the user granted it, and didn't just dismiss the request
      if (!Settings.canDrawOverlays(this))
        Toast.makeText(this, "Sorry. Can't draw overlays without permission...", Toast.LENGTH_SHORT).show();
    }
  }
}
