package net.majorkernelpanic.streaming.screen;

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.majorkernelpanic.streaming.hw.EncoderDebugger;
import net.majorkernelpanic.streaming.mp4.MP4Config;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;
import net.majorkernelpanic.streaming.video.VideoStream;

/**
 * Created by antonio.carrasco on 08/01/2017.
 */

public class ScreenStream extends VideoStream {

  public final static String TAG = "ScreenStream";

  private static final int VIDEO_WIDTH = 1280;
  private static final int VIDEO_HEIGHT = 720;
  private static final String VIDEO_MIME_TYPE = "video/avc";

  private MP4Config mConfig;
  private MediaProjection mediaProjection;
  private Surface mediaCodecSurface;
  private DisplayMetrics displayMetrics;
  private MediaCodec.BufferInfo mVideoBufferInfo;

  private boolean mMuxerStarted = false;
  private MediaMuxer mMuxer;
  private int mTrackIndex = -1;
  private final Handler mDrainHandler = new Handler(Looper.getMainLooper());
  private Runnable mDrainEncoderRunnable = new Runnable() {
    @Override
    public void run() {
      drainEncoder();
    }
  };

  public ScreenStream(MediaProjection mediaProjection, DisplayMetrics displayMetrics) {
    mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    mPacketizer = new H264Packetizer();
    this.mediaProjection = mediaProjection;
    this.displayMetrics = displayMetrics;
  }

  /**
   * Configures the stream. You need to call this before calling {@link #getSessionDescription()}
   * to apply your configuration of the stream.
   */
  public synchronized void configure() throws IllegalStateException, IOException {
    super.configure();
    mMode = MODE_MEDIACODEC_API;
    mConfig = testMediaCodecAPI();
  }

  // Should not be called by the UI thread
  private MP4Config testMediaCodecAPI() throws RuntimeException, IOException {
    try {
      //EncoderDebugger debugger = EncoderDebugger.debug(mSettings, VIDEO_WIDTH, VIDEO_HEIGHT);
      //return new MP4Config(debugger.getB64SPS(), debugger.getB64PPS());
      return new MP4Config("/sdcard/video.mp4");
    } catch (Exception e) {
      // Fallback on the old streaming method using the MediaRecorder API
      Log.e(TAG, "Resolution not supported with the MediaCodec API.");
    }
    return null;
  }

  /**
   * Returns a description of the stream using SDP. It can then be included in an SDP file.
   */
  public synchronized String getSessionDescription() throws IllegalStateException {
    if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
    return "m=video "
        + String.valueOf(getDestinationPorts()[0])
        + " RTP/AVP 96\r\n"
        + "a=rtpmap:96 H264/90000\r\n"
        + "a=fmtp:96 packetization-mode=1;profile-level-id="
        + mConfig.getProfileLevel()
        + ";sprop-parameter-sets="
        + mConfig.getB64SPS()
        + ","
        + mConfig.getB64PPS()
        + ";\r\n";
  }

  /**
   * Starts the stream.
   * This will also open the camera and display the preview if {@link #startPreview()} has not
   * already been called.
   */
  public synchronized void start() throws IllegalStateException, IOException {
    if (!mStreaming) {
      configure();
      byte[] pps = Base64.decode(mConfig.getB64PPS(), Base64.NO_WRAP);
      byte[] sps = Base64.decode(mConfig.getB64SPS(), Base64.NO_WRAP);
      ((H264Packetizer) mPacketizer).setStreamParameters(pps, sps);
      super.start();
    }
  }

  @Override protected void encodeWithMediaCodec() throws IOException {
    Log.i(TAG, "encodeWithMediaCodec called");

    prepareVideoEncoder();

    try {
      mMuxer = new MediaMuxer("/sdcard/video.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    } catch (IOException ioe) {
      throw new RuntimeException("MediaMuxer creation failed", ioe);
    }

    // Start the video input.
    mediaProjection.createVirtualDisplay("Recording Display", VIDEO_WIDTH,
        VIDEO_HEIGHT, displayMetrics.densityDpi, 0 /* flags */, mediaCodecSurface,
        null /* callback */, null /* handler */);
    //mediaProjection.createVirtualDisplay("Recording Display", displayMetrics.widthPixels,
    //    displayMetrics.heightPixels, displayMetrics.densityDpi, 0 /* flags */, mediaCodecSurface,
    //    null /* callback */, null /* handler */);

    //drainEncoder();

    // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
    mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
    mPacketizer.start();

    mStreaming = true;
  }

  private void prepareVideoEncoder() {

    mVideoBufferInfo = new MediaCodec.BufferInfo();
    MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
    int frameRate = 30; // 30 fps

    // Set some required properties. The media codec may fail if these aren't defined.
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);//6000000); // 6Mbps
    format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
    //format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
    //format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
    //format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

    // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
    try {
      mMediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
      mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      mediaCodecSurface = mMediaCodec.createInputSurface();
      mMediaCodec.start();
    } catch (IOException e) {
      releaseEncoders();
    }
  }

  private boolean drainEncoder() {
    mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
    while (true) {
      int bufferIndex = mMediaCodec.dequeueOutputBuffer(mVideoBufferInfo, 0);

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
        mTrackIndex = mMuxer.addTrack(mMediaCodec.getOutputFormat());
        if (!mMuxerStarted && mTrackIndex >= 0) {
          mMuxer.start();
          mMuxerStarted = true;
        }
      } else if (bufferIndex < 0) {
        System.out.println("### foo 2");
        // not sure what's going on, ignore it
      } else {
        System.out.println("### foo 3");
        ByteBuffer encodedData = mMediaCodec.getOutputBuffer(bufferIndex);
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

        mMediaCodec.releaseOutputBuffer(bufferIndex, false);

        if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break;
        }
      }
    }

    mDrainHandler.postDelayed(mDrainEncoderRunnable, 10);
    return false;
  }

  private void releaseEncoders() {
    if (mMuxer != null) {
      if (mMuxerStarted) {
        mMuxer.stop();
      }
      mMuxer.release();
      mMuxer = null;
      mMuxerStarted = false;
    }
    if (mMediaCodec != null) {
      mMediaCodec.stop();
      mMediaCodec.release();
      mMediaCodec = null;
    }
    if (mediaCodecSurface != null) {
      mediaCodecSurface.release();
      mediaCodecSurface = null;
    }
    if (mediaProjection != null) {
      mediaProjection.stop();
      mediaProjection = null;
    }
    mVideoBufferInfo = null;
    mDrainEncoderRunnable = null;
    mTrackIndex = -1;
  }

  @Override public synchronized void stop() {
    if (mStreaming) {
      releaseEncoders();
    }
    mStreaming = false;
  }
}
