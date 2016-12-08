package com.antonio.droidcast;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.antonio.droidcast.ioc.IOCProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;

/**
 * Created by antonio.carrasco on 04/12/2016.
 */

public class CopyFfserverConfAsyncTask extends AsyncTask<Void, Void, Boolean> {

  @Inject Context context;
  private final String TAG = this.getClass().getSimpleName();
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
  private static final int EOF = -1;

  public CopyFfserverConfAsyncTask() {
    IOCProvider.getInstance().inject(this);
  }

  @Override protected Boolean doInBackground(Void... params) {

    File filesDirectory = context.getFilesDir();
    InputStream is;
    try {
      is = context.getAssets().open("ffserver.conf");
      final FileOutputStream os = new FileOutputStream(new File(filesDirectory, "ffserver.conf"));
      byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

      int n;
      while (EOF != (n = is.read(buffer))) {
        os.write(buffer, 0, n);
      }

      close(os);
      close(is);
    } catch (IOException e) {
      Log.e(TAG, "[CopyFfserverConfAsyncTask] - error copying ffserver.conf to app directory");
      return false;
    }

    return true;
  }

  @Override protected void onPostExecute(Boolean copied) {
    super.onPostExecute(copied);

    if (!copied) {
      // TODO: ERROR
    }
  }

  private void close(InputStream inputStream) {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        // Do nothing
      }
    }
  }

  private void close(OutputStream outputStream) {
    if (outputStream != null) {
      try {
        outputStream.flush();
        outputStream.close();
      } catch (IOException e) {
        // Do nothing
      }
    }
  }
}
