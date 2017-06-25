package com.app.droidcast;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.app.droidcast.ioc.IOCProvider;

/**
 * Base class for activities that implements some of the functionality common to all activities.
 */

public class BaseActivity extends AppCompatActivity {

  // Tag to use when logging messages
  protected final String TAG = this.getClass().getSimpleName();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
}
