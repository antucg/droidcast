package net.majorkernelpanic.streaming.screen;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import net.majorkernelpanic.streaming.Session;

/**
 * Created by antonio.carrasco on 17/01/2017.
 */

public class StreamProcessorServer extends Thread implements Runnable {

  private final String TAG = this.getClass().getSimpleName();
  private final ServerSocket mServer;

  public StreamProcessorServer() throws IOException {
    int port = 9999;
    try {
      mServer = new ServerSocket(port);
      start();
    } catch (BindException e) {
      Log.e(TAG, "Port " + port + " already in use !");
      throw e;
    }
  }

  public void run() {
    Log.i(TAG, "Stream processor server listening on port " + mServer.getLocalPort());
    while (!Thread.interrupted()) {
      try {
        new WorkerThread(mServer.accept()).start();
      } catch (SocketException e) {
        break;
      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
      }
    }
    Log.i(TAG, "Stream processor server stopped !");
  }

  public void kill() {
    try {
      mServer.close();
    } catch (IOException e) {
    }
    try {
      this.join();
    } catch (InterruptedException ignore) {
    }
  }

  // One thread per client
  private class WorkerThread extends Thread implements Runnable {

    private final Socket mClient;
    private final OutputStream mOutput;
    private final BufferedReader mInput;

    // Each client has an associated session
    private Session mSession;

    public WorkerThread(final Socket client) throws IOException {
      mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
      mOutput = client.getOutputStream();
      mClient = client;
    }

    public void run() {

      Log.i(TAG, "Connection from " + mClient.getInetAddress().getHostAddress());

      while (!Thread.interrupted()) {
        String response = 	"RTSP/1.0 200 OK\r\n" +
            "Server: StremProcessorServer\r\n" +
            "Content-Length: " + 0 + "\r\n" +
            "\r\n";
        try {
          mOutput.write(response.getBytes());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      try {
        mClient.close();
      } catch (IOException ignore) {
      }

      Log.i(TAG, "Client disconnected");
    }
  }
}
