package com.antonio.droidcast.ioc;

import com.antonio.droidcast.CopyFfserverConfAsyncTask;
import com.antonio.droidcast.DroidCastApp;
import com.antonio.droidcast.HomeActivity;
import com.antonio.droidcast.MediaShareActivity;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Created by antonio.carrasco on 08/10/2016.
 */
@Component(modules = { DroidCastModule.class }) @Singleton public interface DroidCastComponent {
  void inject(HomeActivity homeActivity);

  void inject(MediaShareActivity mediaShareActivity);

  void inject(DroidCastApp droidCastApp);

  void inject(CopyFfserverConfAsyncTask copyFfserverConfAsyncTask);
}
