package com.antonio.droidcast;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import com.antonio.droidcast.ioc.IOCProvider;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class MediaShareActivity extends BaseActivity implements Session.Callback {

  private static final int REQUEST_CODE = 1000;
  private MediaProjectionManager mProjectionManager;
  private static final int DISPLAY_WIDTH = 1920;
  private static final int DISPLAY_HEIGHT = 1080;
  private MediaProjection mMediaProjection;
  private VirtualDisplay mVirtualDisplay;
  private MediaProjectionCallback mMediaProjectionCallback;
  private MediaRecorder mMediaRecorder;
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  private static final int REQUEST_PERMISSIONS = 10;
  private static final int BUFFER_SIZE = 1048576;
  private Session mSession;

  private final String VIDEO_PATH =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/video.mp4";

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  /**
   * Create an intent that opens this activity
   *
   * @param context Activity that opens this one.
   * @return Intent
   */
  public static Intent createIntent(Context context) {
    return new Intent(context, MediaShareActivity.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_share);

    IOCProvider.getInstance().inject(this);

    // Get the display size and density.
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    int screenWidth = metrics.widthPixels;
    int screenHeight = metrics.heightPixels;
    int screenDensity = metrics.densityDpi;

    mProjectionManager =
        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    askForPermission();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.media_share_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.stop:
        //mMediaRecorder.stop();
        //stopScreenShare();
        //parseMP4();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void askForPermission() {
    if (ContextCompat.checkSelfPermission(MediaShareActivity.this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat.checkSelfPermission(
        MediaShareActivity.this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(MediaShareActivity.this,
          Manifest.permission.WRITE_EXTERNAL_STORAGE)
          || ActivityCompat.shouldShowRequestPermissionRationale(MediaShareActivity.this,
          Manifest.permission.RECORD_AUDIO)) {
        Snackbar.make(findViewById(android.R.id.content), R.string.label_permissions,
            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE", new View.OnClickListener() {
          @Override public void onClick(View v) {
            ActivityCompat.requestPermissions(MediaShareActivity.this, new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
            }, REQUEST_PERMISSIONS);
          }
        }).show();
      } else {
        ActivityCompat.requestPermissions(MediaShareActivity.this, new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
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
        if ((grantResults.length > 0)
            && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
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
    mMediaProjectionCallback = new MediaProjectionCallback();
    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
    initSession();
  }

  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override public void onStop() {
      Log.i(TAG, "MediaProjectionCallback onStop");
      //stopScreenShare();
    }
  }

  private void initSession() {

    // Sets the port of the RTSP server to 1234
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putString(RtspServer.KEY_PORT, String.valueOf(1235));
    editor.commit();

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    // Configures the SessionBuilder
    SessionBuilder.getInstance()
        .setCallback(this)
        .setPreviewOrientation(90)
        .setContext(getApplicationContext())
        .setDisplayMetrics(metrics)
        .setMediaProjection(mMediaProjection)
        .setAudioEncoder(SessionBuilder.AUDIO_NONE)
        .setVideoEncoder(SessionBuilder.SCREEN_H264);

    // Starts the RTSP server
    this.startService(new Intent(this, RtspServer.class));
  }

  //private void shareScreen() {
  //  initRecorder();
  //  recordScreen();
  //}
  //
  //private void initRecorder() {
  //  try {
  //    mMediaRecorder = new MediaRecorder();
  //    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
  //    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
  //    mMediaRecorder.setOutputFile(VIDEO_PATH);
  //    mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
  //    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
  //    mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
  //    mMediaRecorder.setVideoFrameRate(30);
  //    int rotation = getWindowManager().getDefaultDisplay().getRotation();
  //    int orientation = ORIENTATIONS.get(rotation + 90);
  //    mMediaRecorder.setOrientationHint(orientation);
  //    //mMediaRecorder.setMaxDuration(3000);
  //    mMediaRecorder.prepare();
  //
  //    //mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
  //    //  @Override public void onInfo(MediaRecorder mr, int what, int extra) {
  //    //    Log.d(TAG, "MediaRecorder callback called !");
  //    //    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
  //    //      Log.d(TAG, "MediaRecorder: MAX_DURATION_REACHED");
  //    //    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
  //    //      Log.d(TAG, "MediaRecorder: MAX_FILESIZE_REACHED");
  //    //    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
  //    //      Log.d(TAG, "MediaRecorder: INFO_UNKNOWN");
  //    //    } else {
  //    //      Log.d(TAG, "WTF ?");
  //    //    }
  //    //    mMediaRecorder.stop();
  //    //    mMediaRecorder.release();
  //    //    mMediaRecorder = null;
  //    //    stopScreenShare();
  //    //    parseMP4();
  //    //  }
  //    //});
  //  } catch (IOException e) {
  //    e.printStackTrace();
  //  }

  // TODO: pass media recorder to builder and from there to screenstream
  //mSession = SessionBuilder.getInstance()
  //    .setCallback(this)
  //    //.setSurfaceView(mSurfaceView)
  //    .setDestination("192.168.1.10")
  //    //.setPreviewOrientation(90)
  //    .setCallback(this)
  //    .setContext(getApplicationContext())
  //    .setAudioEncoder(SessionBuilder.AUDIO_NONE)
  //    .setAudioQuality(new AudioQuality(16000, 32000))
  //    .setVideoEncoder(SessionBuilder.SCREEN_H264)
  //    .setVideoQuality(new VideoQuality(320, 240, 20, 500000))
  //    .build();
  //if (!mSession.isStreaming()) {
  //  mSession.configure();
  //}
  //else {
  //  mSession.stop();
  //}

  //private void recordScreen() {
  //  if (mMediaProjection == null) {
  //    startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
  //    return;
  //  }
  //  mVirtualDisplay = createVirtualDisplay();
  //  mMediaRecorder.start();
  //}
  //
  //
  //
  //private VirtualDisplay createVirtualDisplay() {
  //  return mMediaProjection.createVirtualDisplay("MediaShareActivity", DISPLAY_WIDTH,
  //      DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
  //      mMediaRecorder.getSurface(), null /*Callbacks*/, null
  //              /*Handler*/);
  //}

  //public void stopScreenShare() {
  //  Log.v(TAG, "Stopping Recording");
  //
  //  if (mVirtualDisplay == null) {
  //    return;
  //  }
  //  mVirtualDisplay.release();
  //  //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
  //  // be reused again
  //  destroyMediaProjection();
  //}
  //
  //private void destroyMediaProjection() {
  //  if (mMediaProjection != null) {
  //    mMediaProjection.stop();
  //    mMediaProjection.unregisterCallback(mMediaProjectionCallback);
  //    mMediaProjection = null;
  //  }
  //  Log.i(TAG, "MediaProjection Stopped");
  //}
  //
  //private void parseMP4() {
  //  try {
  //    MP4Config config = new MP4Config(VIDEO_PATH);
  //    Log.i(TAG, "config: "
  //        + config.getProfileLevel()
  //        + ","
  //        + config.getB64SPS()
  //        + ","
  //        + config.getB64PPS());
  //  } catch (IOException e) {
  //    e.printStackTrace();
  //  }
  //}

  @Override public void onDestroy() {
    //stopScreenShare();
    stopService(new Intent(this,RtspServer.class));
    super.onDestroy();
  }

  @Override public void onBitrateUpdate(long bitrate) {
    //Log.d(TAG, "Bitrate: " + bitrate);
  }

  @Override public void onSessionError(int message, int streamType, Exception e) {
    if (e != null) {
      logError(e.getMessage());
    }
  }

  @Override public void onPreviewStarted() {
    Log.d(TAG, "onPreviewStarted");
  }

  @Override public void onSessionConfigured() {
    Log.d(TAG, "Preview configured.");
    // Once the stream is configured, you can get a SDP formated session description
    // that you can send to the receiver of the stream.
    // For example, to receive the stream in VLC, store the session description in a .sdp file
    // and open it with VLC while streming.
    //Log.d(TAG, mSession.getSessionDescription());
  }

  @Override public void onSessionStarted() {
    Log.d(TAG, "onSessionStarted");
  }

  @Override public void onSessionStopped() {
    Log.d(TAG, "onSessionStopped");
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

  //private void configureSession() {
  //  // Sets the port of the RTSP server to 1234
  //  SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
  //  editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
  //  editor.commit();
  //
  //  // Configures the SessionBuilder
  //  SessionBuilder.getInstance()
  //      .setSurfaceView()
  //      .setPreviewOrientation(90)
  //      .setContext(getApplicationContext())
  //      .setAudioEncoder(SessionBuilder.AUDIO_NONE)
  //      .setVideoEncoder(SessionBuilder.VIDEO_H264);
  //
  //  // Starts the RTSP server
  //  this.startService(new Intent(this,RtspServer.class));
  //}
}
