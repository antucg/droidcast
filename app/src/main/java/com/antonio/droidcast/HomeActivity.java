package com.antonio.droidcast;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.antonio.droidcast.dao.DaoFactory;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.utils.NsdUtils;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class HomeActivity extends BaseActivity {

  @Inject DaoFactory daoFactory;
  @Inject NsdUtils nsdUtils;
  @BindView(R.id.home_code_editText) EditText codeEditText;
  @BindView(R.id.home_code_empty_textview) TextView codeEmptyTextView;

  /**
   * Create an intent that opens this activity
   *
   * @param context Activity that opens this one.
   * @return Intent
   */
  public static Intent createIntent(Context context) {
    Intent intent = new Intent(context, HomeActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    ButterKnife.bind(this);
    IOCProvider.getInstance().inject(this);

    // Sets the port of the RTSP server to 1234
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
    editor.apply();
  }

  /**
   * Handle start streaming button click event.
   *
   * @param v Clicked view.
   */
  @OnClick(R.id.home_stream_button) public void startStreaming(View v) {
    startActivity(MediaShareActivity.createIntent(this));
  }

  /**
   * Handle connect action. Check that code is not empty.
   *
   * @param v Clicked button.
   */
  @OnClick(R.id.home_connect_button) public void onConnect(final View v) {
    if (codeEditText.getText().length() < 6) {
      v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));

      codeEmptyTextView.getAnimation().cancel();
      codeEmptyTextView.setAlpha(1f);
      codeEmptyTextView.setVisibility(View.VISIBLE);
      codeEmptyTextView.animate()
          .setStartDelay(2000)
          .alpha(0f)
          .setDuration(500)
          .withEndAction(new Runnable() {
            @Override public void run() {
              codeEmptyTextView.setVisibility(View.INVISIBLE);
              codeEmptyTextView.setAlpha(1f);
            }
          });
      return;
    }
    //startActivity(VideoActivity.createIntent(this, codeEditText.getText().toString()));
    startActivity(VideoActivityVLC.createIntent(this, codeEditText.getText().toString()));
    //startActivity(VideoActivityVLC.createIntentPath(this, "rtsp://192.168.1.10:7654/test2-rtsp"));
  }

  @Override protected void onDestroy() {
    nsdUtils.tearDown();
    stopService(new Intent(this, RtspServer.class));
    NotificationManager notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(MediaShareActivity.NOTIFICATION_ID);
    super.onDestroy();
  }
}
