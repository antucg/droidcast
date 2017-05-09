package net.majorkernelpanic.streaming.screen;

import android.content.SharedPreferences;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.majorkernelpanic.streaming.exceptions.ConfNotSupportedException;
import net.majorkernelpanic.streaming.mp4.MP4Config;

/**
 * Created by antonio.carrasco on 28/01/2017.
 */

class H264Parameters {

  private final String TAG = this.getClass().getSimpleName();

  /**
   * Prefix that will be used for all shared preferences saved by libstreaming.
   */
  private static final String PREF_PREFIX = "screenstreaming-";

  /**
   * Will be incremented every time this test is modified.
   */
  private static final int VERSION = 1;

  private MediaProjection mediaProjection;
  private DisplayMetrics displayMetrics;
  private SharedPreferences sharedPreferences;
  private MediaCodec mediaCodec;
  private Surface mediaCodecSurface;
  private VirtualDisplay virtualDisplay;
  private byte[] SPS = null;
  private byte[] PPS = null;
  private String b64PPS;
  private String b64SPS;
  private int videoWidth;
  private int videoHeight;

  H264Parameters(MediaProjection mediaProjection, DisplayMetrics displayMetrics,
      SharedPreferences sharedPreferences) {
    this.mediaProjection = mediaProjection;
    this.displayMetrics = displayMetrics;
    this.sharedPreferences = sharedPreferences;
    videoWidth = MediaCodecUtils.VIDEO_WIDTH;
    videoHeight = MediaCodecUtils.VIDEO_HEIGHT;
  }

  MP4Config getConfig() throws ConfNotSupportedException {

    if (!checkTestNeeded()) {
      String resolution = videoWidth + "x" + videoHeight + "-";
      if (!sharedPreferences.getBoolean(PREF_PREFIX + resolution + "successful", false)) {
        throw new ConfNotSupportedException("This device can't create specified codec");
      }

      b64PPS = sharedPreferences.getString(PREF_PREFIX + resolution + "pps", "");
      b64SPS = sharedPreferences.getString(PREF_PREFIX + resolution + "sps", "");
      return new MP4Config(b64SPS, b64PPS);
    }

    MediaCodecUtils mediaCodecUtils = MediaCodecUtils.getInstance();

    try {
      mediaCodec = mediaCodecUtils.buildMediaCodec();
    } catch (IOException e) {
      releaseEncoders();
      saveTestResult(false);
      return null;
    }

    mediaCodecSurface = mediaCodecUtils.getMediaCodecSurface();
    mediaCodec.start();
    virtualDisplay =
        mediaCodecUtils.buildVirtualDisplay(mediaProjection, mediaCodecSurface, displayMetrics);
    drainEncoder();
    return new MP4Config(b64SPS, b64PPS);
  }

  private void drainEncoder() {

    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    byte[] csd = new byte[128];
    int len = 0;
    int p = 4;
    int q = 4;
    long elapsed = 0;
    long now = timestamp();
    while (elapsed < 3000000 && (SPS == null || PPS == null)) {

      int bufferIndex = mediaCodec.dequeueOutputBuffer(info, 0);
      if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        // The PPS and PPS shoud be there
        MediaFormat format = mediaCodec.getOutputFormat();
        ByteBuffer spsb = format.getByteBuffer("csd-0");
        ByteBuffer ppsb = format.getByteBuffer("csd-1");
        SPS = new byte[spsb.capacity() - 4];
        spsb.position(4);
        spsb.get(SPS, 0, SPS.length);
        PPS = new byte[ppsb.capacity() - 4];
        ppsb.position(4);
        ppsb.get(PPS, 0, PPS.length);
        break;
      } else if (bufferIndex >= 0) {
        len = info.size;
        if (len < 128) {
          ByteBuffer encodedData = mediaCodec.getOutputBuffer(bufferIndex);
          if (encodedData == null) {
            throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
          }
          encodedData.get(csd, 0, len);
          if (len > 0 && csd[0] == 0 && csd[1] == 0 && csd[2] == 0 && csd[3] == 1) {
            // Parses the SPS and PPS, they could be in two different packets and in a different order
            //depending on the phone so we don't make any assumption about that
            while (p < len) {
              while (!(csd[p] == 0 && csd[p + 1] == 0 && csd[p + 2] == 0 && csd[p + 3] == 1)
                  && p + 3 < len) {
                p++;
              }
              if (p + 3 >= len) {
                p = len;
              }
              if ((csd[q] & 0x1F) == 7) {
                SPS = new byte[p - q];
                System.arraycopy(csd, q, SPS, 0, p - q);
              } else {
                PPS = new byte[p - q];
                System.arraycopy(csd, q, PPS, 0, p - q);
              }
              p += 4;
              q = p;
            }
          }
        }
        mediaCodec.releaseOutputBuffer(bufferIndex, false);
      }

      elapsed = timestamp() - now;
    }

    if (PPS == null || SPS == null) {
      Log.e(TAG, "H264Parameters - darinEncoder(), couldn't determine PPS or SPS");
      throw new IllegalStateException("Couldn't determine PPS or SPS");
    }

    b64PPS = Base64.encodeToString(PPS, 0, PPS.length, Base64.NO_WRAP);
    b64SPS = Base64.encodeToString(SPS, 0, SPS.length, Base64.NO_WRAP);
    releaseEncoders();
    saveTestResult(true);
  }

  private void releaseEncoders() {
    MediaCodecUtils.getInstance().tearDown(false);
    //if (mediaCodec != null) {
    //  mediaCodec.stop();
    //  mediaCodec.release();
    //  mediaCodec = null;
    //}
    //if (virtualDisplay != null) {
    //  virtualDisplay.release();
    //  virtualDisplay = null;
    //}
    //if (mediaCodecSurface != null) {
    //  mediaCodecSurface.release();
    //  mediaCodecSurface = null;
    //}
  }

  private boolean  checkTestNeeded() {
    // Forces the test
    if (sharedPreferences == null) {
      return true;
    }

    String resolution = videoWidth + "x" + videoHeight + "-";

    // If the sdk has changed on the phone, or the version of the test
    // it has to be run again
    if (sharedPreferences.contains(PREF_PREFIX + resolution + "lastSdk")) {
      int lastSdk = sharedPreferences.getInt(PREF_PREFIX + resolution + "lastSdk", 0);
      int lastVersion = sharedPreferences.getInt(PREF_PREFIX + resolution + "lastVersion", 0);
      if (Build.VERSION.SDK_INT > lastSdk || VERSION > lastVersion) {
        return true;
      }
    } else {
      return true;
    }
    return false;
  }

  private void saveTestResult(boolean successful) {
    String resolution = videoWidth + "x" + videoHeight + "-";
    SharedPreferences.Editor editor = sharedPreferences.edit();

    if (!successful) {
      editor.putBoolean(PREF_PREFIX + resolution + "successful", false);
    } else {
      editor.putBoolean(PREF_PREFIX + resolution + "successful", true);
      editor.putInt(PREF_PREFIX + resolution + "lastSdk", Build.VERSION.SDK_INT);
      editor.putInt(PREF_PREFIX + resolution + "lastVersion", VERSION);
      editor.putString(PREF_PREFIX + resolution + "pps", b64PPS);
      editor.putString(PREF_PREFIX + resolution + "sps", b64SPS);
    }
    editor.commit();
  }

  private long timestamp() {
    return System.nanoTime() / 1000;
  }
}
