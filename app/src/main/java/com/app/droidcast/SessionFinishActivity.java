package com.app.droidcast;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.app.droidcast.utils.BounceView;
import com.app.droidcast.utils.Units;

public class SessionFinishActivity extends BaseActivity {

  private final static String FINISHED_BY_CLIENT_KEY = "finished_by_client_key";
  @BindView(R.id.session_finish_textview) TextView sessionFinishTextView;

  /**
   * Create an intent that opens this activity
   *
   * @param context Activity that opens this one.
   * @return Intent
   */
  public static Intent createIntent(Context context, boolean finishByServer) {
    Intent intent = new Intent(context, SessionFinishActivity.class);
    intent.putExtra(FINISHED_BY_CLIENT_KEY, finishByServer);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_session_finish);

    ButterKnife.bind(this);

    if (getIntent().getBooleanExtra(FINISHED_BY_CLIENT_KEY, false)) {
      sessionFinishTextView.setText(getString(R.string.session_finished_by_server));
    }

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
    BounceView.animate(view, new Runnable() {
      @Override public void run() {
        startActivity(HomeActivity.createIntent(SessionFinishActivity.this));
      }
    });
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      startActivity(HomeActivity.createIntent(SessionFinishActivity.this));
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }
}
