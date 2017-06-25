package com.app.droidcast;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.app.droidcast.ioc.IOCProvider;
import com.app.droidcast.models.ConnectionInfo;
import com.app.droidcast.utils.NsdUtils;
import com.app.droidcast.utils.Units;
import java.util.ArrayList;
import javax.inject.Inject;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class VideoActivityVLC extends BaseActivity implements IVLCVout.OnNewVideoLayoutListener {

  @Inject NsdUtils nsdUtils;

  private static final String CODE_KEY = "code_key";
  private static final String PATH_KEY = "path_key";
  private String code = null;
  private String path = null;

  @BindView(R.id.parent_view) RelativeLayout parentView;
  @BindView(R.id.loading_overlay) RelativeLayout loadingOverlay;
  @BindView(R.id.loading_textview) TextView loadingText;
  @BindView(R.id.video_surface_frame) FrameLayout mVideoSurfaceFrame;

  // display surface
  @BindView(R.id.surface) SurfaceView mVideoSurface;

  private static final int SURFACE_BEST_FIT = 0;
  private static final int SURFACE_FIT_SCREEN = 1;
  private static final int SURFACE_FILL = 2;
  private static final int SURFACE_16_9 = 3;
  private static final int SURFACE_4_3 = 4;
  private static final int SURFACE_ORIGINAL = 5;
  private static int CURRENT_SIZE = SURFACE_BEST_FIT;

  private final Handler mHandler = new Handler();
  private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

  private LibVLC mLibVLC = null;
  private MediaPlayer mMediaPlayer = null;
  private int mVideoHeight = 0;
  private int mVideoWidth = 0;
  private int mVideoVisibleHeight = 0;
  private int mVideoVisibleWidth = 0;
  private int mVideoSarNum = 0;
  private int mVideoSarDen = 0;

  private ConnectionInfo connectionInfo;

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

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_video_vlc);
    IOCProvider.getInstance().inject(this);
    ButterKnife.bind(this);

    updateStatusTextView(getString(R.string.video_activity_looking_for_host));
    code = getIntent().getStringExtra(CODE_KEY);
    path = getIntent().getStringExtra(PATH_KEY);

    final ArrayList<String> args = new ArrayList<>();
    args.add("-vvv");
    mLibVLC = new LibVLC(this, args);
    mMediaPlayer = new MediaPlayer(mLibVLC);

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
            startPlayer();
          }
        });
      }

      @Override public void onError() {
        Log.e(TAG, "[VideoActivityVLC] - error");
      }
    });
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mMediaPlayer.release();
    mLibVLC.release();
  }

  @Override protected void onStart() {
    super.onStart();

    mMediaPlayer.setEventListener(new MyPlayerListener());

    final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
    vlcVout.setVideoView(mVideoSurface);
    vlcVout.attachViews(this);

    if (connectionInfo != null) {
      startPlayer();
    }

    if (mOnLayoutChangeListener == null) {
      mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
        private final Runnable mRunnable = new Runnable() {
          @Override public void run() {
            updateVideoSurfaces();
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

  private void startPlayer() {
    if (!mMediaPlayer.isPlaying()) {
      Media media = new Media(mLibVLC, Uri.parse(connectionInfo != null ? "rtsp://"
          + MediaShareActivity.USERNAME
          + ":"
          + code
          + "@"
          + connectionInfo.getHost()
          + ":"
          + connectionInfo.getPort() : path));
      media.addOption(":network-caching=150");
      media.addOption(":clock-jitter=0");
      media.addOption(":clock-synchro=0");
      mMediaPlayer.setMedia(media);
      media.release();
      mMediaPlayer.play();
    }
  }

  @Override protected void onStop() {
    super.onStop();

    if (mOnLayoutChangeListener != null) {
      mVideoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
      mOnLayoutChangeListener = null;
    }

    mMediaPlayer.stop();
    mMediaPlayer.getVLCVout().detachViews();
    mMediaPlayer.setEventListener(null);
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

  private void updateVideoSurfaces() {
    int sw = getWindow().getDecorView().getWidth();
    int sh = getWindow().getDecorView().getHeight();

    // sanity check
    if (sw * sh == 0) {
      Log.e(TAG, "Invalid surface size");
      return;
    }

    mMediaPlayer.getVLCVout().setWindowSize(sw, sh);

    ViewGroup.LayoutParams lp = mVideoSurface.getLayoutParams();
    if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
      lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
      lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
      mVideoSurface.setLayoutParams(lp);
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
    mVideoSurface.setLayoutParams(lp);

    // set frame size (crop if necessary)
    lp = mVideoSurfaceFrame.getLayoutParams();
    lp.width = (int) Math.floor(dw);
    lp.height = (int) Math.floor(dh);
    mVideoSurfaceFrame.setLayoutParams(lp);

    mVideoSurface.invalidate();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) @Override
  public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth,
      int visibleHeight, int sarNum, int sarDen) {
    mVideoWidth = width;
    mVideoHeight = height;
    mVideoVisibleWidth = visibleWidth;
    mVideoVisibleHeight = visibleHeight;
    mVideoSarNum = sarNum;
    mVideoSarDen = sarDen;
    updateVideoSurfaces();
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
            startActivity(SessionFinishActivity.createIntent(VideoActivityVLC.this, false));
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

  private class MyPlayerListener implements MediaPlayer.EventListener {

    @Override public void onEvent(MediaPlayer.Event event) {
      switch (event.type) {
        case MediaPlayer.Event.EndReached:
          startActivity(SessionFinishActivity.createIntent(VideoActivityVLC.this, true));
          finish();
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
}