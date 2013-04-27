package com.manuelpeinado.addressfragment;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

public class ReverseGeocodingTask extends AsyncTask<Location, Void, Address> {

    private Context mContext;
    private ReverseGeocodingListener mListener;
    private Location mLocation;
    
    public interface ReverseGeocodingListener {
        void onReverseGeocodingResultReady(ReverseGeocodingTask sender, Address result);
    }

    public ReverseGeocodingTask(Context context) {
        this.mContext = context;
    }

    public void setListener(ReverseGeocodingListener listener) {
        this.mListener = listener;
    }
    
    public Location getLocation() {
        return mLocation;
    }

    @Override
    protected Address doInBackground(Location... params) {
        Geocoder geocoder = new Geocoder(mContext);
        List<Address> results;
        try {
            mLocation = params[0];
            results = geocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
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
        if (mListener != null) {
            mListener.onReverseGeocodingResultReady(this, result);
        }
    }
}
