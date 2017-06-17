package com.antonio.droidcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.utils.NotificationUtils;
import javax.inject.Inject;

/**
 * Service that toggles the microphone.
 */
public class MuteService extends IntentService {
  private static final String ACTION_MUTE = "com.antonio.droidcast.action.TOGGLE_MUTE_MICRO";

  @Inject NotificationUtils notificationUtils;

  public MuteService() {
    super("MuteService");
    IOCProvider.getInstance().inject(this);
  }

  /**
   * Create an intent that starts this service
   *
   * @param context Application context
   * @return Intent
   */
  public static Intent createIntent(Context context) {
    Intent intent = new Intent(context, MuteService.class);
    intent.setAction(ACTION_MUTE);
    return intent;
  }

  @Override protected void onHandleIntent(Intent intent) {
    if (intent != null) {
      final String action = intent.getAction();
      if (ACTION_MUTE.equals(action)) {
        toggleMute();
      }
    }
  }

  /**
   * Toggle microphone.
   */
  private void toggleMute() {
    AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    if (!audioManager.isMicrophoneMute()) {
      audioManager.setMicrophoneMute(true);
    } else {
      audioManager.setMicrophoneMute(false);
    }
    notificationUtils.updateStreamNotification();
  }
}
