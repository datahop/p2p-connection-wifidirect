package network.datahop.wifidirect;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import datahop.WifiHotspot;
import datahop.WifiHotspotNotifier;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * WifiDirectHotSpot class creates a Wifi-Direct group in autonomous mode; i.e. a Wifi-Direct
 * group is generated without requiring any user connected to it, and can be discovered and joined in
 * the same way a legacy WiFi hotspot. Implements WifiHotspot (link to go bindings)
 */
public class WifiDirectHotSpot implements ConnectionInfoListener, ChannelListener, GroupInfoListener, WifiHotspot {


    WifiDirectHotSpot that = this;
    Context context;

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;

    String mNetworkName = "";
    String mPassphrase = "";
    String mInetAddress = "";

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    public static final String TAG = "WifiDirectHotSpot";

    boolean started;

    private static volatile WifiDirectHotSpot mWifiHotspot;

    private static WifiHotspotNotifier notifier;


    /* WifiDirectHotSpot constructor
     * @param Android context
     */
    private WifiDirectHotSpot(Context Context) {
        this.context = Context;
        started = false;

    }

    /* Singleton method that returns a WifiDirectHotSpot instance
     * @return WifiDirectHotSpot instance
     */
    public static synchronized WifiDirectHotSpot getInstance(Context appContext) {
        if (mWifiHotspot == null) {
            mWifiHotspot = new WifiDirectHotSpot(appContext);
            // initDriver();
        }
        return mWifiHotspot;
    }


    /**
     * Set the notifier that receives the events advertised
     * when creating or destroying the group or when receiving users connections
     * @param notifier instance
     */
    public void setNotifier(WifiHotspotNotifier notifier) {
        Log.d(TAG, "Trying to start");
        this.notifier = notifier;
    }


    /**
     * Starts the service and creates the Wifi-Direct group
     */
    public void start() {

        if (notifier == null) {
            Log.e(TAG, "notifier not found");
            return;
        }
        if (!started) {
            Log.d(TAG, "Starting P2P group " + mNetworkName + ":" + mPassphrase);
            started = true;

            p2p = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);

            if (p2p == null) {
                Log.d(TAG, "This device does not support Wi-Fi Direct");
            } else {

                channel = p2p.initialize(context, context.getMainLooper(), this);
                receiver = new AccessPointReceiver();

                filter = new IntentFilter();
                filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
                filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
                try {
                    this.context.registerReceiver(receiver, filter);
                } catch (Exception e) {
                    Log.d(TAG, "leaked register");
                }

                if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                p2p.createGroup(channel, new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        Log.d(TAG, "Creating Local Group ");
                        //listener.onSuccess();
                        if (notifier != null) notifier.onSuccess();
                    }

                    public void onFailure(int reason) {
                        Log.d(TAG, "Local Group failed, error code " + reason);
                        if (notifier != null) notifier.onFailure(reason);
                    }
                });
                p2p.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        try {
                            if (mNetworkName.equals(group.getNetworkName()) && mPassphrase.equals(group.getPassphrase())) {
                                Log.d(TAG, "Already have local service for " + mNetworkName + " ," + mPassphrase);
                            } else {
                                mNetworkName = group.getNetworkName();
                                mPassphrase = group.getPassphrase();
                                Log.d(TAG, "onGroupInfoAvailable :" + mNetworkName + ":" + mPassphrase + ":");
                                if (notifier != null) {
                                    notifier.networkInfo(mNetworkName, mPassphrase);
                                } else {
                                    Log.d(TAG, "onGroupInfoAvailable notifier null");
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onGroupInfoAvailable, error: " + e.toString());
                        }
                    }
                });
            }
        } else {
            Log.d(TAG, "Trying to set network");
            if (notifier != null) notifier.networkInfo(mNetworkName, mPassphrase);
        }
    }

    /**
     * Stops the service and destroys the Wifi-Direct group
     */
    public void stop() {
        if (started) {
            //Log.d(TAG,"Stop");
            try {
                removeGroup();
            } catch (Exception e) {
                Log.d(TAG, "Remove group error " + e);
            }
            started = false;
        }

    }

    private void removeGroup() {
        Log.d(TAG, "Removing P2P group");
        p2p.removeGroup(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                //Log.d(TAG,"Cleared Local Group ");
                if (notifier != null) notifier.stopOnSuccess();
            }

            public void onFailure(int reason) {
                //Log.d(TAG,"Clearing Local Group failed, error code " + reason);
                if (notifier != null) notifier.stopOnFailure(reason);
            }
        });
    }


    @Override
    public void onChannelDisconnected() {
        // see how we could avoid looping
        //     p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
        //     channel = p2p.initialize(this.context, this.context.getMainLooper(), this);
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        Log.d(TAG, "outer onGroupInfoAvailable");
        try {
            if (notifier != null) notifier.clientsConnected(group.getClientList().size());
            if (mNetworkName.equals(group.getNetworkName()) && mPassphrase.equals(group.getPassphrase())) {
                Log.d(TAG, "Already have local service for " + mNetworkName + " ," + mPassphrase);
            } else {
                mNetworkName = group.getNetworkName();
                mPassphrase = group.getPassphrase();
                Log.d(TAG, "onGroupInfoAvailable " + mNetworkName + " " + mPassphrase);
                if (notifier != null) notifier.networkInfo(mNetworkName, mPassphrase);
            }
        } catch (Exception e) {
            Log.d(TAG, "onGroupInfoAvailable, error: " + e.toString());
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "WifiP2pinfo received " + info);
        try {
            if (info.isGroupOwner) {
                mInetAddress = info.groupOwnerAddress.getHostAddress();
                Log.d(TAG, "inet address " + mInetAddress);
                if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                p2p.requestGroupInfo(channel, this);
            } else {
                Log.d(TAG,"we are client !! group owner address is: " + info.groupOwnerAddress.getHostAddress());
            }
        } catch(Exception e) {
            Log.d(TAG,"onConnectionInfoAvailable, error: " + e.toString());
        }
    }

    private class AccessPointReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    //debug_print("We are connected, will check info now");
                    Log.d(TAG,"We are connected, will check info now");
                    p2p.requestConnectionInfo(channel, that);
                } else{
                    //debug_print("We are DIS-connected");
                    Log.d(TAG,"We are DIS-connected");
                }
            }
        }
    }



}