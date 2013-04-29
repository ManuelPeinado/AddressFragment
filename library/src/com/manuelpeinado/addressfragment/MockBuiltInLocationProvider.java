package com.manuelpeinado.addressfragment;

import android.location.Location;
import android.os.Handler;

import com.manuelpeinado.addressfragment.AddressView.LocationProvider;



public class MockBuiltInLocationProvider implements LocationProvider {

    private AddressView mAddressView;
    private long mInitTime;
    private Location mLastLocation = START_LOCATION;
    private static final Location START_LOCATION = Utils.newLocation(40.7308, -73.9974); // Washington Square Park
    private static final Location END_LOCATION = Utils.newLocation(40.8052, -73.9431); // Marcus Garvey Park
    private static final int TOTAL_TIME = 10 * 60; // In seconds (that's 10 km in 10 minutes, almost 60 km/h)
    private static final int PERIOD = 1000; // In ms

    @Override
    public void setAddressView(AddressView av) {
        this.mAddressView = av;
        mInitTime = System.currentTimeMillis() / 1000;
        scheduleNext();
    }

    private void scheduleNext() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                double elapsed = System.currentTimeMillis() / 1000 - mInitTime;
                double t = Math.min(elapsed / TOTAL_TIME, 1);
                if (mAddressView != null) {
                    double deltaLat = END_LOCATION.getLatitude() - START_LOCATION.getLatitude();
                    double deltaLon = END_LOCATION.getLongitude() - START_LOCATION.getLongitude();
                    double lat = START_LOCATION.getLatitude() + deltaLat * t;
                    double lon = START_LOCATION.getLongitude() + deltaLon * t;
                    mLastLocation = Utils.newLocation(lat, lon);
                    mAddressView.setLocation(mLastLocation, false);
                }
                if (t < 1) {
                    scheduleNext();
                }   
            }
        }, PERIOD);
    }

    @Override
    public Location getLocation() {
        return null;
    }
}
