package com.app.droidcast.utils.ioc;

import android.app.Application;
import com.app.droidcast.ioc.DroidCastComponent;
import com.app.droidcast.ioc.DroidCastModule;

/**
 * Class that builds the graph and holds the module that will inject dependencies.
 */
public class IOCProviderTest {

  /**
   * Module that contains all the provides
   */
  private static DroidCastTestComponent droidCastTestComponent;

  /**
   * Hidden constructor
   */
  private IOCProviderTest() {
  }

  /**
   * Build the graph with all the dependencies
   */
  public static void build(Application application) {
    if (droidCastTestComponent != null) {
      return;
    }
    droidCastTestComponent = DaggerDroidCastTestComponent.builder()
        .droidCastModule(new DroidCastModule(application))
        .build();
  }

  /**
   * Return module that contains all references to injectable dependencies
   *
   * @return DroidCastTestComponent
   */
  public static DroidCastTestComponent getInstance() {
    return droidCastTestComponent;
  }
}
