package net.majorkernelpanic.streaming.screen;

import android.content.Context;
import android.view.OrientationEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonio.carrasco on 03/06/2017.
 */

public class OrientantionListener {
  private Context context;
  private OrientationEventListener orientationEventListener;
  private List<OnOrientationChange> callbackList = new ArrayList<>();
  private int currentOrientation;
  private static volatile OrientantionListener instance = null;
  private static final Object mutex = new Object();

  private OrientantionListener(final Context context) {
    currentOrientation = context.getResources().getConfiguration().orientation;
    orientationEventListener = new OrientationEventListener(context) {
      @Override public void onOrientationChanged(int orientation) {
        int newOrientation = context.getResources().getConfiguration().orientation;
        if (currentOrientation != newOrientation) {
          currentOrientation = newOrientation;
          if (!callbackList.isEmpty()) {
            for (OnOrientationChange cb : callbackList) {
              cb.newOrientation(newOrientation);
            }
          }
        }
      }
    };
  }

  public static OrientantionListener getInstance(Context context) {
    if (instance == null) {
      synchronized (mutex) {
        if (instance == null) {
          instance = new OrientantionListener(context);
        }
      }
    }
    return instance;
  }

  public void addListener(OnOrientationChange cb) {
    if (callbackList.isEmpty()) {
      orientationEventListener.enable();
    }
    callbackList.add(cb);
  }

  public void removeListener(OnOrientationChange cb) {
    callbackList.remove(cb);
    if (callbackList.isEmpty()) {
      orientationEventListener.disable();
    }
  }

  interface OnOrientationChange {
    void newOrientation(int orientation);
  }
}
