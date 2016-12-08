package com.antonio.droidcast;

import android.app.Application;
import com.antonio.droidcast.ioc.DroidCastComponent;
import com.antonio.droidcast.ioc.IOCProvider;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import javax.inject.Inject;

/**
 * Main application class
 */
public class DroidCastApp extends Application {

  private static DroidCastComponent droidCastComponent;
  @Inject FFmpeg ffmpeg;

  @Override public void onCreate() {
    super.onCreate();

    // Init dagger component
    IOCProvider.build(this);
    IOCProvider.getInstance().inject(this);

    new CopyFfserverConfAsyncTask().execute();

    try {
      ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

        @Override public void onStart() {
        }

        @Override public void onFailure() {
        }

        @Override public void onSuccess() {
        }

        @Override public void onFinish() {
        }
      });
    } catch (FFmpegNotSupportedException e) {
      // Handle if FFmpeg is not supported by device
      System.out.println("Error loading ffmpeg");
    }
  }

  /**
   * Return component for injections
   *
   * @return DroidCastComponent instance
   */
  public static DroidCastComponent getComponent() {
    return droidCastComponent;
  }
}
