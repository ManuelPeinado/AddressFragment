package com.manuelpeinado.addressfragment.demo;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

public class DirectionsActivity extends SherlockFragmentActivity {
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
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
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
        new DialogFragment() {
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View result = inflater.inflate(R.layout.dialog_directions, container, false);
                return result;
            }
        }.show(getSupportFragmentManager(), "directionsDlg");
    }
}
