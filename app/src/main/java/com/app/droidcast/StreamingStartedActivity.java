package com.app.droidcast;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import com.app.droidcast.utils.BounceView;
import com.app.droidcast.utils.Units;

public class StreamingStartedActivity extends BaseActivity {

  @BindView(R.id.streaming_started_textview) TextView streamingTextView;
  @BindView(R.id.enable_microphone_switch) Switch enableMicrophoneSwitch;

  /**
   * Creates an intent that opens this activity.
   *
   * @param context Application context.
   * @return Intent.
   */
  public static Intent createIntent(Context context) {
    return new Intent(context, StreamingStartedActivity.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_streaming_started);

    ButterKnife.bind(this);

    streamingTextView.postDelayed(new Runnable() {
      @Override public void run() {
        streamingTextView.setTranslationY(Units.dpToPixel(StreamingStartedActivity.this, -50));
        streamingTextView.animate().translationY(0f).alpha(1f).setDuration(400).start();
      }
    }, 250);
  }

  /**
   * Handler for switch button. Enable/disable microphone.
   *
   * @param compoundButton View
   * @param enable Whether the switch button is active or not.
   */
  @OnCheckedChanged(R.id.enable_microphone_switch) public void onSwitchChange(
      CompoundButton compoundButton, boolean enable) {
    AudioManager audioManager =
        (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    audioManager.setMicrophoneMute(!enable);
  }

  /**
   * Handler for close button click event.
   *
   * @param view Clicked view.
   */
  @OnClick(R.id.streaming_started_close_button) public void onCloseClick(View view) {
    BounceView.animate(view, new Runnable() {
      @Override public void run() {
        StreamingStartedActivity.this.moveTaskToBack(true);
      }
    });
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      StreamingStartedActivity.this.moveTaskToBack(true);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }
}
