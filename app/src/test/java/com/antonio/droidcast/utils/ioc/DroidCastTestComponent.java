package com.antonio.droidcast.utils.ioc;

import com.antonio.droidcast.DaoTest;
import com.antonio.droidcast.ioc.DroidCastComponent;
import com.antonio.droidcast.ioc.DroidCastModule;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Created by antonio.carrasco on 09/10/2016.
 */

@Component(modules = { DroidCastModule.class }) @Singleton public interface DroidCastTestComponent
    extends DroidCastComponent {
  void inject(DaoTest daoTest);
}
