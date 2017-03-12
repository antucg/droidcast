package com.antonio.droidcast;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.majorkernelpanic.streaming.screen.MediaCodecUtils;

public class TestActivity extends BaseActivity {

  private static final int REQUEST_CODE = 1000;
  private MediaProjectionManager mProjectionManager;
  private MediaProjection mMediaProjection;
  private MediaCodec mMediaCodec;
  private Surface mySurface;
  private VirtualDisplay virtualDisplay;
  private Rect displaySize;
  @BindView(R.id.surface_view) SurfaceView surfaceView;

  private final Handler mDrainHandler = new Handler(Looper.getMainLooper());
  private Runnable mDrainEncoderRunnable = new Runnable() {
    @Override public void run() {
      drainEncoder();
    }
  };

  public static Intent createIntent(Context context) {
    return new Intent(context, TestActivity.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test);

    ButterKnife.bind(this);

    surfaceView.getHolder().addCallback(new MyCallback());
  }

  public class MyCallback implements SurfaceHolder.Callback {
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
        int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      mProjectionManager =
          (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

      if (mMediaProjection == null) {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      // and here you need to stop it
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
    MediaProjectionCallback mMediaProjectionCallback = new MediaProjectionCallback();
    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
    test();
  }

  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override public void onStop() {
      Log.i(TAG, "MediaProjectionCallback onStop");
    }
  }

  private void test() {

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    MediaCodecUtils mediaCodecUtils = new MediaCodecUtils();

    try {
      mMediaCodec = mediaCodecUtils.buildMediaCodec();
    } catch (IOException e) {
      return;
    }

    Surface mediaCodecSurface = mMediaCodec.createInputSurface();
    mMediaCodec.start();

    VirtualDisplay virtualDisplay =
        mediaCodecUtils.buildVirtualDisplay(mMediaProjection, mediaCodecSurface /*surfaceView.getHolder().getSurface()*/, metrics);
    mySurface = virtualDisplay.getSurface();
    drainEncoder();
    buildNotificationControl();
  }

  private void drainEncoder() {
    mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
    int mIndex;
    MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    ByteBuffer mBuffer;
    while (true) {
      mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500000);
      if (mIndex >= 0) {
        mBuffer = mMediaCodec.getOutputBuffer(mIndex);
        if (mBuffer == null) {
          throw new RuntimeException("couldn't fetch buffer at index " + mIndex);
        }
        mBuffer.position(0);
        mMediaCodec.releaseOutputBuffer(mIndex, false);
        Log.v(TAG, "Buffer available");
      } else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        Log.v(TAG, "MediaFormat changed");
      } else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        Log.v(TAG, "No buffer available...");
        break;
      }
    }
    mDrainHandler.postDelayed(mDrainEncoderRunnable, 10);
  }

  private void buildNotificationControl() {

    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(this)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            //.setSmallIcon(R.drawable.ic_media_route_off_holo_light)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.manage_streaming));
    Intent resultIntent = MediaShareActivity.createIntent(this);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addParentStack(HomeActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(1, mBuilder.build());
  }
}
