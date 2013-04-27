package com.manuelpeinado.addressfragment;

import java.io.IOException;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class GeocodingTask extends AsyncTask<String, Void, List<Address>> {

    private static final int MAX_RESULTS = 10;
    private FragmentActivity mActivity;
    private GeocodingTaskListener mListener;
    private String mAddressText;

    public interface GeocodingTaskListener {
        void onGeocodingResultReady(GeocodingTask sender, Address result);

        void onGeocodingCanceled(GeocodingTask geocodingTask);
    }

    public GeocodingTask(FragmentActivity activity) {
        this.mActivity = activity;
    }

    public void setListener(GeocodingTaskListener listener) {
        this.mListener = listener;
    }

    public String getAddressText() {
        return mAddressText;
    }

    @Override
    protected List<Address> doInBackground(String... params) {
        Geocoder geocoder = new Geocoder(mActivity);
        try {
            mAddressText = params[0];
            return geocoder.getFromLocationName(mAddressText, MAX_RESULTS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Address> results) {
        if (mListener == null) {
            return;
        }
        if (results.size() == 0) {
            mListener.onGeocodingResultReady(this, null);
        } else if (results.size() == 1) {
            mListener.onGeocodingResultReady(this, results.get(0));
        } else {
            showDisambiguationDialog(results);
        }
    }

    private void showDisambiguationDialog(final List<Address> results) {
        DialogFragment dlg = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                String[] items = buildAddressList(results);
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setTitle(R.string.aet__address_disambiguation_dialog_title);
                builder.setCancelable(false);
                builder.setItems(items, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onGeocodingResultReady(GeocodingTask.this, results.get(which));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onGeocodingCanceled(GeocodingTask.this);
                    }
                });
                return builder.create();
            }
        };
        dlg.show(mActivity.getSupportFragmentManager(), getClass().getName() + ".DLG");
    }

    private String[] buildAddressList(List<Address> results) {
        String[] result = new String[results.size()];
        for (int i = 0; i < results.size(); ++i) {
            result[i] = Utils.prettyPrint(results.get(i));
        }
        return result;
    }
}
