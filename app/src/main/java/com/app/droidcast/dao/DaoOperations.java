package com.app.droidcast.dao;

import com.app.droidcast.models.Model;
import java.util.List;

/**
 * Created by antonio.carrasco on 08/10/2016.
 */

public interface DaoOperations<T extends Model> {
  void persist(T object) throws DaoException;
  T get(String id) throws DaoException;
  T get() throws DaoException;
  List<T> getAll() throws DaoException;
  void clear() throws DaoException;
  void clear(String id) throws DaoException;
}
