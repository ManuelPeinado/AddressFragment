package com.manuelpeinado.addressfragment;

import java.util.Locale;

import android.location.Address;
import android.location.Location;


public class Utils {
    private static final String LOCATION_FMT_STR = "%.4f, %.4f";

    public static void sleep(long time) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static String prettyPrint(Address result) {
        return prettyPrint(result, ", ");
    }

    public static String prettyPrint(Address result, String separator) {
        StringBuilder builder = new StringBuilder();
        builder.append(result.getAddressLine(0));
        for (int i = 1; i <= result.getMaxAddressLineIndex(); ++i) {
            builder.append(separator);
            builder.append(result.getAddressLine(i));
        }
        return builder.toString();
    }
    
    public static String prettyPrint(Location location) {
        return String.format(Locale.getDefault(), LOCATION_FMT_STR, location.getLatitude(), location.getLongitude());
    }
    
    public static boolean isDifferentLocation(Location oldLocation, Location newLocation) {
        if (newLocation == null) {
            return false;
        }
        // TODO implement this
        return true;
    }
    
    public static Location addressToLocation(Address address) {
        Location result = new Location((String)null);
        result.setLatitude(address.getLatitude());
        result.setLongitude(address.getLongitude());
        return result;
    }
}
