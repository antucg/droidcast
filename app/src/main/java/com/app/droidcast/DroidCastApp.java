package com.app.droidcast;

import android.app.Application;
import com.app.droidcast.ioc.DroidCastComponent;
import com.app.droidcast.ioc.IOCProvider;

/**
 * Main application class
 */
public class DroidCastApp extends Application {

  private static DroidCastComponent droidCastComponent;

  @Override public void onCreate() {
    super.onCreate();

    // Init dagger component
    IOCProvider.build(this);
    IOCProvider.getInstance().inject(this);
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
