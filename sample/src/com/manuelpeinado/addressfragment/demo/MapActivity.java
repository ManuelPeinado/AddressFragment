package com.manuelpeinado.addressfragment.demo;

import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.manuelpeinado.addressfragment.AddressFragment;
import com.manuelpeinado.addressfragment.AddressView;

public class MapActivity extends SherlockFragmentActivity implements AddressView.LocationProvider,
        AddressView.OnNewAddressListener, OnMyLocationChangeListener,
        AddressView.OnMyLocationClickIgnoredListener, OnMapClickListener {

    private GoogleMap mMap;
    private boolean mFirstFixReceived;
    private Marker mMarker;
    private AddressView mAddressView;
    private boolean mShowToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        mMap = mapFragment.getMap();
        if (!ensureMapIsReady()) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setOnMapClickListener(this);
        AddressFragment addressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressView = addressFragment.getAddressView();
        mAddressView.setLocationProvider(this);
        mAddressView.setOnNewAddressListener(this);
        mAddressView.setOnMyLocationClickIgnoredListener(this);
        
        if (getUseMockLocationSource()) {
            mMap.setLocationSource(new MockLocationSource());
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mShowToast = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mShowToast) {
                    Utils.shortToast(MapActivity.this, getString(R.string.click_on_the_map_to_change_the_selected_location));
                }
            }
        }, 5000);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mShowToast  = false;
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
    public void onNewAddress(AddressView sender, Address address, Location location, boolean isUserProvided) {
        LatLng latLng = Utils.locationToLatLng(location);
        moveMarkerTo(latLng, address);
        if (!mFirstFixReceived || isUserProvided) {
            CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
            mMap.animateCamera(update);
        }
        mFirstFixReceived = true;
    }

    private void moveMarkerTo(LatLng position, Address address) {
        if (mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions().position(position));
        } else {
            mMarker.setPosition(position);
        }
        mMarker.setTitle(Utils.prettyPrint(address));
        mMarker.setSnippet(Utils.prettyPrint(position));
    }

    @Override
    public void onMyLocationChange(Location newLocation) {
        mAddressView.setLocation(newLocation, false);
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

    @Override
    public void onMyLocationClickIgnored(AddressView sender) {
        LatLng latLng = Utils.locationToLatLng(mMap.getMyLocation());
        CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(update);
    }

    @Override
    public void onMapClick(LatLng point) {
        mAddressView.setLocation(Utils.latLngToLocation(point), true);
    }
    
    protected boolean getUseMockLocationSource() {
        return false;
    }
}
