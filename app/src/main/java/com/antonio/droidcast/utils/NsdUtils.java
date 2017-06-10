package com.antonio.droidcast.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.models.ConnectionInfo;
import com.youview.tinydnssd.DiscoverResolver;
import com.youview.tinydnssd.MDNSDiscover;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import javax.inject.Inject;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

/**
 * Created by antonio.carrasco on 15/02/2017.
 */

public class NsdUtils {

  public final static int DEFAULT_PORT = 55640;
  private final String TAG = this.getClass().getSimpleName();
  private final String NSD_ATTRIBUTE_KEY = "nsd_key";
  private final String NSD_SERVICE_NAME = "DroidCast";
  private final String NSD_SERVICE_TYPE = "_rtsp._udp.";

  @Inject Context context;
  @Inject MetaDataProvider metaDataProvider;
  private NsdManager.RegistrationListener registrationListener;
  private NsdManager.DiscoveryListener discoveryListener;
  private NsdManager.ResolveListener resolveListener;
  private NsdManager nsdManager;
  private DiscoverResolver resolver;

  public NsdUtils() {
    IOCProvider.getInstance().inject(this);
    nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
  }

  public int getAvailablePort() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    return Integer.parseInt(sharedPreferences.getString(RtspServer.KEY_PORT, String.valueOf(DEFAULT_PORT)));
    //try {
    //  ServerSocket serverSocket = new ServerSocket(0);
    //  return serverSocket.getLocalPort();
    //} catch (IOException e) {
    //  e.printStackTrace();
    //}
    //return 0;
  }

  public void registerNsdService(Activity activity, String mediaShareCode, NsdManager.RegistrationListener listener) {
    int port = getAvailablePort();
    //if (port == 0) {
    //  return;
    //}

    String nsdKey = metaDataProvider.getNsdKey();
    if (nsdKey == null) {
      Log.e(TAG, "[NsdUtils] - registerNsdService, nsdKey empty");
      return;
    }

    registrationListener = listener;
    String raw = nsdKey + mediaShareCode;
    String encoded = Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP);

    NsdServiceInfo serviceInfo = new NsdServiceInfo();
    serviceInfo.setServiceName(NSD_SERVICE_NAME);
    serviceInfo.setServiceType(NSD_SERVICE_TYPE);
    serviceInfo.setPort(port);
    serviceInfo.setAttribute(NSD_ATTRIBUTE_KEY, encoded);

    nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
  }

  public void discoverNsdService(Activity activity, final String mediaShareCode,
      final NsdResolveCallback callback) {

    final String nsdKey = metaDataProvider.getNsdKey();
    if (nsdKey == null) {
      return;
    }

    resolver = new DiscoverResolver(context, NSD_SERVICE_TYPE, new DiscoverResolver.Listener() {
      @Override public void onServicesChanged(Map<String, MDNSDiscover.Result> services) {
        for (MDNSDiscover.Result result : services.values()) {
          if (result.srv.fqdn.contains(NSD_SERVICE_NAME)) {
            String attributeKey = result.txt.dict.get(NSD_ATTRIBUTE_KEY);
            String raw = nsdKey + mediaShareCode;
            if (attributeKey.equals(Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP))) {
              ConnectionInfo connectionInfo = new ConnectionInfo(result.a.ipaddr, result.srv.port);
              if (callback != null) {
                callback.onHostFound(connectionInfo);
              }
              resolver.stop();
              return;
            }
          }
        }
      }
    });
    resolver.start();
  }

  public void tearDown() {
    if (discoveryListener != null) {
      nsdManager.stopServiceDiscovery(discoveryListener);
      discoveryListener = null;
    }
    if (registrationListener != null) {
      nsdManager.unregisterService(registrationListener);
      registrationListener = null;
    }
    if (resolveListener != null) {
      resolveListener = null;
    }
  }

  public interface NsdResolveCallback {
    void onHostFound(ConnectionInfo connectionInfo);

    void onError();
  }
}
