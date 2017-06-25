package com.app.droidcast.dao;

import android.content.SharedPreferences;
import com.app.droidcast.models.Model;

/**
 * Factory class that instantiate Dao's for storage
 */
public class DaoFactory {

  // Android SharedPreferences instance
  private SharedPreferences sharedPreferences;

  /**
   * Constructor
   *
   * @param sharedPreferences SharedPreferences object
   */
  public DaoFactory(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
  }

  public <T extends Model> Dao<T> get(Class<T> objectType) {
    return new Dao<>(objectType, sharedPreferences);
  }
}
