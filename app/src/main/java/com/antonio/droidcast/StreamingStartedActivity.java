package com.antonio.droidcast;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.antonio.droidcast.utils.Units;

public class StreamingStartedActivity extends BaseActivity {

  @BindView(R.id.streaming_started_textview) TextView streamingTextView;

  /**
   * Creates an intent that opens this activity.
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
   * Handler for close button click event.
   *
   * @param view Clicked view.
   */
  @OnClick(R.id.streaming_started_close_button) public void onCloseClick(View view) {
    this.moveTaskToBack(true);
  }
}
