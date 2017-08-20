package com.app.droidcast;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.TextView;
import com.app.droidcast.utils.CustomRobolectricTestRunner;
import com.app.droidcast.utils.DroidCastTestApp;
import com.app.droidcast.utils.NsdUtils;
import com.app.droidcast.utils.Utils;
import com.app.droidcast.utils.ioc.IOCProviderTest;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = DroidCastTestApp.class)
public class MediaShareActivityTest {

  @Inject NsdUtils nsdUtils;
  @Inject Context context;
  @Inject AudioManager audioManager;

  @Before public void init() {
    IOCProviderTest.getInstance().inject(this);
  }

  @Test public void microphoneShouldBeMutedByDefault() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    assertThat(audioManager.isMicrophoneMute()).isTrue();
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void shareCodeShouldNotBeEmpty() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity activity = mediaShareActivityActivityController.get();
    assertThat(((TextView) activity.findViewById(R.id.media_share_code_textview)).getText()
        .length()).isEqualTo(6);
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void shareLinkForPCShouldTriggerIntent() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity activity = mediaShareActivityActivityController.get();
    activity.findViewById(R.id.media_share_pc_link_button).performClick();
    Intent actionChooserIntent = shadowOf(activity).getNextStartedActivity();
    Intent intent = actionChooserIntent.getParcelableExtra(Intent.EXTRA_INTENT);
    String text = intent.getStringExtra(Intent.EXTRA_TEXT);
    String code =
        ((TextView) activity.findViewById(R.id.media_share_code_textview)).getText().toString();
    assertThat(text).endsWith("rtsp://"
        + MediaShareActivity.USERNAME
        + ":"
        + code
        + "@"
        + Utils.getIPAddress(true)
        + ":"
        + nsdUtils.getAvailablePort());
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void shareLinkForAppShouldTriggerIntent() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity activity = mediaShareActivityActivityController.get();
    activity.findViewById(R.id.media_share_app_link_button).performClick();
    Intent actionChooserIntent = shadowOf(activity).getNextStartedActivity();
    Intent intent = actionChooserIntent.getParcelableExtra(Intent.EXTRA_INTENT);
    String text = intent.getStringExtra(Intent.EXTRA_TEXT);
    String code =
        ((TextView) activity.findViewById(R.id.media_share_code_textview)).getText().toString();
    assertThat(text).endsWith("http://app.droidcast.com/?code=" + code);
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void stopIntentShouldStartSessionFinish() {
    Intent stopIntent = MediaShareActivity.createStopIntent(context);
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity activity = mediaShareActivityActivityController.get();
    activity.onNewIntent(stopIntent);
    Intent intent = shadowOf(activity).getNextStartedActivity();
    assertThat(intent.getComponent().getClassName()).isEqualTo(
        SessionFinishActivity.class.getName());
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void firstConnectionShouldOpenStreamingStartedActivity() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    MediaShareActivity activity = mediaShareActivityActivityController.get();
    activity.onSessionStarted();
    assertThat(shadowOf(activity).getNextStartedActivity().getComponent().getClassName()).isEqualTo(
        StreamingStartedActivity.class.getName());
    mediaShareActivityActivityController.pause().stop().destroy();
  }

  @Test public void stopServerShouldUnMuteMic() {
    ActivityController<MediaShareActivity> mediaShareActivityActivityController =
        Robolectric.buildActivity(MediaShareActivity.class).create().start().resume().visible();
    assertThat(audioManager.isMicrophoneMute()).isTrue();
    mediaShareActivityActivityController.pause().stop().destroy();
    assertThat(audioManager.isMicrophoneMute()).isFalse();
  }
}
