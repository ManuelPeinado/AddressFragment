package com.manuelpeinado.addressfragment;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class AutocompleteAddressAdapter extends ArrayAdapter<Place> {
    private PlacesAutocompletionClient mAutocompletionClient = new GooglePlacesAutocompletionClient(Constants.API_KEY);
    private List<Place> mPlaces;

    public AutocompleteAddressAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1);
    }

    @Override
    public int getCount() {
        return mPlaces.size();
    }

    @Override
    public Place getItem(int index) {
        return mPlaces.get(index);
    }

    @Override
    public Filter getFilter() {
        Filter myFilter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                // This method is called in a worker thread
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    try {
                        List<Place> places = mAutocompletionClient.getPlacesSync(constraint.toString());
                        filterResults.values = places;
                        filterResults.count = places.size();
                    } catch (Exception e) {
                    }
                }
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override protected void publishResults(CharSequence contraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    mPlaces = (List<Place>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return myFilter;
    }
}