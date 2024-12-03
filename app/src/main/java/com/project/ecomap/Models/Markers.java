package com.project.ecomap.Models;

import com.google.android.gms.maps.model.Marker;

public class Markers {
    private Marker marker;
    private String markerUserId;
    private String markerId;

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public String getMarkerId() {
        return markerId;
    }

    public void setMarkerId(String markerId) {
        this.markerId = markerId;
    }

    public String getMarkerUserId() {
        return markerUserId;
    }

    public void setMarkerUserId(String markerUserId) {
        this.markerUserId = markerUserId;
    }
}
