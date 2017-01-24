package com.antonio.droidcast;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TestActivity extends AppCompatActivity {

  private MediaProjectionManager mMediaProjectionManager;
  private static final int REQUEST_CODE_CAPTURE_PERM = 1234;
  private static final String VIDEO_MIME_TYPE = "video/avc";
  private static final int VIDEO_WIDTH = 1280;
  private static final int VIDEO_HEIGHT = 720;
  private boolean mMuxerStarted = false;
  private MediaProjection mMediaProjection;
  private Surface mInputSurface;
  private MediaMuxer mMuxer;
  private MediaCodec mVideoEncoder;
  private MediaCodec.BufferInfo mVideoBufferInfo;
  private int mTrackIndex = -1;
  private final Handler mDrainHandler = new Handler(Looper.getMainLooper());
  private Runnable mDrainEncoderRunnable = new Runnable() {
    @Override
    public void run() {
      drainEncoder();
    }
  };

  public static Intent createIntent(Context context) {
    return new Intent(context, TestActivity.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test);

    mMediaProjectionManager = (MediaProjectionManager) getSystemService(
        android.content.Context.MEDIA_PROJECTION_SERVICE);
    Intent permissionIntent = mMediaProjectionManager.createScreenCaptureIntent();
    startActivityForResult(permissionIntent, REQUEST_CODE_CAPTURE_PERM);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (REQUEST_CODE_CAPTURE_PERM == requestCode) {
      if (resultCode == RESULT_OK) {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
        startRecording(); // defined below
      } else {
        // user did not grant permissions
      }
    }
  }

  private void startRecording() {
    DisplayManager dm = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
    Display defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY);
    if (defaultDisplay == null) {
      throw new RuntimeException("No display found.");
    }
    prepareVideoEncoder();

    try {
      mMuxer = new MediaMuxer("/sdcard/video.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    } catch (IOException ioe) {
      throw new RuntimeException("MediaMuxer creation failed", ioe);
    }

    // Get the display size and density.
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    int screenWidth = metrics.widthPixels;
    int screenHeight = metrics.heightPixels;
    int screenDensity = metrics.densityDpi;

    // Start the video input.
    mMediaProjection.createVirtualDisplay("Recording Display", screenWidth,
        screenHeight, screenDensity, 0 /* flags */, mInputSurface,
        null /* callback */, null /* handler */);

    // Start the encoders
    drainEncoder();
  }

  private void prepareVideoEncoder() {
    mVideoBufferInfo = new MediaCodec.BufferInfo();
    MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
    int frameRate = 30; // 30 fps

    // Set some required properties. The media codec may fail if these aren't defined.
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); // 6Mbps
    format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
    format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
    format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
    format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

    // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
    try {
      mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
      mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      mInputSurface = mVideoEncoder.createInputSurface();
      mVideoEncoder.start();
    } catch (IOException e) {
      releaseEncoders();
    }
  }

  private boolean drainEncoder() {
    mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
    while (true) {
      int bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);

      if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        System.out.println("### foo 0");
        // nothing available yet
        break;
      } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        System.out.println("### foo 1");
        // should happen before receiving buffers, and should only happen once
        if (mTrackIndex >= 0) {
          throw new RuntimeException("format changed twice");
        }
        mTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());
        if (!mMuxerStarted && mTrackIndex >= 0) {
          mMuxer.start();
          mMuxerStarted = true;
        }
      } else if (bufferIndex < 0) {
        System.out.println("### foo 2");
        // not sure what's going on, ignore it
      } else {
        System.out.println("### foo 3");
        ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
        if (encodedData == null) {
          throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
        }

        if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          mVideoBufferInfo.size = 0;
        }

        if (mVideoBufferInfo.size != 0) {
          if (mMuxerStarted) {
            encodedData.position(mVideoBufferInfo.offset);
            encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
            mMuxer.writeSampleData(mTrackIndex, encodedData, mVideoBufferInfo);
          } else {
            // muxer not started
          }
        }

        mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

        if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break;
        }
      }
    }

    mDrainHandler.postDelayed(mDrainEncoderRunnable, 10);
    return false;
  }

  @Override protected void onDestroy() {
    releaseEncoders();
    super.onDestroy();
  }

  private void releaseEncoders() {
    mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
    if (mMuxer != null) {
      if (mMuxerStarted) {
        mMuxer.stop();
      }
      mMuxer.release();
      mMuxer = null;
      mMuxerStarted = false;
    }
    if (mVideoEncoder != null) {
      mVideoEncoder.stop();
      mVideoEncoder.release();
      mVideoEncoder = null;
    }
    if (mInputSurface != null) {
      mInputSurface.release();
      mInputSurface = null;
    }
    if (mMediaProjection != null) {
      mMediaProjection.stop();
      mMediaProjection = null;
    }
    mVideoBufferInfo = null;
    mDrainEncoderRunnable = null;
    mTrackIndex = -1;
  }
}
