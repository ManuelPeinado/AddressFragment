package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class SingleShotLocationTask implements LocationListener {
    
    private LocationManager mLocationManager;
    private SingleShotLocationListener mListener;

    public interface SingleShotLocationListener {
        void onLocationReady(SingleShotLocationTask singleShotLocationRequest, Location newLocation);
    }

    // TODO add min acccuracy parameter, check that there is one provider enabled, use timeout
    public SingleShotLocationTask(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void getLocation(SingleShotLocationListener listener) {
        this.mListener = listener;
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocationManager.removeUpdates(this);
        mListener.onLocationReady(this, location);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
