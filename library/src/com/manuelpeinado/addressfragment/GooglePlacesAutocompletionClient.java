package com.manuelpeinado.addressfragment;

import java.net.URLEncoder;
import java.util.List;

import us.monoid.web.Resty;

import com.google.gson.Gson;
import com.manuelpeinado.addressfragment.gson.PlacesAutocompletionResult;

public class GooglePlacesAutocompletionClient extends PlacesAutocompletionClient {
    
    private final String mApiKey;

    public GooglePlacesAutocompletionClient(String apiKey) {
        this.mApiKey = apiKey;
    }
    
    @Override public List<Place> getPlacesSync(String text) {
        try {
            String baseUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json?";
            String args = "input="+ URLEncoder.encode(text, "UTF-8") 
                        + "&components=country:es&types=geocode&sensor=true&key=" + mApiKey;
            String url = baseUrl + args;
            String json = new Resty().text(url).toString();
            Gson gson = new Gson();
            PlacesAutocompletionResult result = gson.fromJson(json, PlacesAutocompletionResult.class);
            return result.getPlaces();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
