package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.location.Location;

public class SingleShotLocationTask {
    
    public interface SingleShotLocationListener {
        void onLocationReady(SingleShotLocationTask singleShotLocationRequest, Location newLocation);
    }

    public SingleShotLocationTask(Context context) {
    }

    public void getLocation(SingleShotLocationListener listener) {
        listener.onLocationReady(this, Utils.newLocation(41.383333, 2.183333));
    }
}
