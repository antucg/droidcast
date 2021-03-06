package com.antonio.droidcast.ioc;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.antonio.droidcast.dao.DaoFactory;
import com.antonio.droidcast.dao.SharedPreferencesFactory;
import com.antonio.droidcast.utils.MetaDataProvider;
import com.github.hiteshsondhi88.libffmpeg.FFServer;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Module that contains all the providers for injectable dependencies.
 */

@Module public class DroidCastModule {

  private Context applicationContext;

  public DroidCastModule(Application applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Provides @Singleton Context providesContext() {
    return applicationContext;
  }

  @Provides @Singleton SharedPreferences providesSharedPreferences() {
    return SharedPreferencesFactory.getSharedPreferences(applicationContext);
  }

  @Provides @Singleton DaoFactory providesDaoFactory() {
    SharedPreferences sharedPreferences =
        SharedPreferencesFactory.getSharedPreferences(applicationContext);
    return new DaoFactory(sharedPreferences);
  }

  @Provides @Singleton MetaDataProvider providesMetaData() {
    return new MetaDataProvider();
  }

  @Provides @Singleton FFmpeg providesFFmpeg() {
    return FFmpeg.getInstance(applicationContext);
  }

  @Provides @Singleton FFServer providesFFServer() {
    return FFServer.getInstance(applicationContext);
  }
}
