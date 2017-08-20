package com.app.droidcast.utils.ioc;

import com.app.droidcast.DaoTest;
import com.app.droidcast.MediaShareActivityTest;
import com.app.droidcast.MetaDataProviderTest;
import com.app.droidcast.NotificationsTest;
import com.app.droidcast.SessionFinishActivityTest;
import com.app.droidcast.StreamingStartedActivityTest;
import com.app.droidcast.ioc.DroidCastComponent;
import com.app.droidcast.ioc.DroidCastModule;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Created by antonio.carrasco on 09/10/2016.
 */

@Component(modules = { DroidCastTestModule.class }) @Singleton
public interface DroidCastTestComponent extends DroidCastComponent {
  void inject(DaoTest daoTest);

  void inject(MediaShareActivityTest mediaShareActivityTest);

  void inject(StreamingStartedActivityTest streamingStartedActivityTest);

  void inject(SessionFinishActivityTest sessionFinishActivityTest);

  void inject(MetaDataProviderTest metaDataProviderTest);

  void inject(NotificationsTest notificationsTest);
}
