package com.app.droidcast.ioc;

import com.app.droidcast.DroidCastApp;
import com.app.droidcast.HomeActivity;
import com.app.droidcast.MediaShareActivity;
import com.app.droidcast.MuteService;
import com.app.droidcast.StreamingStartedActivity;
import com.app.droidcast.VideoActivityVLC;
import com.app.droidcast.utils.MetaDataProvider;
import com.app.droidcast.utils.NotificationUtils;
import com.app.droidcast.utils.NsdUtils;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Created by antonio.carrasco on 08/10/2016.
 */
@Component(modules = { DroidCastModule.class }) @Singleton public interface DroidCastComponent {
  void inject(HomeActivity homeActivity);

  void inject(MediaShareActivity mediaShareActivity);

  void inject(DroidCastApp droidCastApp);

  void inject(NsdUtils nsdUtils);

  void inject(MetaDataProvider metaDataProvider);

  void inject(VideoActivityVLC videoActivityVLC);

  void inject(MuteService muteService);

  void inject(NotificationUtils notificationUtils);

  void inject(StreamingStartedActivity streamingStartedActivity);
}
