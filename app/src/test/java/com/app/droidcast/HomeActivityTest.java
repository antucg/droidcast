package com.app.droidcast;

import android.widget.EditText;
import com.app.droidcast.utils.CustomRobolectricTestRunner;
import com.app.droidcast.utils.DroidCastTestApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = DroidCastTestApp.class)
public class HomeActivityTest {

  @Test public void videoActivityShouldntOpenWithEmptyCode() {
    ActivityController<HomeActivity> homeActivityActivityController =
        Robolectric.buildActivity(HomeActivity.class).create().start().resume().visible();
    HomeActivity activity = homeActivityActivityController.get();
    activity.findViewById(R.id.home_connect_button).performClick();
    assertThat(shadowOf(activity).getNextStartedActivity()).isNull();
    homeActivityActivityController.pause().stop().destroy();
  }

  @Test public void videoActivityShouldOpenWhenCorrectCode() {
    String code = "123456";
    ActivityController<HomeActivity> homeActivityActivityController =
        Robolectric.buildActivity(HomeActivity.class).create().start().resume().visible();
    HomeActivity activity = homeActivityActivityController.get();
    ((EditText) activity.findViewById(R.id.home_code_editText)).setText(code);
    activity.findViewById(R.id.home_connect_button).performClick();
    assertThat(shadowOf(activity).getNextStartedActivity().getComponent().getClassName()).isEqualTo(
        VideoActivityVLC.class.getName());
    homeActivityActivityController.pause().stop().destroy();
  }

  @Test public void streamButtonShouldOpenMediaShareActivity() {
    ActivityController<HomeActivity> homeActivityActivityController =
        Robolectric.buildActivity(HomeActivity.class).create().start().resume().visible();
    HomeActivity activity = homeActivityActivityController.get();
    activity.findViewById(R.id.home_stream_button).performClick();
    assertThat(shadowOf(activity).getNextStartedActivity().getComponent().getClassName()).isEqualTo(
        MediaShareActivity.class.getName());
    homeActivityActivityController.pause().stop().destroy();
  }
}
