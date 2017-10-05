package com.app.droidcast.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import com.app.droidcast.ioc.IOCProvider;
import com.app.droidcast.models.ConnectionInfo;
import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDBindable;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.RegisterListener;
import com.github.druk.dnssd.ResolveListener;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import javax.inject.Inject;

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
  private DNSSD dnssd;
  private DNSSDService registerService;
  private DNSSDService browseService;
  private boolean serviceFound = false;

  public NsdUtils() {
    IOCProvider.getInstance().inject(this);
    nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    dnssd = new DNSSDBindable(context);
  }

  public void registerNsdService(Activity activity, String mediaShareCode,
      NsdManager.RegistrationListener listener) {
    int port = Utils.getAvailablePort(context);
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

  public void newRegisterNsdService(final String mediaShareCode,
      RegisterListener registerListener) {
    final int port = Utils.getAvailablePort(context);
    try {
      registerService =
          dnssd.register(NSD_SERVICE_NAME + "-" + Utils.MD5(mediaShareCode), NSD_SERVICE_TYPE, port,
              registerListener);
    } catch (DNSSDException e) {
      Log.e(TAG, "error registering sevice with NSD", e);
    }
  }

  public void newDiscoverNsdService(final String mediaSharecode,
      final NsdResolveCallback callback) {
    serviceFound = false;
    try {
      Log.d(TAG, "Browsing services");
      browseService = dnssd.browse(NSD_SERVICE_TYPE, new BrowseListener() {

        @Override public void serviceFound(DNSSDService browser, int flags, int ifIndex,
            final String serviceName, String regType, String domain) {
          Log.i(TAG, "Found " + serviceName);
          if (serviceName.equals(NSD_SERVICE_NAME + "-" + Utils.MD5(mediaSharecode))) {
            startResolve(flags, ifIndex, serviceName, regType, domain, callback);
          }
        }

        @Override
        public void serviceLost(DNSSDService browser, int flags, int ifIndex, String serviceName,
            String regType, String domain) {
          Log.i(TAG, "Lost " + serviceName);
        }

        @Override public void operationFailed(DNSSDService service, int errorCode) {
          Log.e(TAG, "error: " + errorCode);
        }
      });
    } catch (DNSSDException e) {
      Log.e(TAG, "error", e);
    }

    new Handler().postDelayed(new Runnable() {
      @Override public void run() {
        if (!serviceFound && callback != null) {
          callback.onHostNotFound();
          tearDown();
        }
      }
    }, 5000);
  }

  private void startResolve(int flags, int ifIndex, final String serviceName, final String regType,
      final String domain, final NsdResolveCallback callback) {
    try {
      dnssd.resolve(flags, ifIndex, serviceName, regType, domain, new ResolveListener() {
        @Override
        public void serviceResolved(DNSSDService resolver, int flags, int ifIndex, String fullName,
            String hostName, int port, Map<String, String> txtRecord) {
          Log.d("TAG", "Resolved " + hostName);
          startQueryRecords(flags, ifIndex, serviceName, regType, domain, hostName, port, txtRecord,
              callback);
        }

        @Override public void operationFailed(DNSSDService service, int errorCode) {

        }
      });
    } catch (DNSSDException e) {
      e.printStackTrace();
    }
  }

  private void startQueryRecords(int flags, int ifIndex, final String serviceName,
      final String regType, final String domain, final String hostName, final int port,
      final Map<String, String> txtRecord, final NsdResolveCallback callback) {
    try {
      QueryListener listener = new QueryListener() {
        @Override public void queryAnswered(DNSSDService query, final int flags, final int ifIndex,
            final String fullName, int rrtype, int rrclass, final InetAddress address, int ttl) {
          String ipAddress = address.getHostAddress();
          boolean isIPv4 = ipAddress.indexOf(':') < 0;
          if (isIPv4 && callback != null) {
            serviceFound = true;
            callback.onHostFound(new ConnectionInfo(ipAddress, port));
            tearDown();
          }
        }

        @Override public void operationFailed(DNSSDService service, int errorCode) {

        }
      };
      dnssd.queryRecord(0, ifIndex, hostName, 1, 1, listener);
      dnssd.queryRecord(0, ifIndex, hostName, 28, 1, listener);
    } catch (DNSSDException e) {
      e.printStackTrace();
    }
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
    if (registerService != null) {
      registerService.stop();
      registerService = null;
    }
    if (browseService != null) {
      browseService.stop();
      browseService = null;
    }
  }

  public interface NsdResolveCallback {
    void onHostFound(ConnectionInfo connectionInfo);

    void onHostNotFound();

    void onError();
  }
}
