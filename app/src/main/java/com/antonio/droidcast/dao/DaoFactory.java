package com.antonio.droidcast.dao;

import android.content.SharedPreferences;
import com.antonio.droidcast.models.Model;

/**
 * Factory class that instantiate Dao's for storage
 */
public class DaoFactory<T extends Model> {

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

  public Dao<T> get(Class<T> objectType) {
    return new Dao<>(objectType, sharedPreferences);
  }
}
