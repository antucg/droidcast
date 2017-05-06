package com.antonio.droidcast;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.utils.NsdUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.screen.MediaCodecUtils;

public class MediaShareActivity extends BaseActivity implements Session.Callback {

  @Inject NsdUtils nsdUtils;

  private static final int REQUEST_CODE = 1000;
  public static final String USERNAME = "d";
  private static final int NOTIFICATION_ID = 1;
  private static final String INTENT_KEY_STOP = "intent_key_stop";

  private MediaProjectionManager mProjectionManager;
  private MediaProjection mMediaProjection;
  private static final int REQUEST_PERMISSIONS = 10;
  private RtspServer rtspServerService;
  private boolean bound = false;
  private String code;

  @BindView(R.id.code_wrapper) LinearLayout codeWrapper;
  @BindView(R.id.media_share_code_textview) TextView mediaShareCodeTextView;
  @BindView(R.id.media_share_waiting_text) TextView mediaShareWaitingText;
  @BindView(R.id.media_share_progress) ProgressBar mediaShareProgressBar;
  @BindView(R.id.media_share_client_connected) TextView mediaShareClientConnected;

  /**
   * Create an intent that opens this activity
   *
   * @param context Activity that opens this one.
   * @return Intent
   */
  public static Intent createIntent(Context context) {
    Intent intent = new Intent(context, MediaShareActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return intent;
  }

  public static Intent createStopIntent(Context context) {
    Intent intent = new Intent(context, MediaShareActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.putExtra(INTENT_KEY_STOP, true);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_share);

    ButterKnife.bind(this);
    IOCProvider.getInstance().inject(this);

    mProjectionManager =
        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    askForPermission();
    Random rnd = new Random();
    code = Integer.toString(100000 + rnd.nextInt(900000));
    mediaShareCodeTextView.setText(code);
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent.getBooleanExtra(INTENT_KEY_STOP, false)) {
      stopService(new Intent(this, RtspServer.class));
    }
  }

  private void askForPermission() {
    if (ContextCompat.checkSelfPermission(MediaShareActivity.this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(MediaShareActivity.this,
          Manifest.permission.RECORD_AUDIO)) {
        Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions,
            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE", new View.OnClickListener() {
          @Override public void onClick(View v) {
            ActivityCompat.requestPermissions(MediaShareActivity.this, new String[] {
                Manifest.permission.RECORD_AUDIO
            }, REQUEST_PERMISSIONS);
          }
        }).show();
      } else {
        ActivityCompat.requestPermissions(MediaShareActivity.this, new String[] {
            Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSIONS);
      }
    } else {
      buildMediaProjection();
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_PERMISSIONS: {
        if ((grantResults.length == 1) && (grantResults[0]) == PackageManager.PERMISSION_GRANTED) {
          buildMediaProjection();
        } else {
          Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions,
              Snackbar.LENGTH_INDEFINITE).setAction("ENABLE", new View.OnClickListener() {
            @Override public void onClick(View v) {
              Intent intent = new Intent();
              intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              intent.addCategory(Intent.CATEGORY_DEFAULT);
              intent.setData(Uri.parse("package:" + getPackageName()));
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
              intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
              startActivity(intent);
            }
          }).show();
        }
      }
    }
  }

  private void buildMediaProjection() {
    if (mMediaProjection == null) {
      startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
      return;
    }
    initSession();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_CODE) {
      Log.e(TAG, "Unknown request code: " + requestCode);
      return;
    }
    if (resultCode != RESULT_OK) {
      Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    MediaProjectionCallback mMediaProjectionCallback = new MediaProjectionCallback();
    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
    initSession();
  }

  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override public void onStop() {
      Log.i(TAG, "MediaProjectionCallback onStop");
    }
  }

  private void initSession() {

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    // Configures the SessionBuilder
    SessionBuilder.getInstance()
        .setCallback(this)
        .setContext(getApplicationContext())
        .setDisplayMetrics(metrics)
        .setMediaProjection(mMediaProjection)
        .setAudioEncoder(SessionBuilder.AUDIO_NONE)
        .setVideoEncoder(SessionBuilder.SCREEN_H264);

    // Starts the RTSP server
    bindService(new Intent(this, RtspServer.class), serviceConnection, Context.BIND_AUTO_CREATE);
    startService(new Intent(this, RtspServer.class));
    nsdUtils.registerNsdService(this, code);
    buildNotificationControl();
  }

  private void buildNotificationControl() {

    Intent stopIntent = createStopIntent(this);
    PendingIntent stopPendingIntent =
        PendingIntent.getActivity(this, (int) System.currentTimeMillis(), stopIntent,
            PendingIntent.FLAG_ONE_SHOT);

    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.notification_bar_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.manage_streaming))
            .addAction(R.drawable.ic_media_stop, getString(R.string.notification_stop),
                stopPendingIntent);
    Intent resultIntent = MediaShareActivity.createIntent(this);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addParentStack(HomeActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
  }

  @Override protected void onStop() {
    super.onStop();
    if (bound) {
      unbindService(serviceConnection);
      bound = false;
    }
  }

  @Override public void onBitrateUpdate(long bitrate) {
  }

  @Override public void onSessionError(int message, int streamType, Exception e) {
    if (e != null) {
      logError(e.getMessage());
    }
  }

  @Override public void onPreviewStarted() {
    Log.d(TAG, "[MediaShareActivity] - onPreviewStarted()");
  }

  @Override public void onSessionConfigured() {
    Log.d(TAG, "[MediaShareActivity] - onSessionConfigured()");
  }

  @Override public void onSessionStarted() {
    codeWrapper.postDelayed(new Runnable() {
      @Override public void run() {
        codeWrapper.setVisibility(View.GONE);
        mediaShareClientConnected.setVisibility(View.VISIBLE);
      }
    }, 1000);
  }

  @Override public void onSessionStopped() {
    Log.d(TAG, "[MediaShareActivity] - onSessionStopped()");
    nsdUtils.tearDown();
  }

  private void logError(final String msg) {
    final String error = (msg == null) ? "Error unknown" : msg;
    AlertDialog.Builder builder = new AlertDialog.Builder(MediaShareActivity.this);
    builder.setMessage(error).setPositiveButton("OK", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
      }
    });
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override public void onServiceConnected(ComponentName name, IBinder service) {
      RtspServer.LocalBinder binder = (RtspServer.LocalBinder) service;
      rtspServerService = binder.getService();
      bound = true;
      rtspServerService.setAuthorization(USERNAME, code);
    }

    @Override public void onServiceDisconnected(ComponentName name) {
      bound = false;
    }
  };
}
