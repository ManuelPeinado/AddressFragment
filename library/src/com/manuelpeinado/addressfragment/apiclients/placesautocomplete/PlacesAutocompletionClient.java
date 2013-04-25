package com.manuelpeinado.addressfragment.apiclients.placesautocomplete;

import java.util.List;

import com.manuelpeinado.addressfragment.Callback;
import com.manuelpeinado.addressfragment.Place;

import android.os.AsyncTask;

public abstract class PlacesAutocompletionClient {

    public abstract List<Place> getPlacesSync(String text);
    
    public final void sendRequest(final String text, final Callback<List<Place>> callback) {
        new AsyncTask<Void, Void, List<Place>>() {
            @Override protected List<Place> doInBackground(Void... params) {
                return getPlacesSync(text);
            }
            
            @Override protected void onPostExecute(List<Place> result) {
                callback.onResultReady(result);
            }
        }.execute();
    }

}
