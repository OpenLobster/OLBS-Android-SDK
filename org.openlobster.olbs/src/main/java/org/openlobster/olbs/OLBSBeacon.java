package org.openlobster.olbs;

/**
 * Created by view on 10/6/15.
 */


public class OLBSBeacon
{
    public String region_id;
    public String UUID;
    public String major_id, minor_id;

    public OLBSBeacon(){
        region_id = "ALL";
        UUID = null;
        major_id = null;
        minor_id =null;
    };

    public OLBSBeacon(String regionid)
    {
        region_id = regionid;

    }

    public OLBSBeacon(String uuid, String majorid, String minorid){
        UUID = null;
        major_id = majorid;
        minor_id = minorid;
    };

    public OLBSBeacon(String regionid, String uuid, String majorid, String minorid){

        region_id = regionid;
        UUID = uuid;
        major_id = majorid;
        minor_id = minorid;
    };

    public void clear()
    {
        UUID = "0";
        major_id = "0";
        minor_id = "0";

    }

    @Override
    public boolean equals(Object object) {
        boolean Same = false;

        if (object != null && object instanceof OLBSBeacon) {
            if ( this.region_id.equals(((OLBSBeacon) object).region_id) )
            {
                Same = true;


            }
//            if ((this.region_id == ((OLBSBeacon) object).region_id) &&
//                    (this.UUID == ((OLBSBeacon) object).UUID) &&
//                    (this.major_id == ((OLBSBeacon) object).major_id) &&
//                    (this.minor_id == ((OLBSBeacon) object).major_id)){
//                Same = true;
//            }
        }

        return Same;
    }

    public boolean BeaconEqual(OLBSBeacon object)
    {
        boolean Same = false;

        if ((this.UUID.equals(((OLBSBeacon) object).UUID)) &&
                (this.major_id.equals(((OLBSBeacon) object).major_id)) &&
                (this.minor_id.equals(((OLBSBeacon) object).minor_id))){
            Same = true;
        }
//        if ((this.UUID.equals(((OLBSBeacon) object).UUID))){
//            Same = true;
//        }
        return Same;
    }

}