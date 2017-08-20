package com.app.droidcast;

import android.content.Context;
import android.media.AudioManager;
import android.widget.Switch;
import com.app.droidcast.ioc.IOCProvider;
import com.app.droidcast.utils.CustomRobolectricTestRunner;
import com.app.droidcast.utils.DroidCastTestApp;
import com.app.droidcast.utils.ioc.IOCProviderTest;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = DroidCastTestApp.class)
public class StreamingStartedActivityTest {

  @Inject Context context;
  @Inject AudioManager audioManager;

  @Before public void init() {
    IOCProviderTest.getInstance().inject(this);
  }

  @Test public void switchOffWhenMicrophoneMuted() {
    audioManager.setMicrophoneMute(true);
    ActivityController<StreamingStartedActivity> streamingStartedActivityActivityController =
        Robolectric.buildActivity(StreamingStartedActivity.class)
            .create()
            .start()
            .resume()
            .visible();
    StreamingStartedActivity streamingStartedActivity =
        streamingStartedActivityActivityController.get();
    Switch switchButton =
        (Switch) streamingStartedActivity.findViewById(R.id.enable_microphone_switch);
    assertThat(switchButton.isChecked()).isFalse();
    streamingStartedActivityActivityController.pause().stop().destroy();
  }

  @Test public void switchOnWhenMicrophoneNotMuted() {
    audioManager.setMicrophoneMute(false);
    ActivityController<StreamingStartedActivity> streamingStartedActivityActivityController =
        Robolectric.buildActivity(StreamingStartedActivity.class)
            .create()
            .start()
            .resume()
            .visible();
    StreamingStartedActivity streamingStartedActivity =
        streamingStartedActivityActivityController.get();
    Switch switchButton =
        (Switch) streamingStartedActivity.findViewById(R.id.enable_microphone_switch);
    assertThat(switchButton.isChecked()).isTrue();
    streamingStartedActivityActivityController.pause().stop().destroy();
  }
}
