package com.app.droidcast;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
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
import com.app.droidcast.ioc.IOCProvider;
import com.app.droidcast.utils.BounceView;
import com.app.droidcast.utils.MetaDataProvider;
import com.app.droidcast.utils.NotificationUtils;
import com.app.droidcast.utils.NsdUtils;
import com.app.droidcast.utils.Utils;
import java.util.Random;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class MediaShareActivity extends BaseActivity implements Session.Callback {

  @Inject NsdUtils nsdUtils;
  @Inject NotificationUtils notificationUtils;
  @Inject AudioManager audioManager;
  @Inject MetaDataProvider metaDataProvider;

  private static final int REQUEST_CODE = 1000;
  public static final String USERNAME = "droidCast";
  private static final String INTENT_KEY_STOP = "intent_key_stop";

  private MediaProjectionManager mProjectionManager;
  private MediaProjection mMediaProjection;
  private static final int REQUEST_PERMISSIONS = 10;
  private RtspServer rtspServerService;
  private boolean bound = false;
  private String code;
  private String linkURL = null;

  private boolean isStreaming = false;
  private int clientsCount = 0;

  @BindView(R.id.code_wrapper) LinearLayout codeWrapper;
  @BindView(R.id.media_share_code_textview) TextView mediaShareCodeTextView;
  @BindView(R.id.media_share_progress) ProgressBar mediaShareProgressBar;
  @BindView(R.id.media_share_pc_link_button) Button sharePCLinkButton;
  @BindView(R.id.media_share_separator) View shareSeparator;

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

  /**
   * Creae an intent that stops the server.
   *
   * @param context Application context.
   * @return Intent
   */
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

    // Let's mute microphone by default
    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    audioManager.setMicrophoneMute(true);
    mProjectionManager =
        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    askForPermission();
    Random rnd = new Random();
    code = Integer.toString(100000 + rnd.nextInt(900000));
    mediaShareCodeTextView.setText(code);
    buildLinkURL();
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent.getBooleanExtra(INTENT_KEY_STOP, false)) {
      stopServer();
      startActivity(SessionFinishActivity.createIntent(this, false));
      if (!isFinishing()) {
        finish();
      }
    }
  }

  private void stopServer() {
    if (mMediaProjection != null) {
      mMediaProjection.stop();
      mMediaProjection = null;
    }
    nsdUtils.tearDown();
    stopService(new Intent(this, RtspServer.class));
    notificationUtils.cancelStreamNotification();
    // Put microphone back to its normal state
    audioManager.setMode(AudioManager.MODE_NORMAL);
    audioManager.setMicrophoneMute(false);
    isStreaming = false;
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
      if (mProjectionManager != null) {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
      }
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
        .setAudioEncoder(SessionBuilder.AUDIO_AAC)
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
    notificationUtils.buildStreamNotification(code);
  }

  private void buildLinkURL() {
    String ip = Utils.getIPAddress(true);
    if (ip != null) {
      linkURL = "rtsp://"
          + USERNAME
          + ":"
          + Utils.MD5(metaDataProvider.getRtspKey() + code)
          + "@"
          + ip
          + ":"
          + nsdUtils.getAvailablePort();
    } else {
      sharePCLinkButton.setVisibility(View.GONE);
      shareSeparator.setVisibility(View.GONE);
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
    ++clientsCount;
    if (isStreaming) {
      notificationUtils.newConnection();
    } else {
      startActivity(StreamingStartedActivity.createIntent(this));
    }
    notificationUtils.updateStreamNotification(clientsCount);
    isStreaming = true;
  }

  @Override public void onSessionStopped() {
    if (clientsCount > 0) {
      --clientsCount;
    }
    notificationUtils.updateStreamNotification(clientsCount);
    Log.d(TAG, "[MediaShareActivity] - onSessionStopped()");
  }

  @Override protected void onDestroy() {
    stopServer();
    super.onDestroy();
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
      String password = Utils.MD5(metaDataProvider.getRtspKey() + code);
      rtspServerService.setAuthorization(USERNAME, password);
    }

    @Override public void onServiceDisconnected(ComponentName name) {
      bound = false;
    }
  };

  /**
   * Handler for Share PC Link event.
   *
   * @param v View that receives the event.
   */
  @OnClick(R.id.media_share_pc_link_button) public void onSharePCLinkClick(View v) {
    BounceView.animate(v, new Runnable() {
      @Override public void run() {
        shareLink(getString(R.string.media_share_text_pc, linkURL));
      }
    });
  }

  /**
   * Handler for Share App Link click event.
   *
   * @param v View that receives the event.
   */
  @OnClick(R.id.media_share_app_link_button) public void onShareAppLinkClick(View v) {
    BounceView.animate(v, new Runnable() {
      @Override public void run() {
        String appLink = "http://app.droidcast.com/?code=" + code;
        shareLink(getString(R.string.media_share_text_app, appLink));
      }
    });
  }

  /**
   * Share the link with apps that support it.
   *
   * @param link Link to share, app or pc.
   */
  private void shareLink(String link) {
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.setType("text/plain");
    sendIntent.putExtra(Intent.EXTRA_TEXT, link);
    startActivity(Intent.createChooser(sendIntent, getString(R.string.media_share_chooser_title)));
  }
}
