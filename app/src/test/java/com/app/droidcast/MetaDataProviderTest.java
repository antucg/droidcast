package com.app.droidcast;

import com.app.droidcast.utils.CustomRobolectricTestRunner;
import com.app.droidcast.utils.DroidCastTestApp;
import com.app.droidcast.utils.MetaDataProvider;
import com.app.droidcast.utils.ioc.IOCProviderTest;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = DroidCastTestApp.class)
public class MetaDataProviderTest {

  @Inject MetaDataProvider metaDataProvider;

  @Before public void init() {
    IOCProviderTest.getInstance().inject(this);
  }

  @Test public void correctKeyShouldBeReturned() {
    assertThat(metaDataProvider.getNsdKey()).isNotEmpty();
  }
}
