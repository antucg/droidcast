package net.majorkernelpanic.streaming.screen;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import net.majorkernelpanic.streaming.mp4.MP4Config;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;
import net.majorkernelpanic.streaming.video.VideoStream;

/**
 * Created by antonio.carrasco on 08/01/2017.
 */

public class ScreenStream extends VideoStream {

  public final static String TAG = "ScreenStream";

  private Context context;
  private MP4Config mConfigPortrait;
  private MP4Config mConfigLandscape;
  private MediaProjection mediaProjection;
  private DisplayMetrics displayMetrics;
  private VirtualDisplay virtualDisplay;
  private Surface mediaCodecSurface;
  private OrientantionListener.OnOrientationChange onOrientationChange;

  public ScreenStream(Context context, MediaProjection mediaProjection,
      DisplayMetrics displayMetrics) {
    mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    mPacketizer = new H264Packetizer();
    this.context = context;
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
    testMediaCodecAPI();
  }

  private void testMediaCodecAPI() {
    H264Parameters h264Parameters = new H264Parameters(mediaProjection, displayMetrics, mSettings,
        Configuration.ORIENTATION_PORTRAIT);
    mConfigPortrait = h264Parameters.getConfig();
    h264Parameters = new H264Parameters(mediaProjection, displayMetrics, mSettings,
        Configuration.ORIENTATION_LANDSCAPE);
    mConfigLandscape = h264Parameters.getConfig();
  }

  /**
   * Returns a description of the stream using SDP. It can then be included in an SDP file.
   */
  public synchronized String getSessionDescription() throws IllegalStateException {
    if (mConfigPortrait == null) {
      throw new IllegalStateException("You need to call configure() first !");
    }
    return "m=video "
        + String.valueOf(getDestinationPorts()[0])
        + " RTP/AVP 96\r\n"
        + "a=rtpmap:96 H264/90000\r\n"
        + "a=fmtp:96 packetization-mode=1;profile-level-id="
        + mConfigPortrait.getProfileLevel()
        + ";sprop-parameter-sets="
        + mConfigPortrait.getB64SPS()
        + ","
        + mConfigPortrait.getB64PPS()
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
      setStreamParameters();
      super.start();
    }
  }

  private void setStreamParameters() {
    byte[] pps;
    byte[] sps;
    if (context.getResources().getConfiguration().orientation
        == Configuration.ORIENTATION_PORTRAIT) {
      pps = Base64.decode(mConfigPortrait.getB64PPS(), Base64.NO_WRAP);
      sps = Base64.decode(mConfigPortrait.getB64SPS(), Base64.NO_WRAP);
    } else {
      pps = Base64.decode(mConfigLandscape.getB64PPS(), Base64.NO_WRAP);
      sps = Base64.decode(mConfigLandscape.getB64SPS(), Base64.NO_WRAP);
    }
    ((H264Packetizer) mPacketizer).setStreamParameters(pps, sps);
  }

  @Override protected void encodeWithMediaCodec() {
    int orientation = context.getResources().getConfiguration().orientation;
    if (mMediaCodec == null) {
      try {
        mMediaCodec = MediaCodecUtils.buildMediaCodec(orientation);
      } catch (IOException e) {
        releaseEncoders();
        Log.e(TAG, "[ScreenStream] - encodeWithMediaCodec(), error creating MediaCodec");
        return;
      }
    }

    if (mediaCodecSurface == null) {
      mediaCodecSurface = mMediaCodec.createInputSurface();
      mMediaCodec.start();
    }

    if (virtualDisplay == null) {
      virtualDisplay =
          MediaCodecUtils.buildVirtualDisplay(mediaProjection, mediaCodecSurface, displayMetrics,
              orientation);
    }

    // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
    mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
    mPacketizer.start();
    mStreaming = true;
    startScreenOrientationListener();
  }

  private void startScreenOrientationListener() {
    onOrientationChange = new OrientantionListener.OnOrientationChange() {
      @Override public void newOrientation(int orientation) {
        mPacketizer.stop();
        setStreamParameters();
        mMediaCodec.stop();
        mMediaCodec.release();
        try {
          mMediaCodec = MediaCodecUtils.buildMediaCodec(orientation);
        } catch (IOException e) {
          releaseEncoders();
          Log.e(TAG, "[ScreenStream] - OrientantionListener(), error creating MediaCodec");
          return;
        }
        mediaCodecSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
        int width =
            orientation == Configuration.ORIENTATION_PORTRAIT ? MediaCodecUtils.VIDEO_WIDTH
                : MediaCodecUtils.VIDEO_HEIGHT;
        int height =
            orientation == Configuration.ORIENTATION_PORTRAIT ? MediaCodecUtils.VIDEO_HEIGHT
                : MediaCodecUtils.VIDEO_WIDTH;
        int density = MediaCodecUtils.SCREEN_DPI;
        virtualDisplay.resize(width, height, density);
        virtualDisplay.setSurface(mediaCodecSurface);

        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
        mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
        mPacketizer.start();
      }
    };

    OrientantionListener.getInstance(context).addListener(onOrientationChange);
  }

  private void releaseEncoders() {
    if (mMediaCodec != null) {
      mMediaCodec.stop();
      mMediaCodec.release();
      mMediaCodec = null;
    }
    if (virtualDisplay != null) {
      virtualDisplay.release();
      virtualDisplay = null;
    }
    if (mediaCodecSurface != null) {
      mediaCodecSurface.release();
      mediaCodecSurface = null;
    }
  }

  @Override public synchronized void stop() {
    if (mStreaming) {
      mPacketizer.stop();
      releaseEncoders();
    }
    OrientantionListener.getInstance(context).removeListener(onOrientationChange);
    mStreaming = false;
  }
}
