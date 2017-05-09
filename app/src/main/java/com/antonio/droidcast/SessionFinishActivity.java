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

public class SessionFinishActivity extends BaseActivity {

  @BindView(R.id.session_finish_textview) TextView sessionFinishTextView;

  /**
   * Create an intent that opens this activity
   *
   * @param context Activity that opens this one.
   * @return Intent
   */
  public static Intent createIntent(Context context) {
    return new Intent(context, SessionFinishActivity.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_session_finish);

    ButterKnife.bind(this);

    sessionFinishTextView.postDelayed(new Runnable() {
      @Override public void run() {
        sessionFinishTextView.setTranslationY(Units.dpToPixel(SessionFinishActivity.this, -50));
        sessionFinishTextView.animate().translationY(0f).alpha(1f).setDuration(400).start();
      }
    }, 250);
  }

  /**
   * Handler for close button click event.
   *
   * @param view Clicked view.
   */
  @OnClick(R.id.session_finish_close_button) public void onCloseClick(View view) {
    startActivity(HomeActivity.createIntent(this));
  }
}
