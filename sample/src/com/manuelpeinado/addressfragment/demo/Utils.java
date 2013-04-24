package com.manuelpeinado.addressfragment.demo;

import java.util.Locale;

import android.location.Address;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class Utils extends com.manuelpeinado.addressfragment.Utils {
    private static final String LOCATION_FMT_STR = "%.4f, %.4f";

    public static void sleep(long time) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static LatLng addressToLatLng(Address address) {
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

    public static String prettyPrint(LatLng latLng) {
        return String.format(Locale.getDefault(), LOCATION_FMT_STR, latLng.latitude, latLng.longitude);
    }

    public static Location newLocation(double latitude, double longitude) {
        Location result = new Location((String) null);
        result.setLatitude(latitude);
        result.setLongitude(longitude);
        return result;
    }

    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static Location latLngToLocation(LatLng point) {
        return newLocation(point.latitude, point.longitude);
    }
}
