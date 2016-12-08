package com.antonio.droidcast;

import android.Manifest;
import android.content.Context;
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
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;

public class MediaShareActivity extends BaseActivity {

  @Inject FFmpeg fFmpeg;

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

    if (mMediaProjection == null) {
      startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
      return;
    }
    mVirtualDisplay = createVirtualDisplay();
    mMediaRecorder.start();
    startStreaming();
  }

  private void initRecorder() {
    try {
      mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mMediaRecorder.setOutputFile(VIDEO_PATH);
      mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
      mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
      mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
      mMediaRecorder.setVideoFrameRate(30);
      int rotation = getWindowManager().getDefaultDisplay().getRotation();
      int orientation = ORIENTATIONS.get(rotation + 90);
      mMediaRecorder.setOrientationHint(orientation);
      mMediaRecorder.prepare();
    } catch (IOException e) {
      e.printStackTrace();
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
    mMediaRecorder.start();
    startStreaming();
  }

  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override public void onStop() {
      stopScreenShare();
    }
  }

  private VirtualDisplay createVirtualDisplay() {
    return mMediaProjection.createVirtualDisplay("MediaSahreActivity", DISPLAY_WIDTH,
        DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
  }

  public void stopScreenShare() {
    Log.v(TAG, "Stopping Recording");
    stopStreaming();
    mMediaRecorder.stop();
    mMediaRecorder.reset();

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
    try {

      String ffserverConfPath = getFilesDir().getAbsolutePath() + File.separator + "ffserver.conf";

      fFmpeg.execute(new String[] { "-f", ffserverConfPath }, new FFmpegExecuteResponseHandler() {
        @Override public void onSuccess(String message) {
          System.out.println("Success " + message);
        }

        @Override public void onProgress(String message) {

        }

        @Override public void onFailure(String message) {
          System.out.println("Failure " + message);
        }

        @Override public void onStart() {

        }

        @Override public void onFinish() {

        }
      });
    } catch (FFmpegCommandAlreadyRunningException e) {
      e.printStackTrace();
    }
  }

  private void stopStreaming() {
    fFmpeg.killRunningProcesses();
  }
}
