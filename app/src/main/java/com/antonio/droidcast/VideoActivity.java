package com.antonio.droidcast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.models.ConnectionInfo;
import com.antonio.droidcast.utils.NsdUtils;
import java.io.IOException;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class VideoActivity extends BaseActivity
    implements MediaPlayer.OnPreparedListener, SurfaceHolder.Callback {

  @Inject NsdUtils nsdUtils;

  private static final String CODE_KEY = "code_key";
  private String code;
  private MediaPlayer mediaPlayer;

  public static Intent createIntent(Context context, String code) {
    Intent intent = new Intent(context, VideoActivity.class);
    intent.putExtra(CODE_KEY, code);
    return intent;
  }

  @BindView(R.id.surface_player) SurfaceView surfaceView;
  private SurfaceHolder surfaceHolder;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_video);

    IOCProvider.getInstance().inject(this);
    ButterKnife.bind(this);

    surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);

    code = getIntent().getStringExtra(CODE_KEY);
  }

  @Override public void surfaceCreated(SurfaceHolder holder) {
    nsdUtils.discoverNsdService(this, code, new NsdUtils.NsdResolveCallback() {
      @Override public void onHostFound(final ConnectionInfo connectionInfo) {
        Log.d(TAG, "[VideoActivity] - onHostFound()");
        VideoActivity.this.runOnUiThread(new Runnable() {
          @Override public void run() {
            openStream(connectionInfo);
          }
        });
      }

      @Override public void onError() {
        Log.e(TAG, "[VideoActivity] - error");
      }
    });
  }

  private void openStream(ConnectionInfo connectionInfo) {

    mediaPlayer = new MediaPlayer();
    mediaPlayer.setDisplay(surfaceHolder);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String port = sharedPreferences.getString(RtspServer.KEY_PORT,
        String.valueOf(RtspServer.DEFAULT_RTSP_PORT));

    //Map<String, String> headers = new HashMap<>();
    //String value = MediaShareActivity.USERNAME + ":" + code;
    //headers.put("authorization",
    //    "Basic " + Base64.encodeToString(value.getBytes(), Base64.NO_WRAP));

    try {
      mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(
          "rtsp://" + MediaShareActivity.USERNAME + ":" + code + "@" + connectionInfo.getHost()
              .getHostAddress() + ":" + connectionInfo.getPort()));
      mediaPlayer.setOnPreparedListener(this);
      mediaPlayer.prepareAsync();
    } catch (IOException e) {
      Log.e(TAG, "[VideoActivity] - openStream(), error");
      e.printStackTrace();
    }
  }

  @Override public void onPrepared(MediaPlayer mp) {
    Log.d(TAG, "[VideoActivity] - onPrepared()");
    mediaPlayer.start();
  }

  @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  @Override public void surfaceDestroyed(SurfaceHolder holder) {

  }
}
