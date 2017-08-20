package com.app.droidcast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import com.app.droidcast.utils.CustomRobolectricTestRunner;
import com.app.droidcast.utils.DroidCastTestApp;
import com.app.droidcast.utils.NotificationUtils;
import com.app.droidcast.utils.ioc.IOCProviderTest;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNotificationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = DroidCastTestApp.class)
public class NotificationsTest {

  @Inject Context context;
  @Inject NotificationUtils notificationUtils;
  @Inject AudioManager audioManager;
  @Inject NotificationManager notificationManager;

  private ShadowNotificationManager shadowNotificationManager;

  @Before public void init() {
    IOCProviderTest.getInstance().inject(this);
    shadowNotificationManager = shadowOf(notificationManager);
  }

  @After public void clear() {
    notificationUtils.cancelStreamNotification();
  }

  @Test public void codeShouldBeRenderedInNotificacion() {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    notificationUtils.buildStreamNotification("123456");
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    Notification notification = shadowNotificationManager.getAllNotifications().get(0);
    assertThat(shadowOf(notification).getContentTitle()).endsWith("123456.");
  }

  @Test public void mutedMicShouldRenderNotification() {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    notificationUtils.buildStreamNotification("123456");
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    Notification notification = shadowNotificationManager.getAllNotifications().get(0);
    Notification.Action[] actions = notification.actions;
    assertThat(actions.length).isEqualTo(2);
    assertThat(actions[0].title).isEqualTo(context.getString(R.string.notification_stop));
    // Microphone should be muted
    assertThat(actions[1].title).isEqualTo(context.getString(R.string.enable_microphone));
  }

  @Test public void nonMutedMicShouldRenderNotification() {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    audioManager.setMicrophoneMute(false);
    notificationUtils.buildStreamNotification("123456");
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    Notification notification = shadowNotificationManager.getAllNotifications().get(0);
    Notification.Action[] actions = notification.actions;
    assertThat(actions.length).isEqualTo(2);
    // Microphone shouldn't be muted
    assertThat(actions[1].title).isEqualTo(context.getString(R.string.mute_micro));
  }

  @Test public void micButtonShouldTriggerMuteServiceIntent()
      throws PendingIntent.CanceledException {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity mediaShareActivity = mediaShareActivityActivityController.get();
    notificationUtils.buildStreamNotification("123456");
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    Notification notification = shadowNotificationManager.getAllNotifications().get(0);
    shadowOf(notification.actions[1].actionIntent).send();
    assertThat(ShadowApplication.getInstance()
        .getNextStartedService()
        .getComponent()
        .getClassName()).isEqualTo(MuteService.class.getName());
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void endButtonShouldTriggerMediaShareIntent()
      throws PendingIntent.CanceledException {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    notificationUtils.buildStreamNotification("123456");
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    Notification notification = shadowNotificationManager.getAllNotifications().get(0);
    shadowOf(notification.actions[0].actionIntent).send();
    assertThat(ShadowApplication.getInstance()
        .getNextStartedActivity()
        .getComponent()
        .getClassName()).isEqualTo(MediaShareActivity.class.getName());
  }

  @Test public void muteServiceShouldUpdateNotification() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity mediaShareActivity = mediaShareActivityActivityController.get();
    notificationUtils.buildStreamNotification("123456");
    Notification notification =
        shadowNotificationManager.getNotification(NotificationUtils.STREAM_NOTIFICATION_ID);
    Notification.Action[] actions = notification.actions;
    // Microphone should be muted
    assertThat(actions[1].title).isEqualTo(context.getString(R.string.enable_microphone));
    Intent intent = MuteService.createIntent(context);
    ServiceController<MuteService> muteServiceServiceController =
        Robolectric.buildService(MuteService.class).create();
    MuteService service = muteServiceServiceController.get();
    service.onHandleIntent(intent);
    notification =
        shadowNotificationManager.getNotification(NotificationUtils.STREAM_NOTIFICATION_ID);
    actions = notification.actions;
    // Microphone shouldn't be muted
    assertThat(actions[1].title).isEqualTo(context.getString(R.string.mute_micro));
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void cancelStreamNotificationShouldClearStatusBar() {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    notificationUtils.buildStreamNotification("123456");
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    notificationUtils.cancelStreamNotification();
    assertThat(shadowNotificationManager.size()).isEqualTo(0);
  }

  @Test public void firstConnectionShouldNotShowNewConnectionNotification() {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    mediaShareActivity.onSessionStarted();
    assertThat(shadowNotificationManager.size()).isEqualTo(1);
    Notification notification =
        shadowNotificationManager.getNotification(NotificationUtils.STREAM_NOTIFICATION_ID);
    assertThat(shadowOf(notification).getContentText()).endsWith("1.");
  }

  @Test public void secondConnectionShouldShowNewConnectionNotification() {
    MediaShareActivity mediaShareActivity = Robolectric.setupActivity(MediaShareActivity.class);
    mediaShareActivity.onSessionStarted();
    mediaShareActivity.onSessionStarted();
    Notification notification =
        shadowNotificationManager.getNotification(NotificationUtils.STREAM_NOTIFICATION_ID);
    assertThat(shadowOf(notification).getContentText()).endsWith("2.");
    assertThat(shadowNotificationManager.size()).isEqualTo(2);
  }
}
