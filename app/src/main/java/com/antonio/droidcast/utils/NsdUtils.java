package com.antonio.droidcast.utils;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Base64;
import android.util.Log;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.models.ConnectionInfo;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by antonio.carrasco on 15/02/2017.
 */

public class NsdUtils {

  private final String TAG = this.getClass().getSimpleName();
  private final String NSD_ATTRIBUTE_KEY = "nsd_attribute_key";
  private final String NSD_SERVICE_NAME = "DroidCast";
  private final String NSD_SERVICE_TYPE = "_rtsp._udp.";

  @Inject Context context;
  @Inject MetaDataProvider metaDataProvider;
  private NsdManager.RegistrationListener registrationListener;
  private NsdManager.DiscoveryListener discoveryListener;
  private NsdManager.ResolveListener resolveListener;
  private NsdManager nsdManager;

  public NsdUtils() {
    IOCProvider.getInstance().inject(this);
    nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
  }

  private int getAvailablePort() {
    try {
      ServerSocket serverSocket = new ServerSocket(0);
      return serverSocket.getLocalPort();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  public void registerNsdService(Activity activity, String mediaShareCode) {
    //int port = getAvailablePort();
    int port = 1234;

    //if (port == 0) {
    //  return;
    //}

    String nsdKey = metaDataProvider.getNsdKey();
    if (nsdKey == null) {
      Log.e(TAG, "[NsdUtils] - registerNsdService, nsdKey empty");
      return;
    }

    registrationListener = new NsdManager.RegistrationListener() {
      @Override public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "[NsdUtils] - onRegistrationFailed, " + errorCode);
      }

      @Override public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "[NsdUtils] - onUnregistrationFailed, " + errorCode);
      }

      @Override public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "[NsdUtils] - onServiceRegistered");
      }

      @Override public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        nsdManager.unregisterService(registrationListener);
      }
    };

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

    resolveListener = new NsdManager.ResolveListener() {
      @Override public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        if (callback != null) {
          callback.onError();
        }
      }

      @Override public void onServiceResolved(NsdServiceInfo serviceInfo) {
        Map<String, byte[]> attributes = serviceInfo.getAttributes();
        String attributeKey = new String(attributes.get(NSD_ATTRIBUTE_KEY));
        String raw = nsdKey + mediaShareCode;
        if (attributeKey.equals(Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP))) {
          ConnectionInfo connectionInfo =
              new ConnectionInfo(serviceInfo.getHost(), serviceInfo.getPort());
          if (callback != null) {
            callback.onHostFound(connectionInfo);
          }
        }
      }
    };

    discoveryListener = new NsdManager.DiscoveryListener() {
      @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "[NsdUtils] - onStartDiscoveryFailed(), " + errorCode);
        nsdManager.stopServiceDiscovery(this);
      }

      @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "[NsdUtils] - onStopDiscoveryFailed(), " + errorCode);
        nsdManager.stopServiceDiscovery(this);
      }

      @Override public void onDiscoveryStarted(String serviceType) {
        Log.d(TAG, "[NsdUtils] - onDiscoveryStarted(), " + serviceType);
      }

      @Override public void onDiscoveryStopped(String serviceType) {
        Log.d(TAG, "[NsdUtils] - onDiscoveryStopped(), " + serviceType);
      }

      @Override public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "[NsdUtils] - onServiceFound()");
        if (serviceInfo.getServiceName().contains(NSD_SERVICE_NAME)) {
          nsdManager.resolveService(serviceInfo, resolveListener);
        }
      }

      @Override public void onServiceLost(NsdServiceInfo serviceInfo) {

      }
    };
    nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
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
