package com.app.droidcast.utils;

import android.os.AsyncTask;
import android.util.Log;
import com.app.droidcast.ioc.IOCProvider;
import com.app.droidcast.models.ConnectionInfo;
import javax.inject.Inject;
import redis.clients.jedis.Jedis;

/**
 * Created by wbmpe044 on 29/09/2017.
 */

public class RedisUtils {

  // Tag to use when logging messages
  protected final String TAG = this.getClass().getSimpleName();

  @Inject MetaDataProvider metaDataProvider;
  private Jedis jedis;

  public RedisUtils() {
    IOCProvider.getInstance().inject(this);
  }

  public void connect(RedisConnected cb) {
    new RedisConnectAsyncTask(cb).execute();
  }

  public void saveSession(final String code, final int port) throws Exception {
    final String ip = Utils.getIPAddress(true);
    if (ip == null) {
      throw new Exception("IP addres can't be null");
    }

    connect(new RedisConnected() {
      @Override public void connected() {
        try {
          new RedisSaveSessionAsyncTask().execute(Utils.MD5(code), ip + ":" + port);
        } catch (Exception e) {
          Log.e(TAG, "Error registering session in redis");
        }
      }
    });
  }

  public void deleteSession(final String code) {
    connect(new RedisConnected() {
      @Override public void connected() {
        new RedisDeleteSessionAsyncTask().execute(Utils.MD5(code));
      }
    });
  }

  public void getSessionData(final String code, final RedisSession cb) {
    connect(new RedisConnected() {
      @Override public void connected() {
        new RedisGetSessionAsyncTask(cb).execute(Utils.MD5(code));
      }
    });
  }

  public void teardown() {
    if (jedis != null) {
      jedis.disconnect();
    }
  }

  private class RedisConnectAsyncTask extends AsyncTask<Void, Void, Jedis> {
    private RedisConnected cb;

    public RedisConnectAsyncTask(RedisConnected cb) {
      this.cb = cb;
    }

    @Override protected Jedis doInBackground(Void... params) {
      return new Jedis(metaDataProvider.getRedisHost());
    }

    @Override protected void onPostExecute(Jedis jedis) {
      RedisUtils.this.jedis = jedis;
      if (cb != null) {
        cb.connected();
      }
    }
  }

  private class RedisSaveSessionAsyncTask extends AsyncTask<String, Void, String> {
    @Override protected String doInBackground(String... params) {
      return jedis.setex(params[0], 3600, params[1]);
    }

    @Override protected void onPostExecute(String result) {
      if (result != null && result.equals("OK")) {
        Log.d(TAG, "Session saved in redis");
      }
      teardown();
    }
  }

  private class RedisDeleteSessionAsyncTask extends AsyncTask<String, Void, Long> {

    @Override protected Long doInBackground(String... params) {
      return jedis.del(params[0]);
    }

    @Override protected void onPostExecute(Long aLong) {
      Log.d(TAG, "Deleted records: " + aLong);
      teardown();
    }
  }

  private class RedisGetSessionAsyncTask extends AsyncTask<String, Void, String> {
    private RedisSession cb;

    public RedisGetSessionAsyncTask(RedisSession cb) {
      this.cb = cb;
    }

    @Override protected String doInBackground(String... params) {
      return jedis.get(params[0]);
    }

    @Override protected void onPostExecute(String s) {
      if (cb != null) {
        if (s != null) {
          String[] hostPort = s.split(":");
          cb.data(new ConnectionInfo(hostPort[0], Integer.parseInt(hostPort[1])));
        } else {
          cb.sessionNotFound();
        }
      }
      teardown();
    }
  }

  public interface RedisConnected {
    void connected();
  }

  public interface RedisSession {
    void data(ConnectionInfo connectionInfo);

    void sessionNotFound();
  }
}
