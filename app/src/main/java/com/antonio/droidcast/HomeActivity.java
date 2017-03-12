package com.antonio.droidcast;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.antonio.droidcast.dao.DaoFactory;
import com.antonio.droidcast.ioc.IOCProvider;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class HomeActivity extends BaseActivity {

  @Inject DaoFactory daoFactory;
  @BindView(R.id.home_code_editText) EditText codeEditText;

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
    //startActivity(TestActivity.createIntent(this));
  }

  @OnClick(R.id.home_connect_button) public void onConnect(View v) {
    startActivity(VideoActivity.createIntent(this, codeEditText.getText().toString()));
  }
}
