package com.antonio.droidcast;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import com.antonio.droidcast.ioc.IOCProvider;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.video.VideoQuality;

public class MediaShareActivity extends BaseActivity implements Session.Callback {

  private static final int REQUEST_CODE = 1000;
  private int mScreenDensity;
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
  private final String VIDEO_PATH2 =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          + "/video1.mp4";

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
    System.out.println(VIDEO_PATH2);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_share);
    System.out.println(getCacheDir());

    IOCProvider.getInstance().inject(this);

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    mScreenDensity = metrics.densityDpi;

    mMediaRecorder = new MediaRecorder();

    mProjectionManager =
        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    askForPermissionAndShare();
  }

  private void askForPermissionAndShare() {
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
      startScreenShare();
    }
  }

  public void startScreenShare() {
    initRecorder();
  }

  private void recordScreen() {
    if (mMediaProjection == null) {
      startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
      return;
    }
    mVirtualDisplay = createVirtualDisplay();
    mMediaRecorder.start();
    startStreaming();
  }

  private void initRecorder() {
    //try {
    //
    //  LocalServerSocket localServerSocket = new LocalServerSocket("Server socket");
    //  LocalSocket receiver = new LocalSocket();
    //  receiver.connect(localServerSocket.getLocalSocketAddress());
    //  receiver.setReceiveBufferSize(BUFFER_SIZE);
    //
    //  LocalSocket sender = localServerSocket.accept();
    //  sender.setSendBufferSize(BUFFER_SIZE);
    //
    //  mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    //  mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    //  mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    //  mMediaRecorder.setOutputFile(sender.getFileDescriptor());
    //  mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
    //  mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    //  mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    //  mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
    //  mMediaRecorder.setVideoFrameRate(30);
    //  int rotation = getWindowManager().getDefaultDisplay().getRotation();
    //  int orientation = ORIENTATIONS.get(rotation + 90);
    //  mMediaRecorder.setOrientationHint(orientation);
    //  mMediaRecorder.prepare();
    //  mMediaRecorder.start();
    //
    //  InputStream is = receiver.getInputStream();
    //  byte buffer[] = new byte[4];
    //  // Skip all atoms preceding mdat atom
    //  while (!Thread.interrupted()) {
    //    while (is.read() != 'm');
    //    is.read(buffer,0,3);
    //    if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
    //  }
    //
    //} catch (IOException e) {
    //  e.printStackTrace();
    //}

    mSession = SessionBuilder.getInstance()
        .setCallback(this)
        //.setSurfaceView(mSurfaceView)
        .setDestination("192.168.1.10")
        .setPreviewOrientation(90)
        .setContext(getApplicationContext())
        .setAudioEncoder(SessionBuilder.AUDIO_NONE)
        .setAudioQuality(new AudioQuality(16000, 32000))
        .setVideoEncoder(SessionBuilder.VIDEO_H264)
        .setVideoQuality(new VideoQuality(320, 240, 20, 500000))
        .build();
    if (!mSession.isStreaming()) {
      mSession.configure();
    } else {
      mSession.stop();
    }
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
    mVirtualDisplay = createVirtualDisplay();
    //mMediaRecorder.start();
    startStreaming();
  }

  @Override public void onBitrateUpdate(long bitrate) {
    Log.d(TAG, "Bitrate: " + bitrate);
  }

  @Override public void onSessionError(int message, int streamType, Exception e) {
    if (e != null) {
      logError(e.getMessage());
    }
  }

  @Override public void onPreviewStarted() {

  }

  @Override public void onSessionConfigured() {
    recordScreen();
    Log.d(TAG, "Preview configured.");
    // Once the stream is configured, you can get a SDP formated session description
    // that you can send to the receiver of the stream.
    // For example, to receive the stream in VLC, store the session description in a .sdp file
    // and open it with VLC while streming.
    Log.d(TAG, mSession.getSessionDescription());
    mSession.start();
  }

  @Override public void onSessionStarted() {

  }

  @Override public void onSessionStopped() {

  }

  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override public void onStop() {
      stopScreenShare();
    }
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

  private VirtualDisplay createVirtualDisplay() {
    return mMediaProjection.createVirtualDisplay("MediaShareActivity", DISPLAY_WIDTH,
        DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
  }

  public void stopScreenShare() {
    Log.v(TAG, "Stopping Recording");
    stopStreaming();
    //mMediaRecorder.stop();
    //mMediaRecorder.reset();

    if (mVirtualDisplay == null) {
      return;
    }
    mVirtualDisplay.release();
    //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
    // be reused again
    destroyMediaProjection();
  }

  private void destroyMediaProjection() {
    if (mMediaProjection != null) {
      mMediaProjection.unregisterCallback(mMediaProjectionCallback);
      mMediaProjection.stop();
      mMediaProjection = null;
    }
    Log.i(TAG, "MediaProjection Stopped");
  }

  @Override public void onDestroy() {
    stopScreenShare();
    super.onDestroy();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_PERMISSIONS: {
        if ((grantResults.length > 0)
            && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
          startScreenShare();
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

  private void startStreaming() {
    //if (!mSession.isStreaming()) {
    //  mSession.configure();
    //} else {
    //  mSession.stop();
    //}
  }

  private void stopStreaming() {
    if (mSession.isStreaming()) {
      mSession.stop();
    }
  }
}
