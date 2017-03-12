package net.majorkernelpanic.streaming.screen;

import android.graphics.SurfaceTexture;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.opengl.GLES20;
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

public class ScreenStream extends VideoStream implements SurfaceTexture.OnFrameAvailableListener{

  public final static String TAG = "ScreenStream";

  private MP4Config mConfig;
  private MediaProjection mediaProjection;
  private Surface mediaCodecSurface;
  private VirtualDisplay virtualDisplay;
  private DisplayMetrics displayMetrics;

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

  private MP4Config testMediaCodecAPI() {
    H264Parameters h264Parameters = new H264Parameters(mediaProjection, displayMetrics, mSettings);
    return h264Parameters.getConfig();
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

  @Override protected void encodeWithMediaCodec() {

    MediaCodecUtils mediaCodecUtils = new MediaCodecUtils();
    try {
      mMediaCodec = mediaCodecUtils.buildMediaCodec();
    } catch (IOException e) {
      releaseEncoders();
      return;
    }

    mediaCodecSurface = mMediaCodec.createInputSurface();
    mMediaCodec.start();
    virtualDisplay =
        mediaCodecUtils.buildVirtualDisplay(mediaProjection, mediaCodecSurface, displayMetrics);

    // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
    mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
    mPacketizer.start();

    mStreaming = true;
  }

  private void releaseEncoders() {
    Log.d(TAG, "[ScreenStream] - releaseEncoders()");
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
    if (virtualDisplay != null) {
      virtualDisplay.release();
    }
  }

  @Override public synchronized void stop() {
    if (mStreaming) {
      releaseEncoders();
    }
    mStreaming = false;
  }

  @Override public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    Log.d(TAG, "onFrameAvailable");
  }
}
