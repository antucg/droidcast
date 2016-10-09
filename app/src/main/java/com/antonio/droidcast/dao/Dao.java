package com.antonio.droidcast.dao;

import android.content.SharedPreferences;
import com.antonio.droidcast.models.Model;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by antonio.carrasco on 08/10/2016.
 */

public class Dao<T extends Model> implements DaoOperations {

  private SharedPreferences sharedPreferences;

  // Model class
  private Class<T> objectClass;
  private String className;

  /**
   * Constructor.
   *
   * @param objectClass Class of the model to persist.
   */
  public Dao(Class<T> objectClass, SharedPreferences sharedPreferences) {
    this.objectClass = objectClass;
    this.sharedPreferences = sharedPreferences;
    className = this.objectClass.getSimpleName();
  }

  /**
   * Persist an object into the storage.
   *
   * @param object Object to persist.
   */
  @Override public void persist(Model object) throws DaoException {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    String key = className + "|" + object.getId();
    String json = new Gson().toJson(object);
    editor.putString(key, json);
    boolean result = editor.commit();
    if (!result) {
      throw new DaoException("Object could not be persisted");
    }
  }

  /**
   * Get the object whose identifier is equal to id.
   *
   * @param id Id of the object to get.
   * @return Object of type T.
   * @throws DaoException Thrown if not found.
   */
  @Override public T get(String id) throws DaoException {
    String key = className + "|" + id;
    String value = sharedPreferences.getString(key, null);
    if (value == null) {
      throw new DaoException("Object of type: " + className + " couldn't be found");
    }

    return new Gson().fromJson(value, objectClass);
  }

  /**
   * Get single object of type T from the storage.
   *
   * @return Object of type T.
   * @throws DaoException Thrown if not found.
   */
  @Override public T get() throws DaoException {
    return get(Model.id);
  }

  /**
   * Return a list of all the objects of type T.
   *
   * @return List of objects of type T.
   * @throws DaoException If no object was found.
   */
  @Override public List<T> getAll() throws DaoException {

    // Get all the objects stored in the preferences
    Map<String, ?> preferences = sharedPreferences.getAll();
    List<String> keys = new ArrayList<>();

    // Check what objects are of type T by looking for className within the key
    for (String key : preferences.keySet()) {
      if (key.contains(className)) {
        keys.add(key);
      }
    }

    // If there are no keys, then there is nothing to remove
    int keysLength = keys.size();
    if (keysLength == 0) {
      throw new DaoException("Object of type: " + className + " couldn't be found");
    }

    List<T> listOfObjects = new ArrayList<>();
    String value;
    for (int i = 0; i < keysLength; ++i) {
      value = sharedPreferences.getString(keys.get(i), null);
      listOfObjects.add(new Gson().fromJson(value, objectClass));
    }
    return listOfObjects;
  }

  /**
   * Clear all the objects of type T from the storage.
   */
  @Override public void clear() throws DaoException {

    // Get all the objects stored in the preferences
    Map<String, ?> preferences = sharedPreferences.getAll();
    List<String> keys = new ArrayList<>();

    // Check what objects are of type T by looking for className within the key
    for (String key : preferences.keySet()) {
      if (key.contains(className)) {
        keys.add(key);
      }
    }

    // If there are no keys, then there is nothing to remove
    int keysLength = keys.size();
    if (keysLength == 0) {
      return;
    }

    // Otherwise remove all those objects
    SharedPreferences.Editor editor = sharedPreferences.edit();
    for (int i = 0; i < keysLength; ++i) {
      editor.remove(keys.get(i));
    }
    boolean result = editor.commit();
    if (!result) {
      throw new DaoException("Objects could not be cleared");
    }
  }

  /**
   * Clear object of type T from storage whose identifier matches id.
   *
   * @param id Id of the object to remove
   */
  @Override public void clear(String id) throws DaoException {
    String key = className + "|" + id;
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.remove(key);
    boolean result = editor.commit();
    if (!result) {
      throw new DaoException("Object could not be cleared");
    }
  }
}
