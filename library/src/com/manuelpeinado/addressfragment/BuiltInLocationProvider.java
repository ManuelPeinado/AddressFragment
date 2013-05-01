 package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.manuelpeinado.addressfragment.AddressView.LocationProvider;


public class BuiltInLocationProvider implements LocationProvider, LocationListener {

    protected static final String TAG = BuiltInLocationProvider.class.getSimpleName();
    private AddressView mAddressFragment;
    private boolean mListeningToGps;
    private boolean mListeningToNetwork;
    private Location mLastLocation;

    @Override
    public void setAddressView(AddressView addressView) {
        if (mAddressFragment == addressView) {
            return;
        }
        cancelLocationUpdates();
        this.mAddressFragment = addressView;
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (mAddressFragment == null) {
            return;
        }

        LocationManager lm = getLocationManager();
//        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        // TODO
//        if (!gpsEnabled && !networkEnabled) {
//            showEnableLocationDialog();
//            return;
//        }

        // TODO extract min time and min dist into a member variable (or constant)
        // TODO disable verbose logs in release version
        if (!mListeningToGps) {
            Log.v(TAG, "Requesting GPS location updates");
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30 * 1000, 200, this);
            mListeningToGps = true;
        }
        if (!mListeningToNetwork) {
            Log.v(TAG, "Requesting NETWORK location updates");
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30 * 1000, 200, this);
            mListeningToNetwork = true;
        }
    }

    private void cancelLocationUpdates() {
        if (mAddressFragment == null) {
            return;
        }
        LocationManager lm = getLocationManager();
        lm.removeUpdates(this);
        mListeningToGps = false;
        mListeningToNetwork = false;
    }

    // TODO add the possibility of using the last known location if the client so desires
    @Override
    public Location getLocation() {
        return mLastLocation;
    }
    
    private LocationManager getLocationManager() {
        return (LocationManager) mAddressFragment.getContext().getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        Utils.logv(TAG, "onLocationChanged", "%s (%s)", Utils.prettyPrint(newLocation), newLocation.getProvider());
        if (!Utils.isBetterLocation(newLocation, mLastLocation)) {
            Utils.logv(TAG, "onLocationChanged", "New location is not significantly better than previous one; ignoring it");
        }
        mLastLocation = newLocation;
        if (mAddressFragment != null) {
            mAddressFragment.setLocation(newLocation, false);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onProviderEnabled(String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
