package com.antonio.droidcast;

import android.app.Application;
import com.antonio.droidcast.ioc.IOCProvider;

/**
 * Main application class
 */
public class DroidCastApp extends Application {

  @Override public void onCreate() {
    super.onCreate();
    IOCProvider.build();
  }
}
