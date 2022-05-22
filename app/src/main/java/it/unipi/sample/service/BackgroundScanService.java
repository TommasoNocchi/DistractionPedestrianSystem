package it.unipi.sample.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.kontakt.sample.R;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import it.unipi.sample.MainActivity;
import it.unipi.sample.samples.common.Message;

public class BackgroundScanService extends Service implements SensorEventListener {

  public static final String TAG = "BackgroundScanService";
  public static final String ACTION_DEVICE_DISCOVERED = "DeviceDiscoveredAction";
  public static final String EXTRA_DEVICE = "DeviceExtra";
  public static final String EXTRA_DEVICES_COUNT = "DevicesCountExtra";

  private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

  private final Handler handler = new Handler();
  private ProximityManager proximityManager;
  public static boolean isRunning; // Flag indicating if service is already running.
  private int devicesCount; // Total discovered devices count

  private SensorManager sm;
  private Sensor s1;
  private Sensor s2;
  private TextView statusText;

  private double systemStepCount = 0;
  private int debugStepCounter = 0;
  private boolean firstMessage = true;
  private long lastNotificationTimestamp = -1;

  // steps by second thresholds
  private double MIN_STEP_SPEED_THRESHOLD = 0.0005;
  private double MAX_STEP_SPEED_THRESHOLD = 0.02;
  private double RSSI_THRESHOLD = -100;
  // user speed thresholds
  private double MIN_USER_SPEED_THRESHOLD = 0.0005;
  private double MAX_USER_SPEED_THRESHOLD = 0.035;
  private int MEASURED_POWER = -69;
  private int ENVIROMENT_FACTOR_CONSTANT = 4;

  private long MIN_INTERVAL_BETWEEN_NOTIFICATIONS = 30000; // 30 seconds

  private boolean isInThePreAlert = false;
  private ArrayList<Message> lastMessages;
  private ArrayList<RemoteBluetoothDevice> encountered_devs = new ArrayList<>();

  @Override
  public void onCreate() {
    super.onCreate();
    setupProximityManager();
    isRunning = false;

    lastMessages = new ArrayList<>();

    // Setup sensors
    sensorSetup();
  }

  private void setupProximityManager() {
    //Create proximity manager instance
    proximityManager = ProximityManagerFactory.create(this);

    //Configure proximity manager basic options
    proximityManager.configuration()
        //Using ranging for continuous scanning or MONITORING for scanning with intervals
        .scanPeriod(ScanPeriod.RANGING)
        //Using BALANCED for best performance/battery ratio
        .scanMode(ScanMode.BALANCED);

    //Setting up iBeacon and Eddystone listeners
    proximityManager.setIBeaconListener(createIBeaconListener());
    proximityManager.setEddystoneListener(createEddystoneListener());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    //Check if service is already active
    if (isRunning) {
      Toast.makeText(this, "Service is already running.", Toast.LENGTH_SHORT).show();
      return START_STICKY;
    }
    startScanning();
    isRunning = true;
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void startScanning() {
    proximityManager.connect(new OnServiceReadyListener() {
      @Override
      public void onServiceReady() {
        proximityManager.startScanning();
        devicesCount = 0;
      }
    });
  }

  private IBeaconListener createIBeaconListener() {
    return new SimpleIBeaconListener() {
      @Override
      public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
        onDeviceDiscovered(ibeacon);
        Log.i(TAG, "onIBeaconDiscovered: " + ibeacon.toString());
      }

      @Override
      public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) { //function to detect the update of the beacon
        for(IBeaconDevice ib: iBeacons){
          onDeviceDiscovered(ib);
        }
        Log.i(TAG, "onIBeaconsUpdated: " + iBeacons.size());
      }
    };
  }

  private EddystoneListener createEddystoneListener() {
    return new SimpleEddystoneListener() {
      @Override
      public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
        onDeviceDiscovered(eddystone);
        Log.i(TAG, "onEddystoneDiscovered: " + eddystone.toString());
      }
    };
  }

  private void onDeviceDiscovered(RemoteBluetoothDevice device) {
    devicesCount++;
    detectAlert(device);
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (proximityManager != null) {
      proximityManager.disconnect();
      proximityManager = null;
    }
    super.onDestroy();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i(TAG, "Accuracy changed");
  }

  private void sensorSetup(){
    sm = (SensorManager)getSystemService(SENSOR_SERVICE);
    s1 = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    if(s1 == null)
    {
      Log.d(TAG, "Sensor(s) unavailable");
    }
    else{
      sm.registerListener(this, s1, SensorManager.SENSOR_DELAY_GAME);
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
      systemStepCount++;
      isInThePreAlert = true;
      System.out.println("StepCount value: "+systemStepCount);
    }
  }

  private void addMessage(Message newMessage){
    Message message;
    for(int i = 0; i< lastMessages.size(); i++){
      message = lastMessages.get(i);
      if(message.getNodeId().equals(newMessage.getNodeId())){
        lastMessages.get(i).setRssi(newMessage.getRssi());
        lastMessages.get(i).setTimestamp(newMessage.getTimestamp());
        return;
      }
    }
    lastMessages.add(newMessage);
  }

  private Message getMessage(String nodeId){
    Message message;
    for(int i = 0; i< lastMessages.size(); i++){
      message = lastMessages.get(i);
      if(message.getNodeId().equals(nodeId))
        return message;
    }
    return null;
  }

  private void detectAlert(RemoteBluetoothDevice device){
    double deviceRssi = device.getRssi();
    long deviceTimestamp = device.getTimestamp();
    Message message;
    if(firstMessage){
      firstMessage = false;
      message = new Message(device.getUniqueId(), device.getRssi(), device.getTimestamp(), systemStepCount);
      lastMessages.add(message);
    }
    else{
      message = getMessage(device.getUniqueId());
      if(message == null)
        message = new Message(device.getUniqueId(), device.getRssi(), device.getTimestamp(), systemStepCount);

      else
      {
        // I check if new device is nearest wrt last one. I check also if the speed is too high,
        // in such case there was an error in the message
        if(deviceTimestamp !=  message.getTimestamp() &&
                (deviceRssi - message.getRssi())/(deviceTimestamp - message.getTimestamp()) > MIN_USER_SPEED_THRESHOLD &&
                (deviceRssi - message.getRssi())/(deviceTimestamp - message.getTimestamp()) < MAX_USER_SPEED_THRESHOLD){

          // I check if new device is in alert zone
          if(deviceRssi > RSSI_THRESHOLD)
          {
            int reqCode = 1;
            if(lastNotificationTimestamp == -1){
              Intent intent = new Intent(getApplicationContext(), MainActivity.class);
              showNotification(this, "Distraction pedestrian system",
                      "Pay attention to the surrounding environment", intent, reqCode);
              lastNotificationTimestamp = deviceTimestamp;
            }

            else if(deviceTimestamp - lastNotificationTimestamp > MIN_INTERVAL_BETWEEN_NOTIFICATIONS){
              Intent intent = new Intent(getApplicationContext(), MainActivity.class);
              showNotification(this, "Distraction pedestrian system",
                      "Pay attention to the surrounding environment", intent, reqCode);
              lastNotificationTimestamp = deviceTimestamp;
            }

            // I check if the user is walking. I check also if he has performed too steps due to step counter errors.
            if((systemStepCount - message.getStepCount())/(deviceTimestamp - message.getTimestamp()) < MAX_STEP_SPEED_THRESHOLD &&
                    (systemStepCount - message.getStepCount())/(deviceTimestamp - message.getTimestamp()) > MIN_STEP_SPEED_THRESHOLD){
              WriteFile(device.getAddress());
              launchMainService();
              // added to encountered devices
              encountered_devs.add(device);
            }
          }
        }
        message.setRssi(deviceRssi);
        message.setTimestamp(deviceTimestamp);
        message.setStepCount(systemStepCount);
      }
      addMessage(message);
    }
  }

  private void launchMainService() {
    Intent svc = new Intent(this, MainService.class);

    stopService(svc);
    startService(svc);
  }

  public void showNotification(Context context, String title, String message, Intent intent, int reqCode) {

    PendingIntent pendingIntent = PendingIntent.getActivity(context, reqCode, intent, PendingIntent.FLAG_IMMUTABLE);
    String CHANNEL_ID = "channel_name";// The id of the channel.
    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent);
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = "Channel Name";// The user-visible name of the channel.
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
      notificationManager.createNotificationChannel(mChannel);
    }
    notificationManager.notify(reqCode, notificationBuilder.build()); // 0 is the request code, it should be unique id

    Log.d("showNotification", "showNotification: " + reqCode);
  }


  public void WriteFile(String info) {
    // add-write text into file
    try {
      FileOutputStream fileout=openFileOutput("PedestrianSystemLogHistoryFile.txt", MODE_APPEND);
      OutputStreamWriter outputWriter=new OutputStreamWriter(fileout);
      String tmp = new Timestamp(System.currentTimeMillis()).toString();
      outputWriter.write(tmp + "\n" + info +"\n");
      outputWriter.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
