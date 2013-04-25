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

public class MapActivity extends SherlockFragmentActivity implements AddressFragment.LocationProvider,
        AddressFragment.OnNewAddressListener, OnMyLocationChangeListener,
        AddressFragment.OnMyLocationClickIgnoredListener, OnMapClickListener {

    private static boolean USE_MOCK_LOCATION_SOURCE = false;
    private GoogleMap mMap;
    private AddressFragment mAddressFragment;
    private boolean mFirstFixReceived;
    private Marker mMarker;

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
        if (USE_MOCK_LOCATION_SOURCE) {
            mMap.setLocationSource(new MockLocationSource());
        }
        mMap.setOnMapClickListener(this);
        mAddressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressFragment.setLocationProvider(this);
        mAddressFragment.setOnNewAddressListener(this);
        mAddressFragment.setOnMyLocationClickIgnoredListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Utils.shortToast(MapActivity.this, "Click on the map to change the selected location");
            }
            
        }, 5000);
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
    public void onNewAddress(AddressFragment sender, Address address, Location location, boolean isUserProvided) {
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
        mAddressFragment.setLocation(newLocation, false);
    }

    @Override
    public void setAddressFragment(AddressFragment addressFragment) {
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
    public void onMyLocationClickIgnored(AddressFragment sender) {
        LatLng latLng = Utils.locationToLatLng(mMap.getMyLocation());
        CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(update);
    }

    @Override
    public void onMapClick(LatLng point) {
        mAddressFragment.setLocation(Utils.latLngToLocation(point), true);
    }
}
