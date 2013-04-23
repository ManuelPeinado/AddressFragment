package com.manuelpeinado.addressfragment.gson;

import java.util.ArrayList;
import java.util.List;

import com.manuelpeinado.addressfragment.Place;

public class PlacesAutocompletionResult {
    private List<Prediction> predictions;
    private String status;

    public List<Place> getPlaces() {
        List<Place> result = new ArrayList<Place>();
        for (Prediction pred : predictions) {
            result.add(new Place(pred.description));
        }
        return result;
    }
}
