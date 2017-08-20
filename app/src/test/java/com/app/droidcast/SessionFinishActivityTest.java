package com.app.droidcast;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
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
import static org.robolectric.Shadows.shadowOf;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = DroidCastTestApp.class)
public class SessionFinishActivityTest {

  @Inject Context context;

  @Before public void init() {
    IOCProviderTest.getInstance().inject(this);
  }

  @Test public void finishByServerShouldDisplayProperText() {
    Intent intent = SessionFinishActivity.createIntent(context, true);
    ActivityController<SessionFinishActivity> sessionFinishActivityActivityController =
        Robolectric.buildActivity(SessionFinishActivity.class, intent)
            .create()
            .start()
            .resume()
            .visible();
    SessionFinishActivity sessionFinishActivity = sessionFinishActivityActivityController.get();
    assertThat(((TextView) sessionFinishActivity.findViewById(
        R.id.session_finish_textview)).getText()).isEqualTo(
        context.getString(R.string.session_finished_by_server));
    sessionFinishActivityActivityController.pause().stop().destroy();
  }

  @Test public void notFinishByServerShouldDisplayProperText() {
    Intent intent = SessionFinishActivity.createIntent(context, false);
    ActivityController<SessionFinishActivity> sessionFinishActivityActivityController =
        Robolectric.buildActivity(SessionFinishActivity.class, intent)
            .create()
            .start()
            .resume()
            .visible();
    SessionFinishActivity sessionFinishActivity = sessionFinishActivityActivityController.get();
    assertThat(((TextView) sessionFinishActivity.findViewById(
        R.id.session_finish_textview)).getText()).isEqualTo(
        context.getString(R.string.session_finished));
    sessionFinishActivityActivityController.pause().stop().destroy();
  }

  @Test public void closeButtonShouldOpenHomeActivity() {
    ActivityController<SessionFinishActivity> sessionFinishActivityActivityController =
        Robolectric.buildActivity(SessionFinishActivity.class).create().start().resume().visible();
    SessionFinishActivity sessionFinishActivity = sessionFinishActivityActivityController.get();
    sessionFinishActivity.findViewById(R.id.session_finish_close_button).performClick();
    assertThat(shadowOf(sessionFinishActivity).getNextStartedActivity()
        .getComponent()
        .getClassName()).isEqualTo(HomeActivity.class.getName());
    sessionFinishActivityActivityController.pause().stop().destroy();
  }
}
