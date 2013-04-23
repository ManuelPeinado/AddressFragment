package com.manuelpeinado.addressfragment.demo;

import android.location.Location;
import android.os.Handler;

import com.google.android.gms.maps.LocationSource;

public class MockLocationSource implements LocationSource {

    private OnLocationChangedListener mListener;
    private long mInitTime;
    private static final Location START_LOCATION = Utils.newLocation(40.41718, -3.70307);
    private static final Location END_LOCATION = Utils.newLocation(40.41999, -3.70151);
    private static final int TOTAL_TIME = 20 * 1; // In seconds
    private static final int PERIOD = 1000; // 1 second

    @Override
    public void activate(OnLocationChangedListener listener) {
        this.mListener = listener;
        mInitTime = System.currentTimeMillis() / 1000;
        listener.onLocationChanged(START_LOCATION);
        scheduleNext();
    }

    private void scheduleNext() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                if (mListener == null) {
                    return;
                }
                double elapsed = System.currentTimeMillis() / 1000 - mInitTime;
                double t = Math.min(elapsed / TOTAL_TIME, 1);
                double deltaLat = END_LOCATION.getLatitude() - START_LOCATION.getLatitude();
                double deltaLon = END_LOCATION.getLongitude() - START_LOCATION.getLongitude();
                double lat = START_LOCATION.getLatitude() + deltaLat * t;
                double lon = START_LOCATION.getLongitude() + deltaLon * t;
                mListener.onLocationChanged(Utils.newLocation(lat, lon));
                if (t < 1) {
                    scheduleNext();
                }   
            }
        }, PERIOD);
    }

    @Override
    public void deactivate() {
        mListener = null;
    }

}
