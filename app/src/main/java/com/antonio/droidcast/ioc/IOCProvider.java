package com.antonio.droidcast.ioc;

import com.antonio.droidcast.DroidCastApp;

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
  public static void build() {
    if (droidCastComponent != null) {
      return;
    }
    droidCastComponent = DaggerDroidCastComponent.builder().build();
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
