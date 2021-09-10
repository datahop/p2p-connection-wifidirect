package network.datahop.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import datahop.WifiConnection;
import datahop.WifiConnectionNotifier;


/**
 * WifiLink can be used to automatically join a Wifi-Direct group in autonomous mode or a legacy
 * WiFi connection (i.e., WiFi Hotspot). It disconnects from current WiFi connection, in case it
 * is connected, when calling connect() method, and reconnects after calling disconnect() method.
 */
public class WifiLink  implements WifiConnection {


    public static final long wifiConnectionWaitingTime = 60000;

    static final public int ConectionStateNONE = 0;
    static final public int ConectionStatePreConnecting = 1;
    static final public int ConectionStateConnecting = 2;
    static final public int ConectionStateConnected = 3;
    static final public int ConectionStateDisconnected = 4;

    private int  mConectionState = ConectionStateNONE, mPreviousState= ConectionStateNONE;

    public static final String TAG = "WifiLink";

    private boolean hadConnection = false;

    WifiManager wifiManager = null;
    WifiConfiguration wifiConfig = null;
    Context context = null;
    int netId = 0;
    WiFiConnectionReceiver receiver;
    private IntentFilter filter;
    boolean connected=false;
    String ssid;

    Handler handler;

    WifiManager.WifiLock mWifiLock = null;

    Date started;

    private static volatile WifiLink mWifiLink;

    private static WifiConnectionNotifier notifier;

    private String peerId;
    /* WifiLink constructor
     * @param Android context
     */
    public WifiLink(Context context)
   {
        this.context = context;
        filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        receiver = new WiFiConnectionReceiver();
        this.wifiManager = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
        handler = new Handler();

    }

    /* Singleton method that returns a WifiLink instance
     * @return WifiLink instance
     */
    public static synchronized WifiLink getInstance(Context appContext) {
        if (mWifiLink == null) {
            mWifiLink = new WifiLink(appContext);
        }
        return mWifiLink;
    }

    /**
     * Set the notifier that receives the events advertised
     * when creating or destroying the group or when receiving users connections
     * @param notifier instance
     */
    public void setNotifier(WifiConnectionNotifier notifier){
        Log.d(TAG,"Trying to start");
        this.notifier = notifier;
    }


    /**
     * This method disconnects from any existing Wifi connection to join the SSID specified
     * as an input parameter. DHCP is used for IP configuration.
     * @param SSID of the WiFi network to join
     * @param password of the WiFi network to join
     */
    public void connect(String SSID, String password){
        connect(SSID,password,"","");
    }

    /**
     * This method disconnects from any existing Wifi connection to join the SSID specified
     * as an input parameter. IP address is configure statically from the input parameter
     * @param SSID of the WiFi network to join
     * @param password of the WiFi network to join
     * @param ip address to configure statically
     * @param peerId peerId of the host
     */
    public void connect(String SSID, String password, String ip, String peerId){

        if (notifier == null) {
            Log.e(TAG, "notifier not found");
            return ;
        }

        Log.d(TAG,"Start connection to ssid "+SSID+" Pass:"+password);
        if(!connected&&(mConectionState==ConectionStateNONE||mConectionState==ConectionStateDisconnected)) {
            started = new Date();
            this.wifiConfig = new WifiConfiguration();
            this.wifiConfig.SSID = String.format("\"%s\"", SSID);
            this.wifiConfig.preSharedKey = String.format("\"%s\"", password);
            ssid = this.wifiManager.getConnectionInfo().getSSID();
            this.wifiConfig.priority = 10000;
            List<WifiConfiguration> wifis = this.wifiManager.getConfiguredNetworks();
            boolean result;
            connected = true;
            hadConnection=false;

            Log.d(TAG,"Connected to "+ssid+" "+this.wifiManager+" "+wifis.size());

            if(wifis!=null) {
                for (WifiConfiguration wifi : wifis) {
                    if (!wifi.SSID.equals(String.format("\"%s\"", SSID))) {
                        if(wifi.SSID.startsWith("\"DIRECT")) {
                            result = this.wifiManager.removeNetwork(wifi.networkId);
                            Log.d(TAG,"Removed "+wifi.SSID+" "+result);
                        } else {
                            result = this.wifiManager.disableNetwork(wifi.networkId);
                            Log.d(TAG,"Disable "+wifi.SSID+" "+result);
                        }
                    }
                }
            }

            if (this.wifiConfig  != null && !ip.equals(""))
            {
                try
                {
                    setStaticIpConfiguration(wifiManager, wifiConfig,
                            InetAddress.getByName(ip), 24,
                            InetAddress.getByName("192.168.49.1"),
                            new InetAddress[] { InetAddress.getByName("192.168.49.1")});
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            this.context.registerReceiver(receiver, filter);
            this.netId = this.wifiManager.addNetwork(this.wifiConfig);
            Log.d(TAG,"Wifimanager add network "+netId);
            this.wifiManager.disconnect();
            this.wifiManager.enableNetwork(this.netId, true);
            this.wifiManager.reconnect();

            holdWifiLock();
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(timeout,wifiConnectionWaitingTime);

            this.peerId = peerId;
            //notifier.connectionStarted(started.getTime());

        }
    }

    public String host() {
        return peerId;
    }

    /**
     * This method disconnects from the specified network in the connect() method and rejoins any
     * prexisting Wifi connection.
     */
    public void disconnect(){
        releaseWifiLock();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG,"Disconnect");
        peerId = null;
        try {
            this.context.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){Log.d(TAG,"Unregister failed "+e);}
        if(connected){
            connected = false;
            this.wifiManager.removeNetwork(this.netId);
            List<WifiConfiguration> wifis = this.wifiManager.getConfiguredNetworks();
            if(wifis!=null) {
                for (WifiConfiguration wifi : wifis) {
                    boolean attempt = false;
                    if (wifi.SSID.equals(ssid)) attempt = true;
                    boolean result = this.wifiManager.enableNetwork(wifi.networkId, attempt);
                    Log.d(TAG,"Wifi enable "+wifi.SSID + " "+result);

                }
            }
            mConectionState=ConectionStateNONE;
            mPreviousState=ConectionStateNONE;
            Log.d(TAG,"Report disconnection");
            if(notifier!=null)notifier.onDisconnect();

        }


    }

    /***
     * Calling this method will aquire the lock on wifi. This is avoid wifi
     * from going to sleep as long as <code>releaseWifiLock</code> method is called.
     **/
    private void holdWifiLock() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if( mWifiLock == null )
            mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);

        mWifiLock.setReferenceCounted(false);

        if( !mWifiLock.isHeld() )
            mWifiLock.acquire();
    }

    /***
     * Calling this method will release if the lock is already help. After this method is called,
     * the Wifi on the device can goto sleep.
     **/
    private void releaseWifiLock() {

        if( mWifiLock == null )
            Log.w(TAG, "#releaseWifiLock mWifiLock was not created previously");

        if( mWifiLock != null && mWifiLock.isHeld() ){
            mWifiLock.release();
        }

    }


    @SuppressWarnings("unchecked")
    private static void setStaticIpConfiguration(WifiManager manager, WifiConfiguration config, InetAddress ipAddress, int prefixLength, InetAddress gateway, InetAddress[] dns) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, InstantiationException
    {
        // First set up IpAssignment to STATIC.
        Object ipAssignment = getEnumValue("android.net.IpConfiguration$IpAssignment", "STATIC");
        callMethod(config, "setIpAssignment", new String[] { "android.net.IpConfiguration$IpAssignment" }, new Object[] { ipAssignment });

        // Then set properties in StaticIpConfiguration.
        Object staticIpConfig = newInstance("android.net.StaticIpConfiguration");
        Object linkAddress = newInstance("android.net.LinkAddress", new Class<?>[] { InetAddress.class, int.class }, new Object[] { ipAddress, prefixLength });

        setField(staticIpConfig, "ipAddress", linkAddress);
        setField(staticIpConfig, "gateway", gateway);
        getField(staticIpConfig, "dnsServers", ArrayList.class).clear();
        for (int i = 0; i < dns.length; i++)
            getField(staticIpConfig, "dnsServers", ArrayList.class).add(dns[i]);

        callMethod(config, "setStaticIpConfiguration", new String[] { "android.net.StaticIpConfiguration" }, new Object[] { staticIpConfig });
        manager.updateNetwork(config);
        manager.saveConfiguration();
    }


    private static Object newInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
    {
        return newInstance(className, new Class<?>[0], new Object[0]);
    }

    private static Object newInstance(String className, Class<?>[] parameterClasses, Object[] parameterValues) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException
    {
        Class<?> clz = Class.forName(className);
        Constructor<?> constructor = clz.getConstructor(parameterClasses);
        return constructor.newInstance(parameterValues);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object getEnumValue(String enumClassName, String enumValue) throws ClassNotFoundException
    {
        Class<Enum> enumClz = (Class<Enum>)Class.forName(enumClassName);
        return Enum.valueOf(enumClz, enumValue);
    }

    private static void setField(Object object, String fieldName, Object value) throws IllegalAccessException, IllegalArgumentException, NoSuchFieldException
    {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.set(object, value);
    }

    private static <T> T getField(Object object, String fieldName, Class<T> type) throws IllegalAccessException, IllegalArgumentException, NoSuchFieldException
    {
        Field field = object.getClass().getDeclaredField(fieldName);
        return type.cast(field.get(object));
    }

    private static void callMethod(Object object, String methodName, String[] parameterTypes, Object[] parameterValues) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException
    {
        Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
            parameterClasses[i] = Class.forName(parameterTypes[i]);

        Method method = object.getClass().getDeclaredMethod(methodName, parameterClasses);
        method.invoke(object, parameterValues);
    }

    Runnable timeout = new Runnable() {
        @Override
        public void run() {
            //Do something after 100ms
            if (!hadConnection) {
                Log.d(TAG, "timeout");
                notifier.onConnectionFailure(1,started.getTime(),(new Date()).getTime());
                disconnect();

            }
        }
    };

    private class WiFiConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info != null) {
                    mPreviousState = mConectionState;
                    if (info.isConnected()) {
                        mConectionState = ConectionStateConnected;
                    }else if(info.isConnectedOrConnecting()) {
                        mConectionState = ConectionStateConnecting;
                    }else {
                        if(hadConnection){
                            mConectionState = ConectionStateDisconnected;
                        }else{
                            mConectionState = ConectionStatePreConnecting;
                        }
                    }

                    Log.d(TAG,"DetailedState: " + info.getDetailedState());

                    String conStatus = "";
                    if(mConectionState == WifiLink.ConectionStateNONE) {
                        conStatus = "NONE";
                    }else if(mConectionState == WifiLink.ConectionStatePreConnecting) {
                        conStatus = "PreConnecting";
                    }else if(mConectionState == WifiLink.ConectionStateConnecting) {
                        conStatus = "Connecting";
                    }else if(mConectionState == WifiLink.ConectionStateConnected) {
                        conStatus = "Connected";
                    }else if(mConectionState == WifiLink.ConectionStateDisconnected) {
                        conStatus = "Disconnected";
                        Log.d(TAG,"Had connection "+hadConnection);

                        if(hadConnection)disconnect();

                    }
                    Log.d(TAG, "Status " + conStatus);

                }

                WifiInfo wiffo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                if(wiffo==null)Log.d(TAG,"Wiifiinfo null "+wifiManager.getConnectionInfo());
                if(wiffo==null&&mConectionState==ConectionStateConnected)wiffo = wifiManager.getConnectionInfo();
                if(wiffo!=null)Log.d(TAG,"Wifiinfo "+wiffo.getSSID()+" "+ConectionStateConnected+" "+mConectionState+" "+hadConnection+" "+wifiConfig.SSID);
                if(wiffo!=null&&mConectionState==ConectionStateConnected&&mPreviousState==ConectionStateConnecting){
                    handler.removeCallbacks(timeout);
                    if(wiffo.getSSID().equals(wifiConfig.SSID)&&!hadConnection) {

                        hadConnection=true;
                        Log.d(TAG, "Connected to " + wiffo);
                        int ip = wiffo.getIpAddress();
                        int gateway = wifiManager.getDhcpInfo().gateway;
                        try {
                            String ipAddress = InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array()).getHostAddress();
                            String gwAddress = InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(gateway).array()).getHostAddress();
                            Log.d(TAG, "IP  " + ipAddress + " " + gwAddress);

                            if(notifier!=null)notifier.onConnectionSuccess(started.getTime(),(new Date()).getTime(),wiffo.getRssi(),wiffo.getLinkSpeed(),wiffo.getFrequency());
                        }catch (UnknownHostException e){}
                    } else if(!wiffo.getSSID().equals(wifiConfig.SSID)) {
                        Log.d(TAG, "Not connected");
                        if(notifier!=null)notifier.onConnectionFailure(0,started.getTime(),(new Date()).getTime());

                    }

                }
            }
        }
    }


}
