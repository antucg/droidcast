package net.majorkernelpanic.streaming.screen;

import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import net.majorkernelpanic.streaming.exceptions.ConfNotSupportedException;
import net.majorkernelpanic.streaming.exceptions.StorageUnavailableException;
import net.majorkernelpanic.streaming.mp4.MP4Config;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.video.VideoStream;

/**
 * Created by antonio.carrasco on 08/01/2017.
 */

public class ScreenStream extends VideoStream {

  public final static String TAG = "ScreenStream";

  private MP4Config mConfig;
  private Semaphore mLock = new Semaphore(0);
  private MediaProjection mediaProjection;
  private int screenDensity;

  public ScreenStream() {
    mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    mPacketizer = new H264Packetizer();
  }

  public void setMediaProjection(MediaProjection mediaProjection, int screenDensity) {
    this.mediaProjection = mediaProjection;
    this.screenDensity = screenDensity;
  }

  /**
   * Returns a description of the stream using SDP. It can then be included in an SDP file.
   */
  public synchronized String getSessionDescription() throws IllegalStateException {
    if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
    return "m=video "
        + String.valueOf(getDestinationPorts()[0])
        + " RTP/AVP 96\r\n"
        +
        "a=rtpmap:96 H264/90000\r\n"
        +
        "a=fmtp:96 packetization-mode=1;profile-level-id="
        + mConfig.getProfileLevel()
        + ";sprop-parameter-sets="
        + mConfig.getB64SPS()
        + ","
        + mConfig.getB64PPS()
        + ";\r\n";
  }

  /**
   * Configures the stream. You need to call this before calling {@link #getSessionDescription()}
   * to
   * apply
   * your configuration of the stream.
   */
  public synchronized void configure() throws IllegalStateException, IOException {
    super.configure();
    mMode = MODE_MEDIARECORDER_API;
    mQuality = mRequestedQuality.clone();
    mConfig = testMediaRecorderAPI();
  }

  // Should not be called by the UI thread
  private MP4Config testMediaRecorderAPI() throws RuntimeException, IOException {
    String key = PREF_PREFIX
        + "h264-mr-"
        + mRequestedQuality.framerate
        + ","
        + mRequestedQuality.resX
        + ","
        + mRequestedQuality.resY;

    if (mSettings != null) {
      if (mSettings.contains(key)) {
        String[] s = mSettings.getString(key, "").split(",");
        return new MP4Config(s[0], s[1], s[2]);
      }
    }

    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      throw new StorageUnavailableException("No external storage or external storage not ready !");
    }

    final String TESTFILE =
        Environment.getExternalStorageDirectory().getPath() + "/spydroid-test.mp4";

    Log.i(TAG, "Testing H264 support... Test file saved at: " + TESTFILE);

    try {
      File file = new File(TESTFILE);
      file.createNewFile();
    } catch (IOException e) {
      throw new StorageUnavailableException(e.getMessage());
    }

    try {
      mMediaRecorder = new MediaRecorder();
      mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mMediaRecorder.setVideoEncoder(mVideoEncoder);
      mMediaRecorder.setVideoSize(mRequestedQuality.resX, mRequestedQuality.resY);
      mMediaRecorder.setVideoFrameRate(mRequestedQuality.framerate);
      mMediaRecorder.setVideoEncodingBitRate((int) (mRequestedQuality.bitrate * 0.8));
      mMediaRecorder.setOutputFile(TESTFILE);
      mMediaRecorder.setMaxDuration(3000);

      // We wait a little and stop recording
      mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
        public void onInfo(MediaRecorder mr, int what, int extra) {
          Log.d(TAG, "MediaRecorder callback called !");
          if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.d(TAG, "MediaRecorder: MAX_DURATION_REACHED");
          } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            Log.d(TAG, "MediaRecorder: MAX_FILESIZE_REACHED");
          } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
            Log.d(TAG, "MediaRecorder: INFO_UNKNOWN");
          } else {
            Log.d(TAG, "WTF ?");
          }
          mLock.release();
        }
      });

      // Start recording
      mMediaRecorder.prepare();
      createVirtualDisplay();
      mMediaRecorder.start();

      if (mLock.tryAcquire(6, TimeUnit.SECONDS)) {
        Log.d(TAG, "MediaRecorder callback was called :)");
        Thread.sleep(400);
      } else {
        Log.d(TAG, "MediaRecorder callback was not called after 6 seconds... :(");
      }
    } catch (RuntimeException e) {
      throw new ConfNotSupportedException(e.getMessage());
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      try {
        mMediaRecorder.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
      mMediaRecorder.release();
      mMediaRecorder = null;
    }

    // Retrieve SPS & PPS & ProfileId with MP4Config
    MP4Config config = new MP4Config(TESTFILE);

    // Delete dummy video
    File file = new File(TESTFILE);
    if (!file.delete()) Log.e(TAG, "Temp file could not be erased");

    Log.i(TAG, "H264 Test succeded...");

    // Save test result
    if (mSettings != null) {
      SharedPreferences.Editor editor = mSettings.edit();
      editor.putString(key,
          config.getProfileLevel() + "," + config.getB64SPS() + "," + config.getB64PPS());
      editor.commit();
    }

    return config;
  }

  private VirtualDisplay createVirtualDisplay() {
    return mediaProjection.createVirtualDisplay("ScreenStream", mRequestedQuality.resX,
        mRequestedQuality.resY, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
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

  @Override protected void encodeWithMediaRecorder() throws IOException {
    Log.d(TAG,
        "[ScreenStream] - encodeWithMediaRecorder(), Video encoded using the MediaRecorder API");

    // We need a local socket to forward data output by the camera to the packetizer
    createSockets();

    try {
      mMediaRecorder = new MediaRecorder();
      mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mMediaRecorder.setVideoEncoder(mVideoEncoder);
      mMediaRecorder.setVideoSize(mRequestedQuality.resX, mRequestedQuality.resY);
      mMediaRecorder.setVideoFrameRate(mRequestedQuality.framerate);

      // The bandwidth actually consumed is often above what was requested
      mMediaRecorder.setVideoEncodingBitRate((int) (mRequestedQuality.bitrate * 0.8));

      // We write the output of the camera in a local socket instead of a file !
      // This one little trick makes streaming feasible quiet simply: data from the camera
      // can then be manipulated at the other end of the socket
      FileDescriptor fd = null;
      if (sPipeApi == PIPE_API_PFD) {
        fd = mParcelWrite.getFileDescriptor();
      } else {
        fd = mSender.getFileDescriptor();
      }

      mMediaRecorder.setOutputFile(fd);

      mMediaRecorder.prepare();
      createVirtualDisplay();
      mMediaRecorder.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ConfNotSupportedException(e.getMessage());
    }

    InputStream is = null;

    if (sPipeApi == PIPE_API_PFD) {
      is = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
    } else {
      is = mReceiver.getInputStream();
    }

    // This will skip the MPEG4 header if this step fails we can't stream anything :(
    try {
      byte buffer[] = new byte[4];
      // Skip all atoms preceding mdat atom
      while (!Thread.interrupted()) {
        while (is.read() != 'm') ;
        is.read(buffer, 0, 3);
        if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
      }
    } catch (IOException e) {
      Log.e(TAG, "Couldn't skip mp4 header :/");
      stop();
      throw e;
    }

    // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
    mPacketizer.setInputStream(is);
    mPacketizer.start();

    mStreaming = true;
  }
}
