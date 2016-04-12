package org.openlobster.olbs;


import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;




public class OpenLobster  implements BeaconConsumer, SensorEventListener, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private OLBSNotifier  ole;

    private Context mContext;
    public static OpenLobster client = null;

    boolean bFindAllBeacon = true;
    boolean bFindNearestBeacon = false;
    boolean bEstimateLocation = false;
    boolean bBeaconRanging = true;
    boolean bBeaconMonitoring = false;

    boolean bSetUserTokenReady = false;

	// You need to request your own OLBSAPIKey from OpenLobster.org
    private String UserAPIkey = "1234567890123456789012";

    final String TAG = "OLBS";

    //
    private OLBSLocation olbs_location;

    //http varible
    private int session_id = 0;
    private int HttpConnectionTimeout = 5000;
    private int SocketConnectionTimeout = 5000;


    //scanlog variable
    private String  ScanLogUrl =   "http://api.openlobster.org/v1/estimateOLBSLocation";
    private String UserToken = "";
    private String ScanLogLocLabel = "";

    //get floor map
    private String  GetFloorMapUrl =   "http://api.openlobster.org/v1/getFloorMap/";

    //Get olbs path
    private String GetOlbsPathUrl = "http://api.openlobster.org/v1/getOLBSPath";

//
    private String SendMSG2UserAtLocationWithWebCallBackUrl = "http://api.openlobster.org/v1/sendMSG2UserAtLocationWithWebCallBack";

    //beacon varible
    private BeaconManager olBeaconManager;

    private long BeaconBackgroundBetweenScanPeriod = 30000;
    private long BeaconBackgroundScanPeriod = 5000;
    private long BeaconForegroundBetweenScanPeriod = 1000;
    private long BeaconForegroundScanPeriod = 5000;

    private Collection<OLBSBeacon> RadarRegion;
    private Collection<OLBSBeacon> LastBeaconInRegion;

    //GPS variable
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;
    private long LocationUpdateIntervalInMilliseconds = 10000;
    private long LocationFastestUpdateInMillisecond = LocationUpdateIntervalInMilliseconds /2;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    //Sensor
    private SensorManager olSensorManager;

    public OpenLobster(OLBSNotifier  inole) {

        ole = inole;
        olbs_location = new OLBSLocation();

        RadarRegion = new ArrayList<OLBSBeacon>();
        LastBeaconInRegion = new ArrayList<OLBSBeacon>();


    }

  public static OpenLobster getInstanceForApplication(Context context) {

       return client;

    }

    public void setContext(Context inContext) {
        client = this;
        mContext = inContext;
    }

    public boolean checkUserTokenReady()
    {
        return bSetUserTokenReady;
    }

    public String getUserToken()
    {
        return UserToken;
    }

    public void setOpenLobsterConfig(OLBSConfigType olConfigType, long value )
    {
        try{
            switch (olConfigType) {
                case OLBS_BEACON_BACKGROUND_BETWEEN_SCAN_PERIOD:
                    BeaconBackgroundBetweenScanPeriod = value;

                    if ( olBeaconManager != null) {
                        if (olBeaconManager.isBound(this)) {
                            olBeaconManager.setBackgroundBetweenScanPeriod(BeaconBackgroundBetweenScanPeriod);
                            olBeaconManager.updateScanPeriods();

                        }
                    }
                    break;

                case OLBS_BEACON_FOREGROUND_BETWEEN_SCAN_PERIOD:
                    BeaconForegroundBetweenScanPeriod = value;
                    if ( olBeaconManager != null) {
                        if (olBeaconManager.isBound(this)) {
                            olBeaconManager.setForegroundBetweenScanPeriod(BeaconForegroundBetweenScanPeriod);
                            olBeaconManager.updateScanPeriods();
                         }
                    }

                    break;

                case OLBS_BEACON_BACKGROUND_SCAN_PERIOD:
                    BeaconBackgroundScanPeriod = value;
                    if ( olBeaconManager != null) {
                        if (olBeaconManager.isBound(this)) {
                            olBeaconManager.setBackgroundScanPeriod(BeaconBackgroundScanPeriod);
                            olBeaconManager.updateScanPeriods();
                         }
                    }

                    break;

                case OLBS_BEACON_FOREGROUND_SCAN_PERIOD:
                    BeaconForegroundScanPeriod = value;
                    if ( olBeaconManager != null) {
                        if (olBeaconManager.isBound(this)) {
                            olBeaconManager.setForegroundScanPeriod(BeaconForegroundScanPeriod);
                            olBeaconManager.updateScanPeriods();
                        }
                    }

                    break;

                case OLBS_LOCATION_UPDATE_INTERVAL_IN_MILLISECONDS:
                    LocationUpdateIntervalInMilliseconds = value;
                    if (mLocationRequest != null)
                    mLocationRequest.setInterval(LocationUpdateIntervalInMilliseconds);
                    break;

                case OLBS_LOCATION_FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS:
                    LocationFastestUpdateInMillisecond = value;
                    if (mLocationRequest != null)
                    mLocationRequest.setFastestInterval(LocationFastestUpdateInMillisecond);
                    break;

                case OLBS_HTTP_CONNECTION_TIMEOUT:
                    HttpConnectionTimeout = ((int) value);

                case OLBS_SOCKET_CONNECTION_TIMEOUT:
                    SocketConnectionTimeout = ((int) value);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[setOpenLobsterConfig]" +e.getMessage());

        }

    }

    public void setOpenLobsterConfig(OLBSConfigType olConfigType, String value )
    {
            switch (olConfigType){
                case OLBS_SCANLOG_URL:
                    ScanLogUrl = value;
                    break;

                case OLBS_SCANLOG_LOC_LABEL:
                    ScanLogLocLabel = value;
                    break;

                case OLBS_USER_TOKEN:
                    UserToken = value;
                    bSetUserTokenReady = true;
                    break;

                case OLBS_USER_API_KEY:
                    UserAPIkey = value;
                    break;
            }


 }



    public boolean SetBeaconScanningMethod(OLBSBeaconScanningMethodType beacon_scanning_method,boolean action)
    {

            switch (beacon_scanning_method){
                case OLBS_FIND_ALL_BEACON:
                    bFindAllBeacon = action;

//                    if (bFindNearestBeacon == true && bFindAllBeacon == true)
//                        bFindNearestBeacon = false;
                    break;


                case OLBS_FIND_NEAREST_BEACON:
                    bFindNearestBeacon = action;
//                    if (bFindAllBeacon == true && bFindNearestBeacon == false)
//                        bFindAllBeacon = false;
                    break;

                case OLBS_ESTIMATE_LOCATION:
                    bEstimateLocation = action;
                    break;


                case OLBS_BEACON_RANGING:

                    if (bBeaconRanging == true && action== false)
                    {
                        if (StopBeaconRanging() == false)
                            return false;

                    }

                    if (bBeaconRanging == false && action== true)
                    {
                        if (StartBeaconRanging() == false)
                            return false;

                    }
                    bBeaconRanging = action;
                    break;

                case OLBS_BEACON_MONITORING:
                    if (bBeaconMonitoring == true && action== false)
                    {
                        if (StopBeaconMonitoring() == false)
                            return false;
                    }

                    if (bBeaconMonitoring == false && action== true)
                    {
                        if (StartBeaconMonitoring() == false)
                            return false;
                    }
                    bBeaconMonitoring = action;
                    break;

            }

        return true;
    }



    public boolean addBeaconRegion(String region_id, String uuid, String major_id, String minor_id)
    {

        try {
            OLBSBeacon beacon = new OLBSBeacon(region_id,uuid,major_id,minor_id);


            if (RadarRegion.contains(beacon) == false)
            {
                RadarRegion.add(beacon);
                OLBSBeacon beacon_in_region = new OLBSBeacon(region_id,"0","0","0");
                LastBeaconInRegion.add(beacon_in_region);

                if(olBeaconManager != null && olBeaconManager.isBound(this)==true) {

                    Region radarRegion = new Region(region_id, beacon.UUID == null ? null : Identifier.parse(beacon.UUID ), beacon.major_id == null ? null : Identifier.parse(beacon.major_id), beacon.minor_id == null ? null : Identifier.parse(beacon.minor_id) );
                    Collection<Region> RangedRegion = olBeaconManager.getRangedRegions();

                    if (RangedRegion.contains(radarRegion) == false) {
                        olBeaconManager.startRangingBeaconsInRegion(radarRegion);
                        olBeaconManager.startMonitoringBeaconsInRegion(radarRegion);
                        return true;

                    }


                }

            }
            else {
                return false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[addBeaconRegion]" +e.getMessage());
            return false;

        }


        return false;
    }

    public boolean deleteBeaconRegion(String region_id)
    {

        try {

            Region radarRegion  = new Region(region_id,null,null,null);
            olBeaconManager.stopRangingBeaconsInRegion(radarRegion);
            olBeaconManager.stopMonitoringBeaconsInRegion(radarRegion);
            OLBSBeacon beacon = new OLBSBeacon(region_id,null,null,null);
            RadarRegion.remove(beacon);
            LastBeaconInRegion.remove(beacon);

            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "[deleteBeaconRegion]" + e.getMessage());
            return false;
        }

    }



//    protected boolean UpdateBeaconRegion()
//    {
//         if ( olBeaconManager != null){
//            if (olBeaconManager.isBound(this)) {
//                olBeaconManager.unbind(this);
//                olBeaconManager.bind(this);
//                return true;
//            }
//        }
//
//        return false;
//   }

    protected boolean StartBeaconMonitoring()
    {
        try {

            if (RadarRegion.isEmpty()) {


                OLBSBeacon beacon = new OLBSBeacon("ALL", null, null, null);
                if (RadarRegion.contains(beacon) == false) {
                    RadarRegion.add(beacon);
                }

                Region radarRegion = new Region("ALL", null, null, null);
                olBeaconManager.startMonitoringBeaconsInRegion(radarRegion);
            }
            else {

                Object[] RadarRegionArray = RadarRegion.toArray();
                for (int i = 0; i < RadarRegionArray.length; i++) {

                    Region radarRegion = new Region(((OLBSBeacon) RadarRegionArray[i]).region_id, ((OLBSBeacon) RadarRegionArray[i]).UUID == null ? null : Identifier.parse(((OLBSBeacon) RadarRegionArray[i]).UUID), ((OLBSBeacon) RadarRegionArray[i]).major_id == null ? null : Identifier.parse(((OLBSBeacon) RadarRegionArray[i]).major_id), ((OLBSBeacon) RadarRegionArray[i]).minor_id == null ? null : Identifier.parse(((OLBSBeacon) RadarRegionArray[i]).minor_id) );

                    Collection<Region> RangedRegion = olBeaconManager.getMonitoredRegions();

                    if (RangedRegion.contains(radarRegion) == false) {
                        olBeaconManager.startMonitoringBeaconsInRegion(radarRegion);
                    }
                }

            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "[StartBeaconMonitoring]" + e.getMessage());
            return false;
        }
    }

    protected boolean StopBeaconMonitoring()
    {

        return true;
    }

    protected boolean StartBeaconRanging()
    {

        try {
            //Region radarRegion;
            if (RadarRegion.isEmpty()) {


                OLBSBeacon beacon = new OLBSBeacon("ALL", null, null, null);
                if (RadarRegion.contains(beacon) == false) {
                    RadarRegion.add(beacon);
                }

                Region radarRegion = new Region("ALL", null, null, null);
                olBeaconManager.startRangingBeaconsInRegion(radarRegion);
            }
            else {

                Object[] RadarRegionArray = RadarRegion.toArray();
                for (int i = 0; i < RadarRegionArray.length; i++) {

                    Region radarRegion = new Region(((OLBSBeacon) RadarRegionArray[i]).region_id, ((OLBSBeacon) RadarRegionArray[i]).UUID == null ? null : Identifier.parse(((OLBSBeacon) RadarRegionArray[i]).UUID), ((OLBSBeacon) RadarRegionArray[i]).major_id == null ? null : Identifier.parse(((OLBSBeacon) RadarRegionArray[i]).major_id), ((OLBSBeacon) RadarRegionArray[i]).minor_id == null ? null : Identifier.parse(((OLBSBeacon) RadarRegionArray[i]).minor_id) );

                    Collection<Region> RangedRegion = olBeaconManager.getRangedRegions();

                    if (RangedRegion.contains(radarRegion) == false) {
                        olBeaconManager.startRangingBeaconsInRegion(radarRegion);
                    }
                }

            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "[StartBeaconRanging]" + e.getMessage());
            return false;
        }
    }

    protected boolean StopBeaconRanging()
    {
     return true;

    }

    public void onCreate() {
        olSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        olSensorManager.registerListener(this, olSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);

        olBeaconManager = BeaconManager.getInstanceForApplication(mContext);
        olBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        olBeaconManager.setBackgroundBetweenScanPeriod(BeaconBackgroundBetweenScanPeriod);
        olBeaconManager.setBackgroundScanPeriod(BeaconBackgroundScanPeriod);
        olBeaconManager.setForegroundBetweenScanPeriod(BeaconForegroundBetweenScanPeriod);
        olBeaconManager.setForegroundScanPeriod(BeaconForegroundScanPeriod);
        olBeaconManager.bind(this);


        buildGoogleApiClient();
        mGoogleApiClient.connect();

        //UserToken = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

    }

    public void onDestroy() {

        olSensorManager.unregisterListener(this);
        olBeaconManager.unbind(this);
        stopLocationUpdates();


    }

    public  void onPause() {

        olSensorManager.unregisterListener(this);
        if (olBeaconManager.isBound(this))
            olBeaconManager.setBackgroundMode(true);
    }

    public  void onResume() {
        olSensorManager.registerListener(this, olSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
        if (olBeaconManager.isBound(this))
            olBeaconManager.setBackgroundMode(false);

    }

    @Override
    public void onSensorChanged(SensorEvent sensor_event) {

        //pass the event to developer callback
         olbs_location.sensor_event = sensor_event;
         ole.OLBSonEventNotify(OLBSEventType.OLBS_SENSOR, olbs_location);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    @Override
    public void onBeaconServiceConnect() {


        olBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                if (bBeaconMonitoring == true) {
                    Log.i(TAG, "I just saw an beacon for the first time!");
                }
             }

            @Override
            public void didExitRegion(Region region) {
                if (bBeaconMonitoring == true) {
                    Log.i(TAG, "I no longer see an beacon");
                }
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                if (bBeaconMonitoring == true) {
                    Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
                }
             }
        });

        olBeaconManager.setRangeNotifier(new RangeNotifier() {

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                boolean bRunEstimateLocation = true;

                olbs_location.indoor_pos.clear();

                if (bBeaconRanging == true) {
                    olbs_location.session_id = session_id++;

                    if (beacons.size() == 0) {
                        Log.e(TAG, "[SID:" + olbs_location.session_id + "]" +  "No beacon detected (" + region.getUniqueId() + ")\n");

                        Iterator<OLBSBeacon> iter = LastBeaconInRegion.iterator();
                        while (iter.hasNext()) {
                            OLBSBeacon LastBeacon = iter.next();
                            OLBSBeacon CurrentRegion = new OLBSBeacon(region.getUniqueId());
                            if( LastBeacon.equals(CurrentRegion) == true)
                            {
                                LastBeacon.clear();
                                LastBeacon.UUID = "0";
                                LastBeacon.major_id = "0";
                                LastBeacon.minor_id = "0";
                                break;
                            }
                        }


                        ole.OLBSonEventNotify(OLBSEventType.OLBS_NO_BEACON_IN_REGION, olbs_location);

                    } else {

                        olbs_location.beacons_last_update_time = DateFormat.getTimeInstance().format(new Date());
                        Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "Beacon last detected time - " + olbs_location.beacons_last_update_time);


                        Object[] beaconArray = beacons.toArray();

                        olbs_location.beacons.clear();

                        //Find ALL beacons in region ///////////////////////////////////////
                        if (bFindAllBeacon == true) {


                            for (int i = 0; i < beaconArray.length; i++) {


                                OLBSBeacon beacon = new OLBSBeacon();
                                beacon.UUID = ((Beacon) beaconArray[i]).getId1().toString().replace("-", "").toUpperCase();
                                beacon.major_id = ((Beacon) beaconArray[i]).getId2().toString();
                                beacon.minor_id = ((Beacon) beaconArray[i]).getId3().toString();

                                olbs_location.beacons.add(beacon);

                            }
                            Log.e(TAG, "[SID:" + olbs_location.session_id + "]" +  + beaconArray.length + " beacon detected (" + region.getUniqueId() + ")\n");
                            ole.OLBSonEventNotify(OLBSEventType.OLBS_ALL_BEACON_IN_REGION, olbs_location);
                        }
                        /////////////////////////////////////////////////////////////////

                        // Find beacon with smallest distance ////////////////////////////////////////////
                        if (bFindNearestBeacon == true) {
                            String minID1 = "", minID2 = "0", minID3 = "0";
                            double minDistance = Double.MAX_VALUE;
                            int maxDB = Integer.MIN_VALUE;
                            for (Beacon b : beacons) {
                                // if (b.getDistance() < minDistance) {
                                //minDistance = b.getDistance();
                                if (b.getRssi() + b.getTxPower() > maxDB) {
                                    maxDB = b.getRssi() + b.getTxPower();
                                    minID1 = b.getId1().toString();
                                    minID2 = b.getId2().toString();
                                    minID3 = b.getId3().toString();
                                }
                            }
                            //if (minDistance < Double.MAX_VALUE) {

                            OLBSBeacon beacon = new OLBSBeacon();
                            beacon.UUID = minID1.replace("-", "").toUpperCase();
                            beacon.major_id = minID2;
                            beacon.minor_id = minID3;

                            olbs_location.beacons.clear();
                            olbs_location.beacons.add(beacon);

                            Log.e(TAG, "[SID:" + olbs_location.session_id + "]" +  + beaconArray.length + " beacon detected (" + region.getUniqueId() + ")\n");
                            ole.OLBSonEventNotify(OLBSEventType.OLBS_NEAREST_BEACON_IN_REGION, olbs_location);

                            Iterator<OLBSBeacon> iter = LastBeaconInRegion.iterator();
                            while (iter.hasNext()) {
                                OLBSBeacon LastBeacon = iter.next();
                                OLBSBeacon CurrentRegion = new OLBSBeacon(region.getUniqueId());
                                if( LastBeacon.equals(CurrentRegion) == true)
                                {
                                    OLBSBeacon CurrentBeacon = new OLBSBeacon(region.getUniqueId(),beacon.UUID,beacon.major_id,beacon.minor_id );
                                    if (LastBeacon.BeaconEqual(CurrentBeacon)) {
                                        bRunEstimateLocation = false;
                                        Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[onBeaconServiceConnect]" + "Last Beacon");
                                    }
                                    else
                                    {
                                        Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[onBeaconServiceConnect]" + "New Beacon");
                                        LastBeacon.UUID = beacon.UUID;
                                        LastBeacon.major_id = beacon.major_id;
                                        LastBeacon.minor_id = beacon.minor_id;
                                    }
                                    break;
                                }
                            }


                            // }
                        }

                        /////////////////////////////////////////////////////////////////////////////////////////

                        // Update Scanlog data /////////////////////////////////////////////////////////////////////////////////////////////////////////
                        if (bEstimateLocation == true && bRunEstimateLocation == true &&  bSetUserTokenReady == true){
                            try {

                                JSONObject ScanLogParent;
                                JSONArray ScanLogArray;

                                ScanLogParent = new JSONObject();
                                ScanLogArray = new JSONArray();



                               // double temp = mCurrentLocation.getLongitude();

                                // Object[] beaconArray = beacons.toArray();
                                ScanLogParent.accumulate("OLBSAPIKey", "6375686B2E6564752E686B2E30303031");
                                ScanLogParent.accumulate("SMode", 1);
                                ScanLogParent.accumulate("UserToken", UserToken);
                                ScanLogParent.accumulate("DeviceID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
                                ScanLogParent.accumulate("DeviceOS", 1);
                               ScanLogParent.accumulate("Longitude", mCurrentLocation == null?-999:mCurrentLocation.getLongitude());
                                ScanLogParent.accumulate("Latitude", mCurrentLocation == null?-999:mCurrentLocation.getLatitude());
                                ScanLogParent.accumulate("Altitude", mCurrentLocation == null?-999:mCurrentLocation.getAltitude());
                                ScanLogParent.accumulate("NumBeacon", beaconArray.length);
                                ScanLogParent.accumulate("LocLabel", ScanLogLocLabel);


                                ScanLogArray = new JSONArray();

                                for (int i = 0; i < beaconArray.length; i++) {


                                    String UUID_string = new String("");
                                    UUID_string = UUID_string + ((Beacon) beaconArray[i]).getId1();

                                    String UUID;

                                    UUID = UUID_string.substring(0, 8);
                                    UUID += UUID_string.substring(9, 13);
                                    UUID += UUID_string.substring(14, 18);
                                    UUID += UUID_string.substring(19, 23);
                                    UUID += UUID_string.substring(24, 36);

                                    String majorid_string = new String("");
                                    majorid_string = majorid_string + ((Beacon) beaconArray[i]).getId2();

                                    String minorid_string = new String("");
                                    minorid_string = minorid_string + ((Beacon) beaconArray[i]).getId3();


                                    final int majorid = Integer.parseInt(majorid_string);
                                    final int minorid = Integer.parseInt(minorid_string);

                                    final int txpower = ((Beacon) beaconArray[i]).getTxPower();
                                    final int rssi = ((Beacon) beaconArray[i]).getRssi();

                                    final String BName = ((Beacon) beaconArray[i]).getBluetoothName();
                                    final String BMac = ((Beacon) beaconArray[i]).getBluetoothAddress();

                                    JSONObject jsonObj = new JSONObject();

                                    jsonObj.put("BUUID", UUID);
                                    jsonObj.put("Major", majorid);
                                    jsonObj.put("Minor", minorid);
                                    jsonObj.put("Tx", txpower);
                                    jsonObj.put("RSSI", rssi);
                                    jsonObj.put("BName", BName);
                                    jsonObj.put("BMac", BMac.replace(":", "").toUpperCase());

                                    ScanLogArray.put(jsonObj);
                                }

                                ScanLogParent.put("ScanDetail", ScanLogArray);


                                //Send UpdateScanLog request
                                EstimateLocation(ScanLogParent.toString());
                                //                            ScanLogParent = new JSONObject();
                                //                            ScanLogArray = new JSONArray();

                            } catch (Exception e) {
                                Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[onBeaconServiceConnect]" + e.getMessage());
                            }
                        }
                        ////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }
                }
            }
        });


        if (bBeaconRanging == true) {
            StartBeaconRanging();
        }

        if (bBeaconMonitoring == true)
        {
            StartBeaconMonitoring();
        }

    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection sc, int state) {
        return mContext.bindService(intent, sc, state);
    }

    @Override
    public void unbindService(ServiceConnection SC) {
        // not implemented yet
        mContext.unbindService(SC);
    }

    protected synchronized void buildGoogleApiClient() {

        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LocationUpdateIntervalInMilliseconds);
        mLocationRequest.setFastestInterval(LocationFastestUpdateInMillisecond);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);


    }


    protected void stopLocationUpdates() {
         LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "olonConnected");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

         }


            startLocationUpdates();



    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i(TAG, "olonLocationChanged");
      mCurrentLocation = location;
        olbs_location.gps_loc_last_update_time = DateFormat.getTimeInstance().format(new Date());
        Log.e(TAG, "GPS last updated time - " + olbs_location.gps_loc_last_update_time);
      olbs_location.gps_loc = location;

        ole.OLBSonEventNotify(OLBSEventType.OLBS_GPS, olbs_location);


    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {


    }


    public void GetOlbsPath(String from_luuid, String to_luuid)
    {
        try
        {
            // String file_url = "";
            JSONObject OlbsPathJasonString;


           OlbsPathJasonString = new JSONObject();


            // Object[] beaconArray = beacons.toArray();
            OlbsPathJasonString.accumulate("OLBSAPIKey", UserAPIkey);
            OlbsPathJasonString.accumulate("FromLUUID", from_luuid);
            OlbsPathJasonString.accumulate("ToLUUID", to_luuid);


            new HTTPGetGetOlbsPathPostJason().execute(GetOlbsPathUrl, OlbsPathJasonString.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[GetFloorMap]" + e.getMessage());
        }
     }

    class HTTPGetGetOlbsPathPostJason extends AsyncTask<String, String, String> {

        // Show Progress bar before
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();
                HttpParams params = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, HttpConnectionTimeout);
                HttpConnectionParams.setSoTimeout(params, SocketConnectionTimeout);

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(f_url[0].toString());

                String json = "";


                // 4. convert JSONObject to JSON to String
                //json = ScanLogParent.toString();
                json = f_url[1].toString();
//                ScanLogParent = new JSONObject();


                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity


                // 7. Set some headers to inform server about the type of the content
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                httpPost.setEntity(se);

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);



                // 9. receive response as inputStream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader((httpResponse.getEntity().getContent())));

                String output;


                while ((output = br.readLine()) != null) {
                    //JSONObject obj = new JSONObject(output);

                    JSONArray mJsonArray = new JSONArray(output);
                    
                    JSONObject mJsonObject = new JSONObject();



                    olbs_location.olbs_path.clear();

                    for (int i = 0; i < mJsonArray.length(); i++) {
                        mJsonObject = mJsonArray.getJSONObject(i);
                        OLBSPath OlbsPathObject = new OLBSPath();

                        OlbsPathObject.from_luuid = mJsonObject.getString("FromLUUID");
                        OlbsPathObject.from_f_map_uuid =  mJsonObject.getString("FromFMapUUID");
                        OlbsPathObject.from_floor_seq =  mJsonObject.getInt("FromFloorSeq");
                        OlbsPathObject.from_floor_label = (mJsonObject.getString("FromFloorLabel"));
                        OlbsPathObject.from_room =  mJsonObject.getString("FromRoom");
                        OlbsPathObject.from_ux = ((float) mJsonObject.getDouble("Fromux"));
                        OlbsPathObject.from_uy = ((float) mJsonObject.getDouble("Fromuy"));
                        OlbsPathObject.to_luuid = mJsonObject.getString("ToLUUID");
                        OlbsPathObject.to_f_map_uuid =  mJsonObject.getString("ToFMapUUID");
                        OlbsPathObject.to_floor_seq =  mJsonObject.getInt("ToFloorSeq");
                        OlbsPathObject.to_floor_label =  mJsonObject.getString("ToFloorLabel");
                        OlbsPathObject.to_room =  mJsonObject.getString("ToRoom");
                        OlbsPathObject.to_ux = ((float) mJsonObject.getDouble("Toux"));
                        OlbsPathObject.to_uy = ((float) mJsonObject.getDouble("Touy"));

                        olbs_location.olbs_path.add(OlbsPathObject);
                     
                    }


//
                    ole.OLBSonEventNotify(OLBSEventType.OLBS_GET_OLBS_PATH, olbs_location);
                    Log.i("[SID:" + olbs_location.session_id + "]" + "GetOlbsPath: ", output);
                }



            } catch (Exception e) {
                Log.e(TAG,"[SID:" + olbs_location.session_id + "]" +  "[GetOlbsPath]" + e.getMessage());
                olbs_location.error = e;
                ole.OLBSonEventNotify(OLBSEventType.OLBS_GET_OLBS_PATH, olbs_location);
            }
            return null;
        }

        // While Progressing
        protected void onProgressUpdate(String... progress) {
            // Set progress percentage

        }

        // Once findished
        @Override
        protected void onPostExecute(String file_url) {
            Log.i("[SID:" + olbs_location.session_id + "]" + "GetFloorMap: ", "onPostExeute called");

        }
    }

    public void GetFloorMap(String f_map_uuid)
    {
        try
        {
            // String file_url = "";
            JSONObject FloorMapJasonString;


            FloorMapJasonString = new JSONObject();


            // Object[] beaconArray = beacons.toArray();
            FloorMapJasonString.accumulate("OLBSAPIKey", UserAPIkey);

            String floor_map_url_string = GetFloorMapUrl + f_map_uuid;
            new HTTPGetFloorMapPostJason().execute(floor_map_url_string, FloorMapJasonString.toString());
        }
        catch (Exception e) {
            Log.e(TAG,"[SID:" + olbs_location.session_id + "]" +  "[GetFloorMap]" + e.getMessage());
        }

    }

    class HTTPGetFloorMapPostJason extends AsyncTask<String, String, String> {

        // Show Progress bar before
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();
                HttpParams params = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, HttpConnectionTimeout);
                HttpConnectionParams.setSoTimeout(params, SocketConnectionTimeout);

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(f_url[0].toString());

                String json = "";


                // 4. convert JSONObject to JSON to String
                //json = ScanLogParent.toString();
                json = f_url[1].toString();
//                ScanLogParent = new JSONObject();


                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity


                // 7. Set some headers to inform server about the type of the content
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                httpPost.setEntity(se);

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);



                // 9. receive response as inputStream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader((httpResponse.getEntity().getContent())));

                String output;


                while ((output = br.readLine()) != null) {
                    JSONObject obj = new JSONObject(output);
//                    olbs_location.floor_map.last_f_map_uuid = olbs_location.floor_map.f_map_uuid;
                    olbs_location.floor_map.f_map_uuid = obj.getString("FMapUUID");
                    olbs_location.floor_map.f_map_url = obj.getString("FMapURI");
                    olbs_location.floor_map.longitude_a = Float.parseFloat(obj.getString("LongitudeA"));
                    olbs_location.floor_map.latitude_a = Float.parseFloat(obj.getString("LatitudeA"));
                    olbs_location.floor_map.ux_a = Float.parseFloat(obj.getString("uxA"));
                    olbs_location.floor_map.uy_a = Float.parseFloat(obj.getString("uyA"));
                    olbs_location.floor_map.bearing_a = Float.parseFloat(obj.getString("BearingA"));
                    olbs_location.floor_map.scale_xa = Float.parseFloat(obj.getString("ScaleXA"));
                    olbs_location.floor_map.scale_ya = Float.parseFloat(obj.getString("ScaleYA"));
                    olbs_location.floor_map.Longitude_m1 = Float.parseFloat(obj.getString("LongitudeM1"));
                    olbs_location.floor_map.Latitude_m1 = Float.parseFloat(obj.getString("LatitudeM1"));
                    olbs_location.floor_map.ux_m1 = Float.parseFloat(obj.getString("uxM1"));
                    olbs_location.floor_map.uy_m1 = Float.parseFloat(obj.getString("uyM1"));
                    olbs_location.floor_map.Longitude_m2 = Float.parseFloat(obj.getString("LongitudeM2"));
                    olbs_location.floor_map.Latitude_m2 = Float.parseFloat(obj.getString("LatitudeM2"));
                    olbs_location.floor_map.ux_m2 = Float.parseFloat(obj.getString("uxM2"));
                    olbs_location.floor_map.uy_m2 = Float.parseFloat(obj.getString("uyM2"));

                    if (olbs_location.floor_map.f_map_url !=null)
                        new LoadFloorBitMapFromUrl().execute("null");

                    //ole.OLBSonEventNotify(OLBSEventType.OLBS_GET_FLOOR_MAP, olbs_location);

                    Log.i("[SID:" + olbs_location.session_id + "]" + "GetFloorMap: ", output);
                }



            } catch (Exception e) {
                Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[GetFloorMap]" + e.getMessage());

            }
            return null;
        }

        // While Progressing
        protected void onProgressUpdate(String... progress) {
            // Set progress percentage

        }

        // Once findished
        @Override
        protected void onPostExecute(String file_url) {
            Log.i("[SID:" + olbs_location.session_id + "]" + "GetFloorMap: ", "onPostExeute called");

        }
    }

    protected void EstimateLocation(String beacon_json_string)
    {
        try
        {
        // String file_url = "";
            new HTTPPostJason().execute(beacon_json_string);
        }
        catch (Exception e) {
            Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[GetFloorMap]" + e.getMessage());
        }

    }


    public boolean SendMSG2UserAtLocationWithWebCallBack(String luuid, String msg)
    {
        try
        {
            // String file_url = "";

            olbs_location.msg_sender.msg = msg;
            olbs_location.msg_sender.luuid = luuid;


            JSONObject SendMSG2UserAtLocationWithWebCallBackJasonString;


            SendMSG2UserAtLocationWithWebCallBackJasonString = new JSONObject();


            // Object[] beaconArray = beacons.toArray();
            SendMSG2UserAtLocationWithWebCallBackJasonString.accumulate("OLBSAPIKey", UserAPIkey);
            SendMSG2UserAtLocationWithWebCallBackJasonString.accumulate("LUUID", olbs_location.msg_sender.luuid);
            SendMSG2UserAtLocationWithWebCallBackJasonString.accumulate("UserToken", UserToken);
            SendMSG2UserAtLocationWithWebCallBackJasonString.accumulate("MSG", olbs_location.msg_sender.msg);


            new HTTPSendMSG2UserAtLocationPostJason().execute(SendMSG2UserAtLocationWithWebCallBackJasonString.toString());

            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "[SID:" + olbs_location.session_id + "]" + "[SendMSG2UserAtLocationWithWebCallBack]" + e.getMessage());
        }

        return false;
    }

    // Async Task Class
    class HTTPSendMSG2UserAtLocationPostJason extends AsyncTask<String, String, String> {

        // Show Progress bar before
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {



                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                HttpParams params = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, HttpConnectionTimeout);
                HttpConnectionParams.setSoTimeout(params, SocketConnectionTimeout);

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(SendMSG2UserAtLocationWithWebCallBackUrl);

                String json = "";


                // 4. convert JSONObject to JSON to String
                //json = ScanLogParent.toString();
                json = f_url[0].toString();
//                ScanLogParent = new JSONObject();


                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity


                // 7. Set some headers to inform server about the type of the content
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                httpPost.setEntity(se);

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);



                // 9. receive response as inputStream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader((httpResponse.getEntity().getContent())));

                String output;


                while ((output = br.readLine()) != null) {
                    JSONObject obj = new JSONObject(output);



                    olbs_location.msg_sender.num_users = Integer.parseInt(obj.getString("NumUsers"));

                    ole.OLBSonEventNotify(OLBSEventType.OLBS_SEND_MSG_2_USER_AT_LOCATION_WITH_WEB_CALLBACK, olbs_location);

                    Log.e("[SID:" + olbs_location.session_id + "] " + "SendMSG2UserAtLocationWithWebCallBack: ", output);
                }



            } catch (Exception e) {
                Log.i(TAG, "[SID:" + olbs_location.session_id + "]" + "[doInBackground]" + e.getMessage());
                //olbs_location.indoor_pos.luuid = null;
                olbs_location.error = e;
                ole.OLBSonEventNotify(OLBSEventType.OLBS_SEND_MSG_2_USER_AT_LOCATION_WITH_WEB_CALLBACK, olbs_location);
            }
            return null;
        }

        // While Progressing
        protected void onProgressUpdate(String... progress) {
            // Set progress percentage

        }

        // Once findished
        @Override
        protected void onPostExecute(String file_url) {
            Log.i("[SID:" + olbs_location.session_id + "]" + "Beacon: ", "onPostExeute called");

        }
    }


    // Async Task Class
    class HTTPPostJason extends AsyncTask<String, String, String> {

        // Show Progress bar before
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {



                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                HttpParams params = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, HttpConnectionTimeout);
                HttpConnectionParams.setSoTimeout(params, SocketConnectionTimeout);

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(ScanLogUrl);

                String json = "";


                // 4. convert JSONObject to JSON to String
                //json = ScanLogParent.toString();
                json = f_url[0].toString();
//                ScanLogParent = new JSONObject();


                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity


                // 7. Set some headers to inform server about the type of the content
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                httpPost.setEntity(se);

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);



                // 9. receive response as inputStream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader((httpResponse.getEntity().getContent())));

                String output;


                while ((output = br.readLine()) != null) {
                    JSONObject obj = new JSONObject(output);

                    olbs_location.indoor_pos.last_floor_map_uuid =  olbs_location.indoor_pos.floor_map_uuid;

                    olbs_location.indoor_pos.luuid = obj.getString("LUUID");
                    olbs_location.indoor_pos.organization = obj.getString("Organization");
                    olbs_location.indoor_pos.building = obj.getString("Building");
                    olbs_location.indoor_pos.floor_seq = Integer.parseInt(obj.getString("FloorSeq"));
                    olbs_location.indoor_pos.floor_label = obj.getString("FloorLabel");
                    olbs_location.indoor_pos.room = obj.getString("Room");
                    olbs_location.indoor_pos.floor_map_uuid= obj.getString("FMapUUID");
                    olbs_location.indoor_pos.ux = Float.parseFloat(obj.getString("ux"));
                    olbs_location.indoor_pos.uy = Float.parseFloat(obj.getString("uy"));
                    ole.OLBSonEventNotify(OLBSEventType.OLBS_ESTIMATE_LOCATION, olbs_location);

                    Log.e("[SID:" + olbs_location.session_id + "] " + "EstimateLocation: ", output);
                }



            } catch (Exception e) {
                Log.i(TAG, "[SID:" + olbs_location.session_id + "]" + "[doInBackground]" + e.getMessage());
                //olbs_location.indoor_pos.luuid = null;
                olbs_location.error = e;
                ole.OLBSonEventNotify(OLBSEventType.OLBS_ESTIMATE_LOCATION, olbs_location);
            }
            return null;
        }

        // While Progressing
        protected void onProgressUpdate(String... progress) {
            // Set progress percentage

        }

        // Once findished
        @Override
        protected void onPostExecute(String file_url) {
            Log.i("[SID:" + olbs_location.session_id + "]" + "Beacon: ", "onPostExeute called");

        }
    }

    class LoadFloorBitMapFromUrl extends AsyncTask <String,String,String>
    {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();

        }
        @Override
        protected String doInBackground(String... f_url) {
            URL url ;
            try {
                url = new URL("http://" + olbs_location.floor_map.f_map_url);
                olbs_location.floor_map.floor_map_bmp  = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                olbs_location.error = e;

            }
            ole.OLBSonEventNotify(OLBSEventType.OLBS_GET_FLOOR_MAP, olbs_location);
            return null;

        }
        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);

            Log.i("[SID:" + olbs_location.session_id + "]" + "LoadFloorBitMapFromUrl: ", "onPostExeute called");
//            source = mMap.addMarker(new MarkerOptions()
//                    .position(sc)
//                    .title("MyHome")
//                    .snippet("Bangalore")
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));
        }
    }

}

