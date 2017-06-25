package com.app.droidcast.models;

import lombok.Getter;
import lombok.Setter;

/**
 * Model that contains information about a resource that has been casted.
 */

public class CastItem {
  @Getter @Setter private String label;
  @Getter @Setter private String type;
}
