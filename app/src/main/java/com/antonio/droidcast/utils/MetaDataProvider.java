package com.antonio.droidcast.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Utility class that reads values from manifest's meta data.
 */
public class MetaDataProvider {

  // Tag for logs
  private final String TAG = this.getClass().getSimpleName();

  /**
   * Method that returns the the cast application id.
   *
   * @param activity Activity that wants to access the id.
   * @return Google Cast application id.
   */
  public String getCastAppId(Activity activity) {
    return getMetaData(activity, "castAppId");
  }

  /**
   * Method that returns a property from the manifest's meta data.
   *
   * @param activity Activity that wants to access to the data.
   * @param property Property whose value we want to read.
   * @return Value.
   */
  private String getMetaData(Activity activity, String property) {
    ActivityInfo activityInfo = null;
    try {
      activityInfo = activity.getPackageManager()
          .getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA);

      Bundle metaData = activityInfo.metaData;
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
