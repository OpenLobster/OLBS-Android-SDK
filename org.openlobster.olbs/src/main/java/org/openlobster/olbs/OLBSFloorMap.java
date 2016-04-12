package org.openlobster.olbs;
import android.graphics.Bitmap;
/**
 * Created by view on 16/6/15.
 */
public class OLBSFloorMap {

//    public String last_f_map_uuid;
    public String f_map_uuid;
    public String f_map_url;
  //  public String m_uuid;
    public float longitude_a;
    public float latitude_a;
    public float ux_a;
    public float uy_a;
    public float bearing_a;
    public float scale_xa;
    public float scale_ya;
    public float Longitude_m1;
    public float Latitude_m1;
    public float ux_m1;
    public float uy_m1;
    public float Longitude_m2;
    public float Latitude_m2;
    public float ux_m2;
    public float uy_m2;

    public Bitmap floor_map_bmp;

    public void OLBSFloorMap() {

    }

    public boolean IsFloorMapValid()
    {
        if (f_map_uuid == null)
            return false;
        else
            return true;

    }

//    public boolean IsFloorMapLoaded()
//    {
//        if (last_f_map_uuid.equals(f_map_uuid))
//        {
//            return true;
//
//        }
//        else
//            return false;
//
//    }


    public float lx(float ux)
    {
        float lx ;
        lx = Latitude_m2 - (Latitude_m2 - Latitude_m1) * (ux_m2 - ux) /
                (ux_m2 - ux_m1);
        return lx;
    }

    public float ly(float uy)
    {
        float ly ;
        ly = Longitude_m2 - (Longitude_m2 - Longitude_m1) * (uy_m2 - uy) /
                (uy_m2 - uy_m1);
        return ly;
    }
}
