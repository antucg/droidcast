package com.app.droidcast.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.app.droidcast.HomeActivity;
import com.app.droidcast.MediaShareActivity;
import com.app.droidcast.MuteService;
import com.app.droidcast.R;

/**
 * Created by antonio.carrasco on 15/06/2017.
 */

public class NotificationUtils {
  private static final int STREAM_NOTIFICATION_ID = 1;
  private Context context;
  private NotificationManager notificationManager;
  private String currentCode;

  public NotificationUtils(Context context) {
    this.context = context;
    notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public void buildStreamNotification(String code) {
    currentCode = code;
    buildAndShowNotification();
  }

  public void updateStreamNotification() {
    buildAndShowNotification();
  }

  private void buildAndShowNotification() {
    Intent stopIntent = MediaShareActivity.createStopIntent(context);
    PendingIntent stopPendingIntent =
        PendingIntent.getActivity(context, (int) System.currentTimeMillis(), stopIntent,
            PendingIntent.FLAG_ONE_SHOT);

    Intent muteIntent = MuteService.createIntent(context);
    PendingIntent mutePendingIntent =
        PendingIntent.getService(context, (int) System.currentTimeMillis(), muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);

    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    int muteIcon = audioManager.isMicrophoneMute() ? R.drawable.ic_voice_search_api_holo_dark
        : R.drawable.ic_voice_search_api_holo_light;

    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification_bar_icon)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.manage_streaming, currentCode))
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_media_stop, context.getString(R.string.notification_stop),
                stopPendingIntent)
            .addAction(muteIcon, context.getString(R.string.mute_micro), mutePendingIntent);
    Intent resultIntent = MediaShareActivity.createIntent(context);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(HomeActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    notificationManager.notify(STREAM_NOTIFICATION_ID, mBuilder.build());
  }

  public void cancelStreamNotification() {
    notificationManager.cancel(STREAM_NOTIFICATION_ID);
  }
}
