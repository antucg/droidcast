package com.antonio.droidcast;

import android.app.Application;
import com.antonio.droidcast.ioc.DroidCastComponent;
import com.antonio.droidcast.ioc.IOCProvider;
import javax.inject.Inject;

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
