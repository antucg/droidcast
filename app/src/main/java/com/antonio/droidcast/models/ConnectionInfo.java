package com.antonio.droidcast.models;

import java.net.InetAddress;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by antonio.carrasco on 18/02/2017.
 */

public class ConnectionInfo implements Model {
  @Getter @Setter InetAddress host;
  @Getter @Setter int port;

  public ConnectionInfo(InetAddress host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override public String getId() {
    return id;
  }

  @Override public String toString() {
    return "ConnectionInfo{" +
        "host=" + host +
        ", port=" + port +
        '}';
  }
}
