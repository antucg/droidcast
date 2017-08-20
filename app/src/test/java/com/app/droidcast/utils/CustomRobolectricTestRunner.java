package com.app.droidcast.utils;

import com.app.droidcast.BuildConfig;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

/**
 * Created by antonio.carrasco on 17/08/2017.
 */

public class CustomRobolectricTestRunner extends RobolectricTestRunner {
  public CustomRobolectricTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
    String buildVariant = (BuildConfig.FLAVOR.isEmpty() ? "" : BuildConfig.FLAVOR + "/") + BuildConfig.BUILD_TYPE;
    System.setProperty("android.package", BuildConfig.APPLICATION_ID);
    System.setProperty("android.manifest", "build/intermediates/manifests/full/" + buildVariant + "/AndroidManifest.xml");
    System.setProperty("android.resources", "build/intermediates/res/" + buildVariant);
    System.setProperty("android.assets", "build/intermediates/assets/" + buildVariant);
  }
}
