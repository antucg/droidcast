package com.app.droidcast.utils;

import android.app.Notification;
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
 * Utility class to handle app notifications.
 */
public class NotificationUtils {
  private static final int STREAM_NOTIFICATION_ID = 1;
  private static final int NEW_CONNECTION_ID = 2;
  private Context context;
  private NotificationManager notificationManager;
  private String currentCode;
  private int clientsCount = 0;

  public NotificationUtils(Context context) {
    this.context = context;
    notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  /**
   * Build and display stream notification in the status bar.
   *
   * @param code Code of the current session.
   */
  public void buildStreamNotification(String code) {
    currentCode = code;
    buildAndShowNotification();
  }

  /**
   * Update stream notification.
   */
  public void updateStreamNotification() {
    buildAndShowNotification();
  }

  /**
   * Update stream notification with the provided number of clients.
   *
   * @param clientsCount Number of connected clients.
   */
  public void updateStreamNotification(int clientsCount) {
    this.clientsCount = clientsCount;
    buildAndShowNotification();
  }

  /**
   * Build the notification for the current session. Will show whether microphone
   * is ON or OFF and number of connected clients.
   */
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
    int muteIcon = audioManager.isMicrophoneMute() ? R.mipmap.mic_off : R.mipmap.mic_on;

    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification_bar_icon)
            .setContentTitle(context.getString(R.string.manage_streaming, currentCode))
            .setContentText(context.getString(R.string.streaming_clients, clientsCount))
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.mipmap.ic_media_stop, context.getString(R.string.notification_stop),
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

  /**
   * Show a notification to notify a user of a new incoming connection.
   */
  public void newConnection() {
    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification_bar_icon)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("New connection")
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true);

    notificationManager.notify(NEW_CONNECTION_ID, mBuilder.build());
  }

  /**
   * Cancel notifications from the status bar.
   */
  public void cancelStreamNotification() {
    notificationManager.cancel(STREAM_NOTIFICATION_ID);
    notificationManager.cancel(NEW_CONNECTION_ID);
  }
}
