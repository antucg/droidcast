package com.antonio.droidcast;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
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
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.utils.BounceView;
import com.antonio.droidcast.utils.NsdUtils;
import com.antonio.droidcast.utils.Utils;
import java.util.Random;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class MediaShareActivity extends BaseActivity implements Session.Callback {

  @Inject NsdUtils nsdUtils;

  private static final int REQUEST_CODE = 1000;
  public static final String USERNAME = "d";
  public static final int NOTIFICATION_ID = 1;
  private static final String INTENT_KEY_STOP = "intent_key_stop";

  private MediaProjectionManager mProjectionManager;
  private MediaProjection mMediaProjection;
  private static final int REQUEST_PERMISSIONS = 10;
  private RtspServer rtspServerService;
  private boolean bound = false;
  private String code;
  private NotificationManager notificationManager;
  private String linkURL = null;

  private boolean isStreaming = false;

  @BindView(R.id.code_wrapper) LinearLayout codeWrapper;
  @BindView(R.id.media_share_code_textview) TextView mediaShareCodeTextView;
  @BindView(R.id.media_share_progress) ProgressBar mediaShareProgressBar;
  @BindView(R.id.media_share_copy_link_textview) TextView copyLinkTextView;
  @BindView(R.id.media_share_copy_link_button) Button copyLinkButton;

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
    //code = Integer.toString(100000 + rnd.nextInt(900000));
    code = "1";
    mediaShareCodeTextView.setText(code);
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent.getBooleanExtra(INTENT_KEY_STOP, false)) {
      stopServer();
      startActivity(SessionFinishActivity.createIntent(this, false));
    }
  }

  private void stopServer() {
    if (mMediaProjection != null) {
      mMediaProjection.stop();
      mMediaProjection = null;
    }
    nsdUtils.tearDown();
    stopService(new Intent(this, RtspServer.class));
    notificationManager.cancel(NOTIFICATION_ID);
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
    Intent intent = new Intent(this, RtspServer.class);
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    startService(intent);

    // Register NSD service so device can be found on the network
    nsdUtils.registerNsdService(this, code, new NsdManager.RegistrationListener() {
      @Override public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "[NsdUtils] - onRegistrationFailed, " + errorCode);
        stopServer();
        Toast.makeText(MediaShareActivity.this, getString(R.string.media_share_nsd_error),
            Toast.LENGTH_LONG).show();
        finish();
      }

      @Override public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "[NsdUtils] - onUnregistrationFailed, " + errorCode);
      }

      @Override public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "[NsdUtils] - onServiceRegistered");
      }

      @Override public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "[NsdUtils] - onServiceUnregistered");
      }
    });

    // Build status bar notification
    buildNotificationControl();
    buildLinkURL();
  }

  private void buildNotificationControl() {

    Intent stopIntent = createStopIntent(this);
    PendingIntent stopPendingIntent =
        PendingIntent.getActivity(this, (int) System.currentTimeMillis(), stopIntent,
            PendingIntent.FLAG_ONE_SHOT);

    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(this).setSmallIcon(R.drawable.notification_bar_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.manage_streaming, code))
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_media_stop, getString(R.string.notification_stop),
                stopPendingIntent);
    Intent resultIntent = MediaShareActivity.createIntent(this);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addParentStack(HomeActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
  }

  private void buildLinkURL() {
    String ip = Utils.getIPAddress(true);
    if (ip != null) {
      linkURL = "rtsp://" + USERNAME + ":" + code + "@" + ip + ":" + nsdUtils.getAvailablePort();
    } else {
      copyLinkTextView.setVisibility(View.GONE);
      copyLinkButton.setVisibility(View.GONE);
    }
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (!isStreaming) {
        stopServer();
      }
    }
    return super.onKeyDown(keyCode, event);
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
    if (isStreaming) {
      return;
    }

    isStreaming = true;
    startActivity(StreamingStartedActivity.createIntent(this));
  }

  @Override public void onSessionStopped() {
    Log.d(TAG, "[MediaShareActivity] - onSessionStopped()");
    isStreaming = false;
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

  @OnClick(R.id.media_share_copy_link_button) public void onCopyLinkClick(View v) {
    BounceView.animate(v, new Runnable() {
      @Override public void run() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(linkURL, linkURL);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(MediaShareActivity.this, R.string.media_share_link_copied, Toast.LENGTH_LONG).show();
      }
    });
  }
}
