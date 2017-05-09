package net.majorkernelpanic.streaming.screen;

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.view.Surface;
import java.io.IOException;
import java.security.InvalidParameterException;
import net.majorkernelpanic.streaming.SessionBuilder;

/**
 * Created by antonio.carrasco on 24/01/2017.
 */

public class MediaCodecUtils {

  private static final String TAG = "MediaCodecUtils";
  static final int VIDEO_WIDTH = 720;
  static final int VIDEO_HEIGHT = 1280;
  private static final int SCREEN_DPI = 320;
  private static final int BIT_RATE = 1800000;
  private static final String VIDEO_MIME_TYPE = "video/avc";
  private static final int FRAME_RATE = 30;

  private static volatile MediaCodecUtils sInstance = null;
  private MediaCodec mediaCodec;
  private VirtualDisplay virtualDisplay;
  private MediaProjection mediaProjection;
  private Surface mediaCodecSurface;

  private MediaCodecUtils() {
  }

  /**
   * Returns a reference to the {@link SessionBuilder}.
   *
   * @return The reference to the {@link SessionBuilder}
   */
  public static MediaCodecUtils getInstance() {
    if (sInstance == null) {
      synchronized (SessionBuilder.class) {
        if (sInstance == null) {
          MediaCodecUtils.sInstance = new MediaCodecUtils();
        }
      }
    }
    return sInstance;
  }

  public boolean mediaObjectsAlreadySet() {
    return mediaCodec != null && virtualDisplay != null && mediaCodecSurface != null;
  }

  /**
   * Creates a MediaCodec object.
   *
   * @return MediaCodec
   */
  public MediaCodec buildMediaCodec() throws IOException {

    if (mediaCodec != null) {
      return mediaCodec;
    }

    MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);

    // Set some required properties. The media codec may fail if these aren't defined.
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
    format.setInteger(MediaFormat.KEY_CAPTURE_RATE, FRAME_RATE);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames
    format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000L / (long) FRAME_RATE);
    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);

    // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
    mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
    mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    return mediaCodec;
  }

  public Surface getMediaCodecSurface() {
    if (mediaCodecSurface != null) {
      return mediaCodecSurface;
    }
    if (mediaCodec == null) {
      throw new IllegalStateException(
          "[MediaCodecUtils] - getMediaCodecSurface(), mediaCodec can't be null");
    }
    mediaCodecSurface = mediaCodec.createInputSurface();
    return mediaCodecSurface;
  }

  /**
   * Create a virtual display using provided mediaProjection instance.
   *
   * @param mediaProjection MediaProjection to create an instance of VirtualDisplay.
   * @param mediaCodecSurface Surface to which the VirtualDisplay will be rendered.
   * @param displayMetrics Display properties.
   * @return VirualDisplay instance.
   */
  public VirtualDisplay buildVirtualDisplay(MediaProjection mediaProjection,
      Surface mediaCodecSurface, DisplayMetrics displayMetrics) {

    if (virtualDisplay != null) {
      return virtualDisplay;
    }

    if (mediaProjection == null || mediaCodecSurface == null || displayMetrics == null) {
      throw new InvalidParameterException(
          "MediaProjection, Surface and DisplayMetrics are mandatory");
    }
    this.mediaProjection = mediaProjection;
    virtualDisplay =
        mediaProjection.createVirtualDisplay("Recording Display", VIDEO_WIDTH, VIDEO_HEIGHT,
            SCREEN_DPI, 0/* flags */, mediaCodecSurface, null /* callback */, null /* handler */);
    return virtualDisplay;
  }

  public void tearDown(boolean clearProjection) {
    if (mediaCodec != null) {
      mediaCodec.stop();
      mediaCodec.release();
      mediaCodec = null;
    }
    if (virtualDisplay != null) {
      virtualDisplay.release();
      virtualDisplay = null;
    }
    if (mediaCodecSurface != null) {
      mediaCodecSurface.release();
      mediaCodecSurface = null;
    }
    if (clearProjection && mediaProjection != null) {
      mediaProjection.stop();
      mediaProjection = null;
    }
  }
}
