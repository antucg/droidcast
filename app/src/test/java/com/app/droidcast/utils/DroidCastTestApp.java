package com.app.droidcast.utils;

import com.app.droidcast.DroidCastApp;
import com.app.droidcast.utils.ioc.IOCProviderTest;

/**
 * Created by antonio.carrasco on 09/10/2016.
 */

public class DroidCastTestApp extends DroidCastApp {

  @Override public void onCreate() {
    super.onCreate();

    // Init dagger component
    IOCProviderTest.build(this);
  }
}
