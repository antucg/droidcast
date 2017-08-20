package com.app.droidcast.ioc;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import com.app.droidcast.dao.DaoFactory;
import com.app.droidcast.dao.SharedPreferencesFactory;
import com.app.droidcast.utils.MetaDataProvider;
import com.app.droidcast.utils.NotificationUtils;
import com.app.droidcast.utils.NsdUtils;
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

  @Provides @Singleton NsdUtils providesNsdUtils() {
    return new NsdUtils();
  }

  @Provides @Singleton NotificationUtils providesNotificationUtils() {
    return new NotificationUtils();
  }

  @Provides @Singleton AudioManager providesAudioManager() {
    return (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);
  }

  @Provides @Singleton NotificationManager providesNotificationManager() {
    return (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }
}
