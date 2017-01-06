package com.github.hiteshsondhi88.libffmpeg;

import android.content.Context;
import android.os.AsyncTask;
import java.io.File;

class FFServerLoadLibraryAsyncTask extends AsyncTask<Void, Void, Boolean> {

  private final String cpuArchNameFromAssets;
  private final FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler;
  private final Context context;

  FFServerLoadLibraryAsyncTask(Context context, String cpuArchNameFromAssets,
      FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler) {
    this.context = context;
    this.cpuArchNameFromAssets = cpuArchNameFromAssets;
    this.ffmpegLoadBinaryResponseHandler = ffmpegLoadBinaryResponseHandler;
  }

  @Override protected Boolean doInBackground(Void... params) {
    File ffserverFile = new File(FileUtils.getFFServer(context));
    if (ffserverFile.exists() && isDeviceFFServerVersionOld() && !ffserverFile.delete()) {
      return false;
    }
    if (!ffserverFile.exists()) {
      boolean isFileCopied = FileUtils.copyBinaryFromAssetsToData(context,
          cpuArchNameFromAssets + File.separator + FileUtils.ffserverFileName,
          FileUtils.ffserverFileName);

      // make file executable
      if (isFileCopied) {
        if (!ffserverFile.canExecute()) {
          Log.d("FFServer is not executable, trying to make it executable ...");
          if (ffserverFile.setExecutable(true)) {
            return true;
          }
        } else {
          Log.d("FFServer is executable");
          return true;
        }
      }
    }
    return ffserverFile.exists() && ffserverFile.canExecute();
  }

  @Override protected void onPostExecute(Boolean isSuccess) {
    super.onPostExecute(isSuccess);
    if (ffmpegLoadBinaryResponseHandler != null) {
      if (isSuccess) {
        ffmpegLoadBinaryResponseHandler.onSuccess();
      } else {
        ffmpegLoadBinaryResponseHandler.onFailure();
      }
      ffmpegLoadBinaryResponseHandler.onFinish();
    }
  }

  private boolean isDeviceFFServerVersionOld() {
    return CpuArch.fromString(FileUtils.SHA1(FileUtils.getFFServer(context))).equals(CpuArch.NONE);
  }
}
