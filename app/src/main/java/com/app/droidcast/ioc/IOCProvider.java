package com.app.droidcast.ioc;

import android.app.Application;

/**
 * Class that builds the graph and holds the module that will inject dependencies.
 */
public class IOCProvider {

  /**
   * Module that contains all the provides
   */
  private static DroidCastComponent droidCastComponent;

  /**
   * Hidden constructor
   */
  private IOCProvider() {
  }

  /**
   * Build the graph with all the dependencies
   */
  public static void build(Application application) {
    if (droidCastComponent != null) {
      return;
    }
    droidCastComponent = DaggerDroidCastComponent.builder()
        .droidCastModule(new DroidCastModule(application))
        .build();
  }

  /**
   * Return module that contains all references to injectable dependencies
   *
   * @return DroidCastComponent
   */
  public static DroidCastComponent getInstance() {
    return droidCastComponent;
  }
}
