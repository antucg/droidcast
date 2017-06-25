package com.app.droidcast.utils;

import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Utility class that gets the real dimension of a given view.
 */
public class ViewDimensions {

  /**
   * Function that gets the dimension of a given view and notifies it through the passed callback.
   *
   * @param view View whose dimension we want to obtain.
   * @param callback Callback that will receive width and height of the view.
   */
  public static void getDimension(final View view, final OnDimensionReady callback) {
    final ViewTreeObserver vto = view.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override public void onGlobalLayout() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        } else {
          view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();

        if (callback != null) {
          callback.onDimension(width, height);
        }
      }
    });
  }

  public interface OnDimensionReady {
    void onDimension(int width, int heigth);
  }
}
