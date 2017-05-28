package com.antonio.droidcast.ioc;

import com.antonio.droidcast.DroidCastApp;
import com.antonio.droidcast.HomeActivity;
import com.antonio.droidcast.MediaShareActivity;
import com.antonio.droidcast.VideoActivityVLC;
import com.antonio.droidcast.utils.MetaDataProvider;
import com.antonio.droidcast.utils.NsdUtils;
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
}
