package com.manuelpeinado.addressfragment.demo;

import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.manuelpeinado.addressfragment.AddressFragment;
import com.manuelpeinado.addressfragment.AddressFragment.LocationProvider;

public class MapActivity extends FragmentActivity implements AddressFragment.LocationProvider,
        AddressFragment.OnNewAddressListener, OnMyLocationChangeListener, AddressFragment.OnMyLocationClickIgnoredListener {

    private static boolean USE_MOCK_LOCATION_SOURCE = false;
    private GoogleMap mMap;
    private AddressFragment mAddressFragment;
    private LocationListener mLocationListener;
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
        mAddressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressFragment.setLocationProvider(this);
        mAddressFragment.setOnNewAddressListener(this);
        mAddressFragment.setOnMyLocationClickIgnoredListener(this);
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
    public void onNewAddress(AddressFragment sender, Address address, boolean isUserProvided) {
        LatLng latLng = Utils.addressToLatLng(address);
        if (sender.isUsingMyLocation()) {
            // This is necessary because the location contained in "address" corresponds to a real
            // address, and thus does not match exactly the actual location of the device. Without
            // this adjustment the marker would appear slightly displaced with respect to the my location
            // marker, which looks awful
            latLng = Utils.locationToLatLng(mMap.getMyLocation());
        }
        moveMarkerTo(latLng, address);
        if (!mFirstFixReceived || isUserProvided) {
            CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
            mMap.animateCamera(update);
        }
        mFirstFixReceived = true;
    }

    private void moveMarkerTo(LatLng position, Address address) {
        if (mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions()
                        .position(position));
        }
        else {
            mMarker.setPosition(position);
        }
        mMarker.setTitle(Utils.prettyPrint(address));
        mMarker.setSnippet(Utils.prettyPrint(position));
    }

    @Override
    public void onMyLocationChange(Location newLocation) {
        if (mLocationListener != null) {
            mLocationListener.onLocationChanged(this, newLocation);
        }
    }

    @Override
    public void addLocationListener(LocationProvider.LocationListener listener) {
        mLocationListener = listener;
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

}
