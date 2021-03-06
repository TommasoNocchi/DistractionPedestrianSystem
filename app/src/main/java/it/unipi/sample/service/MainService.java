package it.unipi.sample.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import com.kontakt.sample.R;

public class MainService extends Service implements View.OnTouchListener {

  private static final String TAG = MainService.class.getSimpleName();

  private WindowManager windowManager;

  private View floatyView;

  @Override
  public IBinder onBind(Intent intent) {

    return null;
  }

  @Override
  public void onCreate() {

    super.onCreate();

    windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

    addOverlayView();
  }

  private void addOverlayView() {

    final LayoutParams params;
    int layoutParamsType;

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      layoutParamsType = LayoutParams.TYPE_APPLICATION_OVERLAY;
    }
    else {
      layoutParamsType = LayoutParams.TYPE_PHONE;
    }

    params = new LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT,
        layoutParamsType,
        0,
        PixelFormat.TRANSLUCENT);

    params.gravity = Gravity.CENTER | Gravity.START;
    params.x = 0;
    params.y = 0;

    FrameLayout interceptorLayout = new FrameLayout(this) {

      @Override
      public boolean dispatchKeyEvent(KeyEvent event) {

        // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
        if (event.getAction() == KeyEvent.ACTION_DOWN) {

          // Check if the HOME button is pressed
          if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            Log.v(TAG, "BACK Button Pressed");

            // As we've taken action, we'll return true to prevent other apps from consuming the event as well
            return true;
          }
        }

        // Otherwise don't intercept the event
        return super.dispatchKeyEvent(event);
      }
    };

    LayoutInflater inflater = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE));

    if (inflater != null) {
      floatyView = inflater.inflate(R.layout.floating_view, interceptorLayout);
      floatyView.setOnTouchListener(this);
      windowManager.addView(floatyView, params);
    }
    else {
      Log.e("SAW-example", "Layout Inflater Service is null; can't inflate and display R.layout.floating_view");
    }
  }

  @Override
  public void onDestroy() {

    super.onDestroy();

    if (floatyView != null) {

      windowManager.removeView(floatyView);

      floatyView = null;
    }
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    view.performClick();

    Log.v(TAG, "onTouch...");

    // Kill service
    onDestroy();

    return true;
  }
}
