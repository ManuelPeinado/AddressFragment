package com.manuelpeinado.addressfragment.demo.apiclients.directions;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

public class Polyline {
    @SerializedName("points")
    public String encodedPoints;
    
    public List<LatLng> decodePoints() {
        List<LatLng> poly = new ArrayList<LatLng>();
          int index = 0, len = encodedPoints.length();
          int lat = 0, lng = 0;

          while (index < len) {
              int b, shift = 0, result = 0;
              do {
                  b = encodedPoints.charAt(index++) - 63;
                  result |= (b & 0x1f) << shift;
                  shift += 5;
              } while (b >= 0x20);
              int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
              lat += dlat;
              shift = 0;
              result = 0;
              do {
                  b = encodedPoints.charAt(index++) - 63;
                  result |= (b & 0x1f) << shift;
                  shift += 5;
              } while (b >= 0x20);
              int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
              lng += dlng;

              poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
          }
          return poly;
    }
    
}
