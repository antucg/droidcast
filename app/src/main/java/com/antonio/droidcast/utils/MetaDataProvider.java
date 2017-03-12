package com.antonio.droidcast.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.antonio.droidcast.ioc.IOCProvider;
import javax.inject.Inject;

/**
 * Utility class that reads values from manifest's meta data.
 */
public class MetaDataProvider {

  @Inject Context context;

  // Tag for logs
  private final String TAG = this.getClass().getSimpleName();

  public MetaDataProvider() {
    IOCProvider.getInstance().inject(this);
  }

  /**
   * Return Nsd "private" key
   * @return Nsd key
   */
  public String getNsdKey() {
    return getMetaData("nsdKey");
  }

  /**
   * Method that returns a property from the manifest's meta data.
   *
   * @param property Property whose value we want to read.
   * @return Value.
   */
  private String getMetaData(String property) {
    ApplicationInfo applicationInfo = null;
    try {
      applicationInfo = context.getPackageManager()
          .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

      Bundle metaData = applicationInfo.metaData;
      if (metaData == null) {
        Log.e(TAG, "[MetaDataProvider] - getMetadata(), error reading metadata from manifest.");
      } else {
        return (String) metaData.get(property);
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "[MetaDataProvider] - getMetadata(), error accessing activity info");
    }

    return null;
  }
}
