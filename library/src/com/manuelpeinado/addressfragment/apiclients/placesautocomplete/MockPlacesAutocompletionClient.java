package com.manuelpeinado.addressfragment.apiclients.placesautocomplete;

import java.util.Arrays;
import java.util.List;

import com.manuelpeinado.addressfragment.Place;
import com.manuelpeinado.addressfragment.Utils;

public class MockPlacesAutocompletionClient extends PlacesAutocompletionClient {

    @Override public List<Place> getPlacesSync(String text) {
        Utils.sleep(500);
        return Arrays.asList(
                new Place("Amoeba Music, Haight Street, San Francisco, CA, United States"), 
                new Place("Amoeba Music, Telegraph Avenue, Berkeley, CA, United States"),
                new Place("Amoeba Music, Sunset Boulevard, Los Angeles, CA, United States"),
                new Place("Amoeba Solutions, Bangalore, Karnataka, India"));
    }
}
