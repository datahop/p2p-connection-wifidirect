package network.datahop.wifitransport;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import datahop.WifiConnectionNotifier;
import datahop.WifiHotspotNotifier;

import network.datahop.wifidirect.WifiLink;
import network.datahop.wifidirect.WifiDirectHotSpot;

public class MainActivity extends AppCompatActivity implements WifiHotspotNotifier, WifiConnectionNotifier {


    private Button startHSButton,stopHSButton,connectButton,disconnectButton;

    private EditText ssid, password;
    private TextView ssidView, passView;
    private WifiDirectHotSpot hotspot;
    private WifiLink connection;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_WIFI_STATE = 2;

    private static final String TAG = "WifiTransportDemo";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hotspot = WifiDirectHotSpot.getInstance(getApplicationContext());
        connection = WifiLink.getInstance(getApplicationContext());
        hotspot.setNotifier(this);
        connection.setNotifier(this);
        startHSButton = (Button) findViewById(R.id.activatebutton);
        connectButton = (Button) findViewById(R.id.connectbutton);

        stopHSButton = (Button) findViewById(R.id.stopbutton);
        disconnectButton = (Button) findViewById(R.id.disconnectbutton);

        ssid = (EditText) findViewById(R.id.network);
        password = (EditText) findViewById(R.id.password);

        ssidView = (TextView) findViewById(R.id.textview_ssid);
        passView = (TextView) findViewById(R.id.textview_pass);

        requestForPermissions();
        startHSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hotspot.start();
            }
        });

        stopHSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hotspot.stop();
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection.disconnect();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        hotspot.stop();
        super.onDestroy();
    }

    @Override
    public void clientsConnected(long l) {
        Log.d(TAG,"Clients connected "+l);
    }

    @Override
    public void networkInfo(String net, String pass) {
        ssidView.setText("SSID: "+net);
        passView.setText("Pass: "+pass);
    }

    @Override
    public void onFailure(long l) {
        Log.d(TAG,"onFailure");

    }

    @Override
    public void onSuccess() {
        Log.d(TAG,"onSuccess");

    }

    @Override
    public void onDisconnect() {
        Log.d(TAG,"onDisconnect");

    }

    @Override
    public void onConnectionFailure(long l) {
        Log.d(TAG,"onFailure "+l);

    }

    @Override
    public void onConnectionSuccess() {
        Log.d(TAG,"onSuccess");

    }

    @Override
    public void stopOnFailure(long l) {
        Log.d(TAG,"stopOnFailure "+l);
    }

    @Override
    public void stopOnSuccess() {
        Log.d(TAG,"stopOnSuccess");

    }

    private void requestForPermissions() {
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                }
            });
            builder.show();
        }
        if (this.checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE}, PERMISSION_WIFI_STATE);
                }
            });
            builder.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permissions " + requestCode + " " + permissions + " " + grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d(TAG, "Location accepted");
                    //timers.setLocationPermission(true);
                    //if(timers.getStoragePermission())startService();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "Location not accepted");

                }
                break;
            }

        }

        // other 'case' lines to check for other
        // permissions this app might request.

    }
}