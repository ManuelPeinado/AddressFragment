package com.manuelpeinado.addressfragment;

import java.util.Locale;
import java.util.Random;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Utils {
    private static final String LOCATION_FMT_STR = "%.4f, %.4f";
    private static final int RANDOM_TAG_KEY = new Random().nextInt();

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

    // Write javadoc explaining differences to isBetterLocation
    public static boolean isDifferentLocation(Location a, Location b) {
        if ((a == null || b == null) && a != b) {
            return true;
        }
        final double EPSILON = 1e-4; // Roughly 10m
        double latDiff = a.getLatitude() - b.getLatitude();
        double lonDiff = a.getLongitude() - b.getLongitude();
        return latDiff * latDiff + lonDiff * lonDiff > EPSILON * EPSILON;
    }

    public static Location addressToLocation(Address address) {
        Location result = new Location((String) null);
        result.setLatitude(address.getLatitude());
        result.setLongitude(address.getLongitude());
        return result;
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static boolean DEBUG = false;

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     * 
     * @param location
     *            The new Location that you want to evaluate
     * @param currentBestLocation
     *            The current Location fix, to which you want to compare the new
     *            one
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public static void longToast(Context ctx, String text) {
        Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
    }

    public static void shortToast(Context ctx, String text) {
        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
    }

    public static Location newLocation(double latitude, double longitude) {
        Location result = new Location((String) null);
        result.setLatitude(latitude);
        result.setLongitude(longitude);
        return result;
    }

    public static void setEditTextReadOnly(EditText editText, boolean readOnly) {
        if (readOnly) {
            editText.setTag(RANDOM_TAG_KEY, editText.getKeyListener());
            editText.setKeyListener(null);
        }
        else {
            Object tag = editText.getTag(RANDOM_TAG_KEY);
            editText.setKeyListener((KeyListener)tag);
        }
    }
    
    public static void logv(String className, String methodName, String fmt, Object...args) {
        if (DEBUG) {
            Log.v(className + "#" + methodName + "()", String.format(fmt, args));
        }
    }

    public static int[] getPadding(View view) {
        return new int[] {
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        };
    }
}
