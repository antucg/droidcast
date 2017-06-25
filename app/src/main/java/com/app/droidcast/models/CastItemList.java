package com.app.droidcast.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Object that contains the list items that has been casted.
 */
public class CastItemList implements Model {

  @Getter @Setter private List<CastItem> castItemList = new ArrayList<>();

  /**
   * Constructor. Populate the list with an empty item.
   */
  public CastItemList() {
    push(new CastItem());
  }

  /**
   * Push a new item into the end of the list.
   *
   * @param castItem Item to push
   */
  public void push(CastItem castItem) {
    castItemList.add(castItem);
  }

  /**
   * Return the number of items within the list.
   *
   * @return Amount of items.
   */
  public int size() {
    return castItemList.size();
  }

  /**
   * Returns whether the list is empty or not.
   *
   * @return True if list is empty, false otherwise.
   */
  public boolean isEmpty() {
    return castItemList.isEmpty();
  }

  @Override public String getId() {
    return id;
  }
}
