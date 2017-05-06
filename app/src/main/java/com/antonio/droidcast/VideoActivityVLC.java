package com.antonio.droidcast;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.models.ConnectionInfo;
import com.antonio.droidcast.utils.NsdUtils;
import com.antonio.droidcast.utils.Units;
import com.antonio.droidcast.utils.ViewDimensions;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.inject.Inject;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class VideoActivityVLC extends BaseActivity
    implements IVLCVout.Callback, SurfaceHolder.Callback {

  @Inject NsdUtils nsdUtils;

  private static final String CODE_KEY = "code_key";
  private static final String PATH_KEY = "path_key";
  private String code;
  private String path;

  @BindView(R.id.parent_view) RelativeLayout parentView;
  @BindView(R.id.loading_overlay) RelativeLayout loadingOverlay;
  @BindView(R.id.loading_textview) TextView loadingText;

  // display surface
  @BindView(R.id.surface) SurfaceView mSurface;
  private SurfaceHolder holder;

  // media player
  private LibVLC libvlc;
  private MediaPlayer mMediaPlayer = null;
  private int availableWidth;
  private int availableHeight;
  private int videoWidth;
  private int videoHeight;

  public static Intent createIntent(Context context, String code) {
    Intent intent = new Intent(context, VideoActivityVLC.class);
    intent.putExtra(CODE_KEY, code);
    return intent;
  }

  public static Intent createIntentPath(Context context, String path) {
    Intent intent = new Intent(context, VideoActivityVLC.class);
    intent.putExtra(PATH_KEY, path);
    return intent;
  }

  /*************
   * Activity
   *************/

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_video_vlc);

    IOCProvider.getInstance().inject(this);
    ButterKnife.bind(this);

    updateStatusTextView(getString(R.string.video_activity_looking_for_host));
    ViewDimensions.getDimension(parentView, new ViewDimensions.OnDimensionReady() {
      @Override public void onDimension(int width, int heigth) {
        availableWidth = width;
        availableHeight = heigth;
        setSize(videoWidth, videoHeight);
      }
    });

    holder = mSurface.getHolder();

    code = getIntent().getStringExtra(CODE_KEY);
    path = getIntent().getStringExtra(PATH_KEY);
    holder.addCallback(this);
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    setSize(videoWidth, videoHeight);
  }

  //@Override protected void onResume() {
  //  super.onResume();
  //  createPlayer();
  //}

  @Override protected void onPause() {
    super.onPause();
    releasePlayer();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    releasePlayer();
  }

  /*************
   * Surface
   *************/
  private void setSize(int width, int height) {
    videoWidth = width;
    videoHeight = height;
    if (videoWidth * videoHeight <= 1) {
      return;
    }
    if (holder == null || mSurface == null) {
      return;
    }

    // get screen size
    int w = getWindow().getDecorView().getWidth();
    int h = getWindow().getDecorView().getHeight();

    Log.d(TAG, "### screen w: " + w + " ,h:" + h + ", video w: " + width + " ,h:" + height);
    // ### screen w: 1080 ,h:1920, video w: 1280 ,h:720

    // getWindow().getDecorView() doesn't always take orientation into
    // account, we have to correct the values
    boolean isPortrait =
        getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    if (w > h && isPortrait || w < h && !isPortrait) {
      int i = w;
      w = h;
      h = i;
    }

    float videoAR = (float) videoWidth / (float) videoHeight;
    float screenAR = (float) w / (float) h;

    if (screenAR < videoAR) {
      h = (int) (w / videoAR);
    } else {
      w = (int) (h * videoAR);
    }

    // force surface buffer size
    holder.setFixedSize(availableWidth, availableHeight);
    //holder.setFixedSize(videoWidth, videoHeight);

    // set display size
    //ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
    //lp.width = w;
    //lp.height = h;
    //lp.width = videoWidth;
    //lp.height = 2 * availableHeight / 3;
    //mSurface.setLayoutParams(lp);
    //mSurface.invalidate();
  }

  /*************
   * Player
   *************/

  private void createPlayer(ConnectionInfo connectionInfo) {
    releasePlayer();
    try {

      // Create LibVLC
      // TODO: make this more robust, and sync with audio demo
      ArrayList<String> options = new ArrayList<>();
      options.add("--aout=opensles");
      options.add("--audio-time-stretch"); // time stretching
      options.add("-vvv"); // verbosity
      libvlc = new LibVLC(this, options);
      holder.setKeepScreenOn(true);

      // Create media player
      mMediaPlayer = new MediaPlayer(libvlc);
      mMediaPlayer.setEventListener(mPlayerListener);

      // Set up video output
      final IVLCVout vout = mMediaPlayer.getVLCVout();
      vout.setVideoView(mSurface);
      vout.addCallback(this);
      vout.attachViews();

      Media m = new Media(libvlc, Uri.parse(connectionInfo != null ? "rtsp://"
          + MediaShareActivity.USERNAME
          + ":"
          + code
          + "@"
          + connectionInfo.getHost()
          + ":"
          + connectionInfo.getPort() : path));
      mMediaPlayer.setMedia(m);
      mMediaPlayer.play();
    } catch (Exception e) {
      Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
    }
  }

  private void releasePlayer() {
    if (libvlc == null) {
      return;
    }
    mMediaPlayer.stop();
    final IVLCVout vout = mMediaPlayer.getVLCVout();
    vout.removeCallback(this);
    vout.detachViews();
    holder = null;
    libvlc.release();
    libvlc = null;
    videoWidth = 0;
    videoHeight = 0;
  }

  /*************
   * Events
   *************/

  private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

  @Override
  public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight,
      int sarNum, int sarDen) {
    if (width * height == 0) {
      return;
    }

    // store video size
    videoWidth = width;
    videoHeight = height;
    setSize(videoWidth, videoHeight);
  }

  @Override public void onSurfacesCreated(IVLCVout vout) {

  }

  @Override public void onSurfacesDestroyed(IVLCVout vout) {

  }

  @Override public void onHardwareAccelerationError(IVLCVout vlcVout) {

  }

  @Override public void surfaceCreated(SurfaceHolder holder) {
    nsdUtils.discoverNsdService(this, code, new NsdUtils.NsdResolveCallback() {
      @Override public void onHostFound(final ConnectionInfo connectionInfo) {
        Log.d(TAG, "[VideoActivityVLC] - onHostFound()");
        VideoActivityVLC.this.runOnUiThread(new Runnable() {
          @Override public void run() {
            updateStatusTextView(getString(R.string.video_activity_host_found));
            loadingOverlay.postDelayed(new Runnable() {
              @Override public void run() {
                updateStatusTextView(getString(R.string.video_activity_connecting));
              }
            }, 1000);
            createPlayer(connectionInfo);
          }
        });
      }

      @Override public void onError() {
        Log.e(TAG, "[VideoActivityVLC] - error");
      }
    });
  }

  @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  @Override public void surfaceDestroyed(SurfaceHolder holder) {

  }

  private class MyPlayerListener implements MediaPlayer.EventListener {
    private WeakReference<VideoActivityVLC> mOwner;

    MyPlayerListener(VideoActivityVLC owner) {
      mOwner = new WeakReference<>(owner);
    }

    @Override public void onEvent(MediaPlayer.Event event) {
      VideoActivityVLC player = mOwner.get();

      switch (event.type) {
        case MediaPlayer.Event.EndReached:
          player.releasePlayer();
          break;
        case MediaPlayer.Event.Playing:
          updateStatusTextView(getString(R.string.video_activity_connected));
          loadingOverlay.postDelayed(new Runnable() {
            @Override public void run() {
              hideOverlay();
            }
          }, 1000);
          break;
        case MediaPlayer.Event.Paused:
        case MediaPlayer.Event.Stopped:
        default:
          break;
      }
    }
  }

  /**
   * Update TextView text and animates it.
   *
   * @param text New text to be displayed.
   */
  private void updateStatusTextView(String text) {
    loadingText.setAlpha(0f);
    loadingText.setText(text);
    loadingText.setTranslationY(Units.dpToPixel(this, 50));
    loadingText.animate().setDuration(400).translationY(0f).alpha(1f).start();
  }

  private void hideOverlay() {
    loadingOverlay.animate().alpha(0f).setDuration(500).start();
  }
}