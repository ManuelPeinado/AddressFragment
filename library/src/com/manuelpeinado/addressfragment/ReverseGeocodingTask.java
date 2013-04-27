package com.manuelpeinado.addressfragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

public class ReverseGeocodingTask extends AsyncTask<Location, Void, Address> {

    private WeakReference<ReverseGeocodingListener> mListener;
    private Location mLocation;
    private Geocoder mGeocoder;
    
    public interface ReverseGeocodingListener {
        void onReverseGeocodingResultReady(ReverseGeocodingTask sender, Address result);
    }

    public ReverseGeocodingTask(Context context) {
        mGeocoder = new Geocoder(context);
    }

    public void setListener(ReverseGeocodingListener listener) {
        this.mListener = new WeakReference<ReverseGeocodingListener>(listener);
    }
    
    public Location getLocation() {
        return mLocation;
    }

    @Override
    protected Address doInBackground(Location... params) {
        List<Address> results;
        try {
            mLocation = params[0];
            results = mGeocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
            if (results.size() > 0) {
                return results.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    protected void onPostExecute(Address result) {
        if (mListener != null && mListener.get() != null) {
            mListener.get().onReverseGeocodingResultReady(this, result);
        }
    }
}
