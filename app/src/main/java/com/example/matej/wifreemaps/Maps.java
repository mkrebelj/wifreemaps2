package com.example.matej.wifreemaps;

import android.app.Dialog;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Maps extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener{

    private long waitForLocationTimeout;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    List<ScanResult> wifiList;
    WifiManager mainWifi;
    TextView debugTextView;
    WifiReceiver receiverWifi;
    LocationManager mainLocationManager;
    private Context mContext;
    PromptWiFi popupWifi;
    PromptGPS popupGPS;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String EAP = "EAP";
    public static final String OPEN = "Open";

    private LocationManager myLocManager;
    private GoogleApiClient myGoogleApiClient;
    private LatLng myLocation;
    private int gpsPrecision=10;
    private HashMap <String,AccessPoint> accessPoints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        waitForLocationTimeout=System.currentTimeMillis()+5000;

        accessPoints=new HashMap<String,AccessPoint>();
        myLocation= new LatLng(0,0);
        debugTextView = (TextView) findViewById(R.id.debugText);

        receiverWifi = new WifiReceiver();
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if(!mainWifi.isWifiEnabled()){
            //wifi not enabled, prompt to enable
            popupWifi=new PromptWiFi();
            popupWifi.show(getFragmentManager(), "popup WiFi");


        }
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        debugTextView.setText("\nStarting Scan...\n");
        debugTextView.setMovementMethod(new ScrollingMovementMethod());



        mainLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mainLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            //GPS not enabled, prompt
            popupGPS=new PromptGPS();
            popupGPS.show(getFragmentManager(),"popup GPS");
        }




    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        mMap.setMyLocationEnabled(true);

    }

    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (System.currentTimeMillis() > waitForLocationTimeout) {
                StringBuilder sb;
                sb = new StringBuilder();
                wifiList = mainWifi.getScanResults();
                //			for(int i = 0; i < wifiList.size(); i++){
                //				sb.append(new Integer(i+1).toString() + ".");
                //				sb.append((wifiList.get(i)).toString());
                //				sb.append("\n");
                //			}
                //			mainText.setText(sb);


                //check if there is any open access points

                String[] securityModes = {WEP, PSK, EAP};
                int idx = 0;

                sb.append("Currently sniffed:\n");

                for (ScanResult scan : wifiList) {
                    sb.append(new Integer(idx + 1).toString() + ".");
                    sb.append("SSID: " + scan.SSID);
                    sb.append("BSSID: " + scan.BSSID);
                    sb.append("Security: " + scan.capabilities);

                    sb.append("Freq:" + scan.frequency);

                    sb.append("\n");

                    idx++;

                    //add if there is no information for this bssid
                    if (!accessPoints.containsKey(scan.BSSID)) {
                        //String bssid, String ssid, int frequency, String region, String security, int internetavailable, LatLng gpslocation, Timestamp timestamp
                        accessPoints.put(scan.BSSID, new AccessPoint(scan.BSSID, scan.SSID, scan.frequency, "unknown", scan.capabilities, -1, myLocation, scan.timestamp));
                    }
                    accessPoints.get(scan.BSSID).addNewMeasurement(myLocation, scan.level, gpsPrecision, scan.timestamp);

                }
                debugTextView.setText(sb);
                //just test adding markers on map
                updateMapMarkers();
            }
            else
            {
                Log.d("Waiting","Location might be falsch");
            }
        }

    }

    //GOOGLE SERVICES
    @Override
    public void onLocationChanged(Location newLocation) {
        // TODO Auto-generated method stub

        if(newLocation != null){

            myLocation=new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
            Log.d("Found alt location","Location:"+myLocation.toString());
        }

    }





    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO Auto-generated method stub
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
			/*
			 * If no resolution is available, display a dialog to the
			 * user with the error.
			 */
            Toast.makeText(this, "Error code:" + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onConnected(Bundle arg0) {
        // TODO Auto-generated method stub
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();

    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                Log.d("Location Updates","Google Play services error:" +errorDialog.toString());
            }
            return false;
        }
    }

    private void updateMapMarkers(){
        if(myLocation.equals(new LatLng(0,0)))
        {
           //find my current position
        }
        for(Map.Entry<String,AccessPoint> entry:accessPoints.entrySet())
        {
            mMap.addMarker(new MarkerOptions()
                    .position(entry.getValue().getGPSLocation())
                    .title(entry.getKey()));
        }


    }



}
