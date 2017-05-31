package com.antonio.droidcast;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
    implements IVLCVout.Callback, SurfaceHolder.Callback, IVLCVout.OnNewVideoLayoutListener {

  @Inject NsdUtils nsdUtils;

  private static final int SURFACE_BEST_FIT = 0;
  private static final int SURFACE_FIT_SCREEN = 1;
  private static final int SURFACE_FILL = 2;
  private static final int SURFACE_16_9 = 3;
  private static final int SURFACE_4_3 = 4;
  private static final int SURFACE_ORIGINAL = 5;
  private static int CURRENT_SIZE = SURFACE_BEST_FIT;

  private static final String CODE_KEY = "code_key";
  private static final String PATH_KEY = "path_key";
  private String code;
  private String path = null;

  @BindView(R.id.parent_view) RelativeLayout parentView;
  @BindView(R.id.loading_overlay) RelativeLayout loadingOverlay;
  @BindView(R.id.loading_textview) TextView loadingText;
  @BindView(R.id.video_surface_frame) FrameLayout mVideoSurfaceFrame;

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
  private ConnectionInfo connectionInfo = null;
  private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

  private int mVideoHeight = 0;
  private int mVideoWidth = 0;
  private int mVideoVisibleHeight = 0;
  private int mVideoVisibleWidth = 0;
  private int mVideoSarNum = 0;
  private int mVideoSarDen = 0;

  private final Handler mHandler = new Handler();
  private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

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

    holder = mSurface.getHolder();

    code = getIntent().getStringExtra(CODE_KEY);
    path = getIntent().getStringExtra(PATH_KEY);
    ViewDimensions.getDimension(parentView, new ViewDimensions.OnDimensionReady() {
      @Override public void onDimension(int width, int heigth) {
        int frameWidth = Math.round(Units.dpToPixel(VideoActivityVLC.this, 4));
        availableWidth = width - frameWidth;
        availableHeight = heigth - frameWidth;
        //setSize(videoWidth, videoHeight);
        holder.addCallback(VideoActivityVLC.this);
      }
    });
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    //setSize(videoWidth, videoHeight);
  }

  @Override protected void onResume() {
    super.onResume();
    if (connectionInfo != null) {
      createPlayer(connectionInfo);
    }
  }

  @Override protected void onPause() {
    releasePlayer();
    super.onPause();
  }

  @Override protected void onDestroy() {
    releasePlayer();
    super.onDestroy();
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      confirmLeave();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  private void confirmLeave() {
    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    alertDialog.setTitle("Stop streaming");
    alertDialog.setMessage("Do you want to finish this session?");
    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            releasePlayer();
            startActivity(SessionFinishActivity.createIntent(VideoActivityVLC.this));
            finish();
          }
        });
    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
        new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            //  Do nothing
          }
        });
    alertDialog.show();
  }

  /*************
   * Surface
   *************/
  private void setSize(int width, int height) {
    //if (mMediaPlayer == null) {
    //  return;
    //}
    //
    //int sw = getWindow().getDecorView().getWidth();
    //int sh = getWindow().getDecorView().getHeight();

    //boolean isPortrait =
    //    getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    //IVLCVout vout = mMediaPlayer.getVLCVout();
    //if (isPortrait) {
    //  vout.setWindowSize(availableWidth, availableHeight);
    //} else {
    //  vout.setWindowSize(availableHeight, availableWidth);
    //}
    //vout.setWindowSize(sw, sh);
    //mMediaPlayer.setAspectRatio(null);
    //mMediaPlayer.setScale(0);

    //holder.setFixedSize(availableWidth, availableHeight);
    //videoWidth = width;
    //videoHeight = height;
    //if (videoWidth * videoHeight <= 1) {
    //  return;
    //}
    //if (holder == null || mSurface == null) {
    //  return;
    //}
    //
    //// get screen size
    //int w = getWindow().getDecorView().getWidth();
    //int h = getWindow().getDecorView().getHeight();
    //
    //Log.d(TAG, "### screen w: " + w + " ,h:" + h + ", video w: " + width + " ,h:" + height);
    //// ### screen w: 1080 ,h:1920, video w: 1280 ,h:720
    //
    //// getWindow().getDecorView() doesn't always take orientation into
    //// account, we have to correct the values
    //boolean isPortrait =
    //    getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    //if (w > h && isPortrait || w < h && !isPortrait) {
    //  int i = w;
    //  w = h;
    //  h = i;
    //}
    //
    //float videoAR = (float) videoWidth / (float) videoHeight;
    //float screenAR = (float) w / (float) h;
    //
    //if (screenAR < videoAR) {
    //  h = (int) (w / videoAR);
    //} else {
    //  w = (int) (h * videoAR);
    //}
    //
    //// force surface buffer size
    //holder.setFixedSize(availableWidth, availableHeight);
    //holder.setSizeFromLayout();
    //holder.setFixedSize(videoWidth, videoHeight);

    // set display size
    //ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
    //lp.width = w;
    //lp.height = h;
    //lp.width = videoWidth;
    //lp.height = 2 * availableHeight / 3;
    //mSurface.setLayoutParams(lp);
    //mSurface.invalidate();

    // OLD - NEW

    int sw = getWindow().getDecorView().getWidth();
    int sh = getWindow().getDecorView().getHeight();

    // sanity check
    if (sw * sh == 0) {
      Log.e(TAG, "Invalid surface size");
      return;
    }

    mMediaPlayer.getVLCVout().setWindowSize(sw, sh);

    ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
    if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
      lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
      lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
      mSurface.setLayoutParams(lp);
      lp = mVideoSurfaceFrame.getLayoutParams();
      lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
      lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
      mVideoSurfaceFrame.setLayoutParams(lp);
      changeMediaPlayerLayout(sw, sh);
      return;
    }

    if (lp.width == lp.height && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
      mMediaPlayer.setAspectRatio(null);
      mMediaPlayer.setScale(0);
    }

    double dw = sw, dh = sh;
    final boolean isPortrait =
        getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

    if (sw > sh && isPortrait || sw < sh && !isPortrait) {
      dw = sh;
      dh = sw;
    }

    // compute the aspect ratio
    double ar, vw;
    if (mVideoSarDen == mVideoSarNum) {
            /* No indication about the density, assuming 1:1 */
      vw = mVideoVisibleWidth;
      ar = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
    } else {
            /* Use the specified aspect ratio */
      vw = mVideoVisibleWidth * (double) mVideoSarNum / mVideoSarDen;
      ar = vw / mVideoVisibleHeight;
    }

    // compute the display aspect ratio
    double dar = dw / dh;

    switch (CURRENT_SIZE) {
      case SURFACE_BEST_FIT:
        if (dar < ar) {
          dh = dw / ar;
        } else {
          dw = dh * ar;
        }
        break;
      case SURFACE_FIT_SCREEN:
        if (dar >= ar) {
          dh = dw / ar; /* horizontal */
        } else {
          dw = dh * ar; /* vertical */
        }
        break;
      case SURFACE_FILL:
        break;
      case SURFACE_16_9:
        ar = 16.0 / 9.0;
        if (dar < ar) {
          dh = dw / ar;
        } else {
          dw = dh * ar;
        }
        break;
      case SURFACE_4_3:
        ar = 4.0 / 3.0;
        if (dar < ar) {
          dh = dw / ar;
        } else {
          dw = dh * ar;
        }
        break;
      case SURFACE_ORIGINAL:
        dh = mVideoVisibleHeight;
        dw = vw;
        break;
    }

    // set display size
    lp.width = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
    lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
    mSurface.setLayoutParams(lp);

    // set frame size (crop if necessary)
    lp = mVideoSurfaceFrame.getLayoutParams();
    lp.width = (int) Math.floor(dw);
    lp.height = (int) Math.floor(dh);
    mVideoSurfaceFrame.setLayoutParams(lp);

    mSurface.invalidate();
  }

  private void changeMediaPlayerLayout(int displayW, int displayH) {
        /* Change the video placement using the MediaPlayer API */
    switch (CURRENT_SIZE) {
      case SURFACE_BEST_FIT:
        mMediaPlayer.setAspectRatio(null);
        mMediaPlayer.setScale(0);
        break;
      case SURFACE_FIT_SCREEN:
      case SURFACE_FILL: {
        Media.VideoTrack vtrack = mMediaPlayer.getCurrentVideoTrack();
        if (vtrack == null) return;
        final boolean videoSwapped = vtrack.orientation == Media.VideoTrack.Orientation.LeftBottom
            || vtrack.orientation == Media.VideoTrack.Orientation.RightTop;
        if (CURRENT_SIZE == SURFACE_FIT_SCREEN) {
          int videoW = vtrack.width;
          int videoH = vtrack.height;

          if (videoSwapped) {
            int swap = videoW;
            videoW = videoH;
            videoH = swap;
          }
          if (vtrack.sarNum != vtrack.sarDen) videoW = videoW * vtrack.sarNum / vtrack.sarDen;

          float ar = videoW / (float) videoH;
          float dar = displayW / (float) displayH;

          float scale;
          if (dar >= ar) {
            scale = displayW / (float) videoW; /* horizontal */
          } else {
            scale = displayH / (float) videoH; /* vertical */
          }
          mMediaPlayer.setScale(scale);
          mMediaPlayer.setAspectRatio(null);
        } else {
          mMediaPlayer.setScale(0);
          mMediaPlayer.setAspectRatio(
              !videoSwapped ? "" + displayW + ":" + displayH : "" + displayH + ":" + displayW);
        }
        break;
      }
      case SURFACE_16_9:
        mMediaPlayer.setAspectRatio("16:9");
        mMediaPlayer.setScale(0);
        break;
      case SURFACE_4_3:
        mMediaPlayer.setAspectRatio("4:3");
        mMediaPlayer.setScale(0);
        break;
      case SURFACE_ORIGINAL:
        mMediaPlayer.setAspectRatio(null);
        mMediaPlayer.setScale(1);
        break;
    }
  }

  /*************
   * Player
   *************/

  private void createPlayer(ConnectionInfo connectionInfo) {
    //releasePlayer();
    try {

      // Create LibVLC
      // TODO: make this more robust, and sync with audio demo
      ArrayList<String> options = new ArrayList<>();
      //options.add("--aout=opensles");
      //options.add("--audio-time-stretch"); // time stretching
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
      vout.attachViews(this);
      //vout.setWindowSize(availableWidth, availableHeight);
    } catch (Exception e) {
      e.printStackTrace();
      Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
    }
  }

  private void releasePlayer() {
    if (libvlc == null) {
      return;
    }

    if (mOnLayoutChangeListener != null) {
      mVideoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
      mOnLayoutChangeListener = null;
    }

    final IVLCVout vout = mMediaPlayer.getVLCVout();
    vout.removeCallback(this);
    vout.detachViews();

    mMediaPlayer.stop();

    libvlc.release();
    libvlc = null;
  }

  /*************
   * Events
   *************/

  @Override public void surfaceCreated(SurfaceHolder holder) {
    if (path != null) {
      createPlayer(null);
      return;
    }
    nsdUtils.discoverNsdService(this, code, new NsdUtils.NsdResolveCallback() {
      @Override public void onHostFound(final ConnectionInfo connectionInfo) {
        VideoActivityVLC.this.connectionInfo = connectionInfo;
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

  @Override public void onSurfacesCreated(IVLCVout vout) {
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

    if (mOnLayoutChangeListener == null) {
      mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
        private final Runnable mRunnable = new Runnable() {
          @Override public void run() {
            setSize(0, 0);
          }
        };

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
          if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            mHandler.removeCallbacks(mRunnable);
            mHandler.post(mRunnable);
          }
        }
      };
    }
    mVideoSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
  }

  @Override public void onSurfacesDestroyed(IVLCVout vout) {

  }

  @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override public void surfaceDestroyed(SurfaceHolder holder) {

  }

  @Override public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth,
      int visibleHeight, int sarNum, int sarDen) {
    Log.d(TAG,
        "## onNewVideoLayout " + width + " " + height + " " + visibleWidth + " " + visibleHeight);
    //if (width * height == 0) {
    //  return;
    //}
    //
    //// store video size
    //videoWidth = width;
    //videoHeight = height;
    //setSize(videoWidth, videoHeight);
    mVideoWidth = width;
    mVideoHeight = height;
    mVideoVisibleWidth = visibleWidth;
    mVideoVisibleHeight = visibleHeight;
    mVideoSarNum = sarNum;
    mVideoSarDen = sarDen;
    setSize(0, 0);
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
          nsdUtils.tearDown();
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