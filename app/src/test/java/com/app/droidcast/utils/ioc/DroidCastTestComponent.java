package com.app.droidcast.utils.ioc;

import com.app.droidcast.DaoTest;
import com.app.droidcast.ioc.DroidCastComponent;
import com.app.droidcast.ioc.DroidCastModule;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Created by antonio.carrasco on 09/10/2016.
 */

@Component(modules = { DroidCastModule.class }) @Singleton public interface DroidCastTestComponent
    extends DroidCastComponent {
  void inject(DaoTest daoTest);
}
