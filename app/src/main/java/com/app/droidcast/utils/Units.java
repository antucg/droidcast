package com.app.droidcast.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Created by antonio.carrasco on 01/05/2017.
 */

public class Units {
  /**
   * Convert dp to pixel.
   *
   * @param context Application context.
   * @param dp DP to convert.
   * @return DP in pixel.
   */
  public static float dpToPixel(Context context, int dp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
  }
}
