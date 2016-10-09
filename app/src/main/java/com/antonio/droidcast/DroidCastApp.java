package com.antonio.droidcast;

import android.app.Application;
import com.antonio.droidcast.ioc.DaggerDroidCastComponent;
import com.antonio.droidcast.ioc.DroidCastComponent;
import com.antonio.droidcast.ioc.DroidCastModule;
import com.antonio.droidcast.ioc.IOCProvider;

/**
 * Main application class
 */
public class DroidCastApp extends Application {

  private static DroidCastComponent droidCastComponent;

  @Override public void onCreate() {
    super.onCreate();

    // Init dagger component
    droidCastComponent =
        DaggerDroidCastComponent.builder().droidCastModule(new DroidCastModule(this)).build();

    IOCProvider.build(this);
  }

  /**
   * Return component for injections
   *
   * @return DroidCastComponent instance
   */
  public static DroidCastComponent getComponent() {
    return droidCastComponent;
  }
}
