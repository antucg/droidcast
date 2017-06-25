package com.app.droidcast.dao;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Factory class that instantiates SharedPreference object
 */
public class SharedPreferencesFactory {

  private static final String SHARED_PREFERENCES_NAME = "droidCastSharedPreferences";

  public static SharedPreferences getSharedPreferences(Context context) {
    return context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
  }
}
