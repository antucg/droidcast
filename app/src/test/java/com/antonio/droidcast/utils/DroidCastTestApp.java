package com.antonio.droidcast.utils;

import com.antonio.droidcast.DroidCastApp;
import com.antonio.droidcast.ioc.DroidCastModule;
import com.antonio.droidcast.utils.ioc.DaggerDroidCastTestComponent;
import com.antonio.droidcast.utils.ioc.DroidCastTestComponent;

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
