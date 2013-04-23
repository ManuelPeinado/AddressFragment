package com.manuelpeinado.addressfragment;

import java.util.Arrays;
import java.util.List;

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
