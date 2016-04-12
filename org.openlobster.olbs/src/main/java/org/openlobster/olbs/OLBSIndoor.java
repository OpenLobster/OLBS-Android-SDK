package org.openlobster.olbs;

/**
 * Created by view on 10/6/15.
 */

public class OLBSIndoor {
    public String luuid;
   public String room;
    public String building;
    public String organization;
    public String last_floor_map_uuid;
    public String floor_map_uuid;
    public String floor_label;
    public int floor_seq;
    public float ux;
    public float uy;

    public OLBSIndoor() {

        last_floor_map_uuid="";
        floor_map_uuid="";
        clear();
    }

    public boolean IsLuuidValid()
    {
        if (luuid == null)
            return    false;
        else
            return true;
    }

    public void clear()
    {

        luuid=null;
         organization=null;
        building=null;
        floor_seq=-1;
        floor_label=null;
        room=null;
       // last_floor_map_uuid="";
       // floor_map_uuid="";
        ux=-1;
        uy=-1;
    }

    public boolean IsFloorMapLoaded()
    {
        if (floor_map_uuid.equals(last_floor_map_uuid))
        {
            return true;

        }
        else
            return false;

    }


}