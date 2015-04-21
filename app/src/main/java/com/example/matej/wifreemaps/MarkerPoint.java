package com.example.matej.wifreemaps;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Matej on 20.4.2015.
 */
public class MarkerPoint {
    private LatLng location;
    private int gpsAccuracy;
    private float signalStrength;

    public MarkerPoint(){}
    public MarkerPoint(LatLng location, int gpsAccuracy, float signalStrength){
        this.location=location;
        this.gpsAccuracy=gpsAccuracy;
        this.signalStrength=signalStrength;
    }

    public LatLng getLocation(){
        return location;
    }
    public int getGpsAccuracy(){
        return gpsAccuracy;
    }
    public float getSignalStrength(){
        return signalStrength;
    }
}
