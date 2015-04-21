package com.example.matej.wifreemaps;

import com.google.android.gms.maps.model.LatLng;

import java.security.Timestamp;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Matej on 20.4.2015.
 */
public class AccessPoint {

    private int maxMeasurements=30;

    private String BSSID;
    private String SSID;
    private int frequency;
    private String region;
    private String security;
    private int internetAvailable;
    private LatLng GPSLocation;

    private HashMap<LatLng,PrecisionMeasurePoint> measurePoints;

    public AccessPoint(){
        this.measurePoints=new HashMap<LatLng,PrecisionMeasurePoint>();
    }
    public AccessPoint(String bssid, String ssid, int frequency, String region, String security, int internetavailable, LatLng gpslocation, long timestamp){
        this.BSSID=bssid;
        this.SSID=ssid;
        this.frequency=frequency;
        this.region=region;
        this.security=security;
        this.internetAvailable=internetavailable;
        this.GPSLocation=gpslocation;
        this.measurePoints=new HashMap<LatLng,PrecisionMeasurePoint>();
        PrecisionMeasurePoint first=new PrecisionMeasurePoint((float)1.0,1,timestamp);
        measurePoints.put(gpslocation, first);
    }

    public void addNewMeasurement(LatLng position, float signalStrength, int GPSprecision, long timestamp){
        PrecisionMeasurePoint p= new PrecisionMeasurePoint(signalStrength,GPSprecision, timestamp);
        measurePoints.put(position,p);

        if(measurePoints.size() > maxMeasurements)
        {
           Map.Entry<LatLng,PrecisionMeasurePoint> minEntry=null;
           for(Map.Entry<LatLng,PrecisionMeasurePoint> entry:measurePoints.entrySet()){
               if(minEntry==null || entry.getValue().returnQuality() < minEntry.getValue().returnQuality())
                   minEntry = entry;
           }
            //delete worst point
            measurePoints.remove(minEntry.getKey());
        }
    }

    private class PrecisionMeasurePoint{
        float signalStrength;
        int gpsAccuracy;
        long timestamp;

        public PrecisionMeasurePoint(){}

        public PrecisionMeasurePoint(float signalStrength, int gpsAccuracy, long timestamp){
            this.signalStrength = signalStrength;
            this.gpsAccuracy = gpsAccuracy;
            this.timestamp = timestamp;
        }
        public float returnQuality(){
            return this.signalStrength/this.gpsAccuracy;
        }
        public long returnTimestamp(){
            return this.timestamp;
        }
    };

    public LatLng getGPSLocation(){
        return GPSLocation;
    }

    public List<MarkerPoint> getMarkings() {
        List<MarkerPoint> markings = new ArrayList<MarkerPoint>();
        for (Map.Entry<LatLng, PrecisionMeasurePoint> entry : measurePoints.entrySet())
        {
            markings.add(new MarkerPoint(entry.getKey(),entry.getValue().gpsAccuracy,entry.getValue().signalStrength));
        }

        return markings;
    }

    //TODO: set markings?

}
