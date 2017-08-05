package com.app.droidcast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.app.droidcast.dao.DaoFactory;
import com.app.droidcast.ioc.IOCProvider;
import com.app.droidcast.utils.BounceView;
import com.app.droidcast.utils.NotificationUtils;
import com.app.droidcast.utils.NsdUtils;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class HomeActivity extends BaseActivity {

  @Inject DaoFactory daoFactory;
  @Inject NsdUtils nsdUtils;
  @Inject NotificationUtils notificationUtils;
  @BindView(R.id.home_code_editText) EditText codeEditText;
  @BindView(R.id.home_code_empty_textview) TextView codeEmptyTextView;
  @BindView(R.id.home_click_here_textview) TextView clickHereTextView;

  /**
   * Create an intent that opens this activity
   *
   * @param context Activity that opens this one.
   * @return Intent
   */
  public static Intent createIntent(Context context) {
    Intent intent = new Intent(context, HomeActivity.class);
    //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
    //    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    ButterKnife.bind(this);
    IOCProvider.getInstance().inject(this);

    // Sets the port of the RTSP server to 55640
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putString(RtspServer.KEY_PORT, String.valueOf(NsdUtils.DEFAULT_PORT));
    editor.apply();

    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Caveat-Regular.ttf");
    clickHereTextView.setTypeface(font);
  }

  /**
   * Handle start streaming button click event.
   *
   * @param v Clicked view.
   */
  @OnClick(R.id.home_stream_button) public void startStreaming(View v) {
    BounceView.animate(v, 1.3f, new Runnable() {
      @Override public void run() {
        startActivity(MediaShareActivity.createIntent(HomeActivity.this));
      }
    });
  }

  /**
   * Handle connect action. Check that code is not empty.
   *
   * @param v Clicked button.
   */
  @OnClick(R.id.home_connect_button) public void onConnect(final View v) {
    if (codeEditText.getText().length() < 6) {
      v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));

      Animation currentAnimation = codeEmptyTextView.getAnimation();
      if (currentAnimation != null) {
        currentAnimation.cancel();
      }
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
    startActivity(
        VideoActivityVLC.createIntent(HomeActivity.this, codeEditText.getText().toString()));
    //startActivity(VideoActivityVLC.createIntentPath(HomeActivity.this,
    //    "rtsp://192.168.1.10:7654/test2-rtsp"));
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      HomeActivity.this.moveTaskToBack(true);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }
}