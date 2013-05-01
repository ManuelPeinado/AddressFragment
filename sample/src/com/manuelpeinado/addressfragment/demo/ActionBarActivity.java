package com.manuelpeinado.addressfragment.demo;

import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.manuelpeinado.addressfragment.AddressView;
import com.manuelpeinado.addressfragment.AddressView.LocationProvider;
import com.manuelpeinado.addressfragment.AddressView.OnMyLocationClickIgnoredListener;
import com.manuelpeinado.addressfragment.AddressView.OnNewAddressListener;

public class ActionBarActivity extends SherlockFragmentActivity implements OnMyLocationChangeListener,
        OnMapClickListener, LocationProvider, OnNewAddressListener, OnMyLocationClickIgnoredListener {

    private AddressView mAddressView;
    private GoogleMap mMap;
    private boolean mFirstFixReceived;
    private Marker mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_bar);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.activity_actionbar, menu);
        mAddressView = (AddressView) menu.findItem(R.id.action_address).getActionView();
        mAddressView.setLocationProvider(this);
        mAddressView.setOnNewAddressListener(this);
        mAddressView.setOnMyLocationClickIgnoredListener(this);
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
    public void onMapClick(LatLng point) {
        mAddressView.setLocation(Utils.latLngToLocation(point), true);
    }

    private boolean ensureMapIsReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.could_not_load_map, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }
}