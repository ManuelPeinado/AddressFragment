package com.manuelpeinado.addressfragment.demo;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.manuelpeinado.addressfragment.Callback;
import com.manuelpeinado.addressfragment.demo.DirectionsDialog.OnAcceptButtonClickListener;
import com.manuelpeinado.addressfragment.demo.apiclients.directions.GoogleDirectionsClient;
import com.manuelpeinado.addressfragment.demo.apiclients.directions.GoogleDirectionsResponse;

public class DirectionsActivity extends SherlockFragmentActivity implements OnMyLocationChangeListener,
        OnAcceptButtonClickListener {
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);

        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        mMap = mapFragment.getMap();
        if (!ensureMapIsReady()) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(this);
    }

    private boolean ensureMapIsReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.could_not_load_map, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.activity_directions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_directions) {
            showDirectionsDialog();
        }
        return true;
    }

    private void showDirectionsDialog() {
        DirectionsDialog dlg = DirectionsDialog.newInstance();
        // TODO restore this listener when activity is recreated due to configuration change
        dlg.setOnAcceptButtonClickListener(this);
        dlg.show(getSupportFragmentManager(), "directionsDlg");
    }
    
    @Override
    public void onAcceptButtonClick(final DirectionsDialog sender) {
        sender.getStartAddressView().resolveAddress(new Callback<Location>() {
            @Override
            public void onResultReady(final Location startAddress) {
                sender.getEndAddressView().resolveAddress(new Callback<Location>() {
                    @Override
                    public void onResultReady(final Location endAddress) {
                        GoogleDirectionsClient client = new GoogleDirectionsClient();
                        client.sendRequest(startAddress.getLatitude(), startAddress.getLongitude(), 
                                endAddress.getLatitude(), endAddress.getLongitude(), new Callback<GoogleDirectionsResponse>() {
                                    @Override
                                    public void onResultReady(GoogleDirectionsResponse response) {
                                        Utils.shortToast(DirectionsActivity.this, "Duration of the trip is " + response.getDuration());
                                    }
                                });
                    }
                });
            }
        });
    }

    @Override
    public void onMyLocationChange(Location location) {
        LatLng latLng = Utils.locationToLatLng(location);
        CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(update);
        mMap.setOnMyLocationChangeListener(null);
    }
}
