package com.manuelpeinado.addressfragment.demo;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.manuelpeinado.addressfragment.AddressFragment;
import com.manuelpeinado.addressfragment.AddressView;

public class StylingActivity extends SherlockFragmentActivity implements AddressView.LocationProvider,
        OnMyLocationChangeListener {

    private static final int INITIAL_ZOOM_LEVEL = 16;
    private GoogleMap mMap;
    private AddressView mAddressView;
    private boolean mFirstCameraUpdate = true;
    private static boolean USE_MOCK_LOCATION_SOURCE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_read_only);

        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        mMap = mapFragment.getMap();
        if (!ensureMapIsReady()) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        AddressFragment addressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressView = addressFragment.getAddressView();
        mAddressView.setLocationProvider(this);
        if (USE_MOCK_LOCATION_SOURCE) {
            mMap.setLocationSource(new MockLocationSource());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    public void onMyLocationChange(Location newLocation) {
        mAddressView.setLocation(newLocation, false);
        LatLng latLng = Utils.locationToLatLng(newLocation);
        if (mFirstCameraUpdate) {
            mFirstCameraUpdate = false;
            // For some reason the zoom level is not properly read from XML. Besides, we can't animate zoom and
            // target at the same time, that's why we use moveCamera instead of animateCamera
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, INITIAL_ZOOM_LEVEL);
            mMap.moveCamera(update);
        }
    }

    @Override
    public void setAddressView(AddressView addressView) {
        // We don't need to do anything here because we already have the fragment
        // Ideally we would do something like "isPaused = addressFragment != null"
        // and then don't call addressFragment.setLocation() when paused, but that's
        // not really necessary because the fragment is clever enough to ignore
        // location updates when paused
    }

    @Override
    public Location getLocation() {
        return mMap.getMyLocation();
    }
}
