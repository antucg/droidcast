package com.app.droidcast.rtspclient.RtspClinet.Audio;

import com.app.droidcast.rtspclient.RtspClinet.Stream.RtpStream;

/**
 *
 */
public abstract class AudioStream extends RtpStream {
  private final static String tag = "AudioStream";

  protected void recombinePacket(StreamPacks streamPacks) {

  }
}
