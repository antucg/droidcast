package com.app.droidcast.utils;

import com.app.droidcast.DroidCastApp;
import com.app.droidcast.ioc.DroidCastModule;
import com.app.droidcast.utils.ioc.DaggerDroidCastTestComponent;
import com.app.droidcast.utils.ioc.DroidCastTestComponent;

/**
 * Created by antonio.carrasco on 09/10/2016.
 */

public class DroidCastTestApp extends DroidCastApp {

  private static DroidCastTestComponent droidCastTestComponent;

  @Override public void onCreate() {
    super.onCreate();

    // Init dagger test component
    droidCastTestComponent =
        DaggerDroidCastTestComponent.builder().droidCastModule(new DroidCastModule(this)).build();
  }

  /**
   * Return component for injections
   *
   * @return DroidCastComponent instance
   */
  public static DroidCastTestComponent getComponent() {
    return droidCastTestComponent;
  }
}
