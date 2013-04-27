package com.manuelpeinado.addressfragment.demo;

import java.util.List;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.PolylineOptions;
import com.manuelpeinado.addressfragment.Callback;
import com.manuelpeinado.addressfragment.demo.DirectionsDialog.OnAcceptButtonClickListener;
import com.manuelpeinado.addressfragment.demo.apiclients.directions.GoogleDirectionsClient;
import com.manuelpeinado.addressfragment.demo.apiclients.directions.GoogleDirectionsResponse;
import com.manuelpeinado.addressfragment.demo.apiclients.directions.Route;
import com.manuelpeinado.addressfragment.demo.apiclients.directions.Step;

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
                        computeRoute(startAddress, endAddress);
                    }
                });
            }
        });
    }

    private void computeRoute(final Location startAddress, final Location endAddress) {
        GoogleDirectionsClient client = new GoogleDirectionsClient();
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setCancelable(false);
        dlg.setMessage(getString(R.string.computing_directions));
        dlg.setIndeterminate(true);
        dlg.show();
        client.sendRequest(startAddress.getLatitude(), startAddress.getLongitude(), 
                endAddress.getLatitude(), endAddress.getLongitude(), new Callback<GoogleDirectionsResponse>() {
                    @Override
                    public void onResultReady(GoogleDirectionsResponse response) {
                        dlg.hide();
                        addRoute(response);
                    }
                });
    }

    private void addRoute(GoogleDirectionsResponse response) {
        if (response.routes == null || response.routes.size() == 0) {
            return;
        }
        Route route = response.routes.get(0);
        PolylineOptions polylineOptions = new PolylineOptions();
        for (Step step : route.getAllSteps()) {
            List<LatLng> points = step.polyline.decodePoints();
            polylineOptions.addAll(points);
        }
        polylineOptions.color(Color.argb(127, 0, 0, 255));
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, displayMetrics);
        polylineOptions.width(width);
        Builder builder = LatLngBounds.builder(); 
        for (LatLng point : polylineOptions.getPoints()) {
            builder.include(point);
        }
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, displayMetrics);
        mMap.addPolyline(polylineOptions);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(builder.build(), (int)padding);
        mMap.animateCamera(update);
    }

    @Override
    public void onMyLocationChange(Location location) {
        LatLng latLng = Utils.locationToLatLng(location);
        CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(update);
        mMap.setOnMyLocationChangeListener(null);
    }
}
