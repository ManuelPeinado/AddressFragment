package com.manuelpeinado.addressfragment.apiclients.placesautocomplete;

import java.util.ArrayList;
import java.util.List;

import com.manuelpeinado.addressfragment.Place;

public class PlacesAutocompletionResult {
    private List<Prediction> predictions;

    public List<Place> getPlaces() {
        List<Place> result = new ArrayList<Place>();
        for (Prediction pred : predictions) {
            result.add(new Place(pred.description));
        }
        return result;
    }
}
