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
import android.net.LocalServerSocket;
import android.net.LocalSocket;
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
import com.github.hiteshsondhi88.libffmpeg.FFServer;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;

public class MediaShareActivity extends BaseActivity {

  @Inject FFmpeg ffmpeg;
  @Inject FFServer ffServer;

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
    //askForPermissionAndShare();
    startStreaming();
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

      LocalServerSocket localServerSocket = new LocalServerSocket("Server socket");
      LocalSocket receiver = new LocalSocket();
      receiver.connect(localServerSocket.getLocalSocketAddress());
      receiver.setReceiveBufferSize(BUFFER_SIZE);

      LocalSocket sender = localServerSocket.accept();
      sender.setSendBufferSize(BUFFER_SIZE);

      mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      mMediaRecorder.setOutputFile(sender.getFileDescriptor());
      mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
      mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
      mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
      mMediaRecorder.setVideoFrameRate(30);
      int rotation = getWindowManager().getDefaultDisplay().getRotation();
      int orientation = ORIENTATIONS.get(rotation + 90);
      mMediaRecorder.setOrientationHint(orientation);
      mMediaRecorder.prepare();
      mMediaRecorder.start();

      InputStream is = receiver.getInputStream();
      byte buffer[] = new byte[4];
      // Skip all atoms preceding mdat atom
      while (!Thread.interrupted()) {
        while (is.read() != 'm');
        is.read(buffer,0,3);
        if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
      }

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

      ffServer.execute(new String[] { "-d", "-f", ffserverConfPath },
          new FFmpegExecuteResponseHandler() {
            @Override public void onSuccess(String message) {
              System.out.println("startStreaming - success " + message);
            }

            @Override public void onProgress(String message) {
              System.out.println("### progress " + message);
            }

            @Override public void onFailure(String message) {
              System.out.println("startStreaming - failure " + message);
              finish();
            }

            @Override public void onStart() {
              System.out.println("startStreaming - start");
              startFFMpeg();
              //convert();
            }

            @Override public void onFinish() {
              System.out.println("startStreaming - finish");
            }
          });
    } catch (FFmpegCommandAlreadyRunningException e) {
      e.printStackTrace();
    }
  }

  private void convert() {
    try {
      ffmpeg.execute(new String[] {
          "-i", VIDEO_PATH, "-movflags", "faststart", VIDEO_PATH2
      }, new FFmpegExecuteResponseHandler() {
        @Override public void onSuccess(String message) {
          System.out.println("startFFMpeg - success " + message);
        }

        @Override public void onProgress(String message) {

        }

        @Override public void onFailure(String message) {
          System.out.println("startFFMpeg - failure " + message);
        }

        @Override public void onStart() {

        }

        @Override public void onFinish() {
          System.out.println("#### finish");
          startFFMpeg();
        }
      });
    } catch (FFmpegCommandAlreadyRunningException e) {
      e.printStackTrace();
    }
  }

  private void startFFMpeg() {
    try {
      ffmpeg.execute(new String[] {
          "-re", "-loglevel", "debug", "-framerate", "30", "-i", VIDEO_PATH2,
          "udp://localhost:8090/feed1.ffm"
      }, new FFmpegExecuteResponseHandler() {
        @Override public void onSuccess(String message) {
          System.out.println("startFFMpeg - success " + message);
        }

        @Override public void onProgress(String message) {

        }

        @Override public void onFailure(String message) {
          System.out.println("startFFMpeg - failure " + message);
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
    if (ffmpeg.isFFmpegCommandRunning()) {
      ffmpeg.killRunningProcesses();
    }
    if (ffServer.isFFmpegCommandRunning()) {
      ffServer.killRunningProcesses();
    }
  }
}
