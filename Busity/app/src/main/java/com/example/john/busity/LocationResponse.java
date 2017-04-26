package com.example.john.busity;

/**
 * Created by John on 4/23/2017.
 */


public class LocationResponse {
    private double lat;
    private double longi;

    public LocationResponse() {

    }
    public LocationResponse(double lat, double longi) {
        this.lat = lat;
        this.longi = longi;
    }
    public double getLat() {
        return lat;
    }
    public double getLongi() {
        return longi;
    }
}