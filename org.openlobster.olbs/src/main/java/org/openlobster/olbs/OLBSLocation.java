package org.openlobster.olbs;

import android.hardware.SensorEvent;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by view on 10/6/15.
 */


public class OLBSLocation {
    public Location gps_loc;
    public  SensorEvent sensor_event;
    public OLBSIndoor indoor_pos;
    public OLBSFloorMap floor_map;
    public Collection<OLBSBeacon> beacons;
    public Collection<OLBSPath> olbs_path;
    public OLBSMsgSender msg_sender;

    public String gps_loc_last_update_time;
    public String beacons_last_update_time;
    public Exception error;
    public int session_id;
    //NFC

    public OLBSLocation()
    {
        beacons = new ArrayList<OLBSBeacon>();
        indoor_pos = new OLBSIndoor();
        floor_map = new OLBSFloorMap();
        olbs_path = new ArrayList<OLBSPath>();
        msg_sender = new OLBSMsgSender();
    }


}
