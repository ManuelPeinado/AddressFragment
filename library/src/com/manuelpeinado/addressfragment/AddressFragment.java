package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 *
 */
public class AddressFragment extends Fragment implements OnClickListener, ReverseGeocodingTask.Listener,
        OnEditorActionListener, GeocodingTask.Listener, OnItemClickListener, OnFocusChangeListener, LocationListener {

    protected static final String TAG = AddressFragment.class.getSimpleName();
    /**
     * This will be null if the fragment provides its own locations or does not
     * show my location address
     */
    private LocationProvider mLocationProvider;
    /** This object will be notified each time a new location is available */
    private OnNewAddressListener mOnNewAddressListener;
    /**
     * This object will be notified each time the "my location" button is
     * clicked but the fragment does not respond to it because (ignores it)
     * because it's already in my location mode
     */
    private OnMyLocationClickIgnoredListener mOnMyLocationClickIgnoredListener;
    /**
     * Last location that was reversely geocoded. Storing this information
     * allows us to prevent repeating the geocoding of an already processed
     * location
     */
    private Location mLastLocation;
    /** True if the fragment is currently showing the device's location */
    private boolean mIsUsingMyLocation = true;
    private Address mAddress;
    private String mPrettyAddress;
    private GeocodingTask mGeocodingTask;
    private ReverseGeocodingTask mReverseGeocodingTask;
    /**
     * A user-initiated reverse geocoding task is one which starts because the
     * user has clicked on the "cancel current edit" button, as opposed to the
     * task that is executed when a new location fix is received. We need to
     * know in which case we are in order to pass this information to the
     * listener, which might decide to act accordingly (for instance, a map
     * listener may update its camera target only if the new address was user
     * initiated, not if it was initiated itself
     */
    private boolean mIsReverseGeocodingTaskUserInitiated;
    private AutoCompleteTextView mAddressEditText;
    /**
     * This button is also used to cancel the current edition when the edittext
     * has focus
     */
    private ImageView mUseMyLocationBtn;
    private ProgressBar mProgressBar;
    /**
     * Initially we show the progress bar until the first location fix is
     * received from the provider. This variable allows us know whether the
     * first fix has been received yet
     */
    private boolean mWaitingForFirstFix = true;
    private boolean mHandlesOwnLocation;
    private boolean mListeningToGps;
    private boolean mListeningToNetwork;

    /**
     * An objects that implements this interface can provide location fixes to
     * the fragment, both on demand (through the {@code getLocation}) method and
     * when they become available (through its own interface
     * {@code LocationListener})
     */
    public interface LocationProvider {
        Location getLocation();
    }

    /**
     * An object that implements this interface can receive a notification every
     * time the fragment changes its address
     */
    public interface OnNewAddressListener {
        void onNewAddress(AddressFragment sender, Address address, boolean isUserProvided);
    }

    /**
     * An object that implements this interface can receive a notification every
     * time the "use my location" button is clicked and the fragment is already
     * in my location mode. This can be used, for instance, by a map activity to
     * move the camera target to the current location. Where it not for this
     * listener, the click would go unnoticed and the user would have no way to
     * go to the current location (we assume that the map's built-in
     * "my location" button is not shown)
     */
    public interface OnMyLocationClickIgnoredListener {
        void onMyLocationClickIgnored(AddressFragment sender);
    }

    public void setOnNewAddressListener(OnNewAddressListener listener) {
        mOnNewAddressListener = listener;
    }

    public void setOnMyLocationClickIgnoredListener(OnMyLocationClickIgnoredListener listener) {
        mOnMyLocationClickIgnoredListener = listener;
    }

    /**
     * Pass null to remove the current provider
     * 
     * @param provider
     */
    public void setLocationProvider(LocationProvider provider) {
        mLocationProvider = provider;
        if (provider == null) {
            Log.v(TAG, "Removing location provider");
            return;
        }
        Log.v(TAG, "Setting new location provider");
    }

    /**
     * This method is used by the location provider to inform us that the device
     * location has changed
     */
    public void setLocation(Location newLocation) {
        onNewLocation(newLocation, false);
    }

    public void setHandlesOwnLocation(boolean value) {
        if (mHandlesOwnLocation == value) {
            return;
        }
        Log.v(TAG, "Handling own location: " + value);
        mHandlesOwnLocation = value;
        updateLocationManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.aet_default_layout, container, false);
        mProgressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        mAddressEditText = (AutoCompleteTextView) root.findViewById(R.id.addressEditText);
        mAddressEditText.setAdapter(new AutocompleteAddressAdapter(getActivity()));
        // We use this listener to find out when the user has clicked "Done" on the virtual keyboard
        mAddressEditText.setOnEditorActionListener(this);
        // We use this listener to find out when the user has selected a item from the autocompletion dropdown
        mAddressEditText.setOnItemClickListener(this);
        mAddressEditText.setOnFocusChangeListener(this);
        mUseMyLocationBtn = (ImageView) root.findViewById(R.id.useMyLocationButton);
        mUseMyLocationBtn.setOnClickListener(this);
        return root;
    }

    private void updateText() {
        AutocompleteAddressAdapter adapter = (AutocompleteAddressAdapter) mAddressEditText.getAdapter();
        mAddressEditText.setAdapter(null);
        mAddressEditText.setText(mPrettyAddress);
        mAddressEditText.setAdapter(adapter);
    }

    /**
     * @param newLocation
     * @param isUserInitiated
     * @param force
     *            Forces the new location to be applied event if it's equal to
     *            the latest one
     */
    private void onNewLocation(Location newLocation, boolean isUserInitiated) {
        if (mWaitingForFirstFix) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mWaitingForFirstFix = false;
        }
        // We use the hasFocus test to find out when the user has started interaction with the edit text, 
        // at which point we must stop listening to location updates because we don't want to annoy the
        // user by changing the contents of the edittext when she is in the middle of writing an address
        if (mIsUsingMyLocation && !mAddressEditText.hasFocus()) {
            if (Utils.isDifferentLocation(mLastLocation, newLocation)) {
                startReverseGeocodingTask(newLocation, isUserInitiated);
            } else {
                Log.v(TAG, "New location is too similar to previous one; ignoring it");
            }
        }
    }

    private void startReverseGeocodingTask(Location location, boolean isUserInitiated) {
        cancelPendingTasks();
        Log.v(TAG, "Starting reverse geocoding of location " + Utils.prettyPrint(location));
        mLastLocation = location;
        mIsReverseGeocodingTaskUserInitiated = isUserInitiated;
        mReverseGeocodingTask = new ReverseGeocodingTask(getActivity());
        mReverseGeocodingTask.setListener(this);
        mReverseGeocodingTask.execute(location);
    }

    @Override
    public void onReverseGeocodingResultReady(ReverseGeocodingTask sender, Address result) {
        hideProgressBar();
        mReverseGeocodingTask = null;
        String newPrettyAddress = Utils.prettyPrint(result);
        Log.v(TAG, "Reverse geocoding of location " + Utils.prettyPrint(sender.getLocation())
                + " finished; address is " + newPrettyAddress);
        if (newPrettyAddress.equals(mPrettyAddress)) {
            Log.v(TAG, "Ignoring new address because it's the same as previous one");
            return;
        }
        mPrettyAddress = newPrettyAddress;
        mAddress = result;
        updateText();
        notifyListener(mIsReverseGeocodingTaskUserInitiated);
    }

    private void notifyListener(boolean isUserProvided) {
        if (mOnNewAddressListener != null) {
            mOnNewAddressListener.onNewAddress(this, mAddress, isUserProvided);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
            clearEditTextFocus();
            String addressText = getAddressText();
            // TODO check that addressText is not too short
            Log.v(TAG, "Editor action IME_ACTION_SEARCH");
            mIsUsingMyLocation = false;
            startGeocoding(addressText);
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        onEditorAction(mAddressEditText, EditorInfo.IME_ACTION_DONE, null);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            mUseMyLocationBtn.setImageResource(R.drawable.ic_navigation_cancel);
        } else {
            mUseMyLocationBtn.setImageResource(R.drawable.ic_device_access_location_found);
        }
    }

    private void startGeocoding(String addressText) {
        Log.v(TAG, "Starting geocoding of address " + addressText);
        cancelPendingTasks();
        mGeocodingTask = new GeocodingTask(getActivity());
        mGeocodingTask.setListener(this);
        mGeocodingTask.execute(addressText);
        showProgressBar();
    }

    @Override
    public void onGeocodingResultReady(GeocodingTask sender, Address result) {
        hideProgressBar();
        mGeocodingTask = null;
        mLastLocation = Utils.addressToLocation(result);
        String newPrettyAddress = Utils.prettyPrint(result);
        Log.v(TAG,
                "Geocoding of address " + sender.getAddressText() + " finished; location is "
                        + Utils.prettyPrint(mLastLocation));
        if (newPrettyAddress.equals(mPrettyAddress)) {
            Log.v(TAG, "Ignoring new address because it's the same as previous one");
            return;
        }
        mPrettyAddress = newPrettyAddress;
        mAddress = result;
        updateText();
        notifyListener(true);
    }

    @Override
    public void onGeocodingCanceled(GeocodingTask sender) {
        Log.v(TAG, "Geocoding of address " + sender.getAddressText() + " canceled by user");
        hideProgressBar();
        cancelCurrentEdit();
    }

    private void cancelPendingTasks() {
        if (mGeocodingTask != null) {
            Log.v(TAG, "Geocoding task was already running. Cancelling it");
            mGeocodingTask.cancel(true);
        }
        if (mReverseGeocodingTask != null) {
            Log.v(TAG, "Reverse geocoding task was already running. Cancelling it");
            mReverseGeocodingTask.cancel(true);
        }
    }

    private String getAddressText() {
        return mAddressEditText.getText().toString();
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mUseMyLocationBtn.setVisibility(View.INVISIBLE);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mUseMyLocationBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        // This method is invoked when the user clicks on the "use my location" or "cancel" button
        // The actual semantics depend on whether the edittext has focus (i.e., on whether the user
        // is currently entering a new address)

        if (mAddressEditText.hasFocus()) {
            // This means that the button has the semantics "cancel current edit"
            cancelCurrentEdit();
        } else {
            // This means that the button has the semantics "use my location"

            if (mIsUsingMyLocation) {
                if (mOnMyLocationClickIgnoredListener != null) {
                    mOnMyLocationClickIgnoredListener.onMyLocationClickIgnored(this);
                }
            } else {
                // If we were not on "use my location" mode and the button is pressed, we activate
                // that mode and apply 
                mIsUsingMyLocation = true;
                applyMostRecentLocation();
            }
        }
    }

    private void cancelCurrentEdit() {
        if (mIsUsingMyLocation) {
            // If we were on "my location" mode when the edit began, we apply the latest fix, 
            // which has probably been received (and ignored) while the edittext had focus
            clearEditTextFocus();
            applyMostRecentLocation();
        } else {
            // If we were not on "my location" mode when the edit began, we restore the address
            // that was in the edittext when the edit began
            mAddressEditText.setText(mPrettyAddress);
            onEditorAction(mAddressEditText, EditorInfo.IME_ACTION_DONE, null);
        }
    }

    /**
     * Restore the most recent location received from the location provider;
     * this is useful when the user clicks the "cancel" button and we are in the
     * "use my location" mode
     */
    private void applyMostRecentLocation() {
        // We set these two variables to null to force the update of everything, otherwise
        // inside "onNewLocation" we would detect that things haven't changed and we would 
        // not do anything
        mLastLocation = null;
        mPrettyAddress = null;
        onNewLocation(mLocationProvider.getLocation(), true);
    }

    private void clearEditTextFocus() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAddressEditText.getWindowToken(), 0);
        mAddressEditText.clearFocus();
    }

    public boolean isUsingMyLocation() {
        return mIsUsingMyLocation;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLocationManager();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        removeLocationUpdates(lm);
    }

    private void removeLocationUpdates(LocationManager lm) {
        lm.removeUpdates(this);
        mListeningToGps = false;
        mListeningToNetwork = false;
        Log.v(TAG, "Removing location updates");
    }

    private void updateLocationManager() {
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!mHandlesOwnLocation) {
            return;
        }
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            showEnableLocationDialog();
            return;
        }

        // TODO extract min time and min dist into a member variable (or constant)
        // TODO disable verbose logs in release version
        if (!mListeningToGps) {
            Log.v(TAG, "Requesting GPS location updates");
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30 * 1000, 200, this);
            mListeningToGps = true;
        }
        if (!mListeningToNetwork) {
            if (!gpsEnabled && networkEnabled) {
                Log.v(TAG, "Requesting NETWORK location updates because GPS is disabled");
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30 * 1000, 200, this);
                mListeningToNetwork = true;
            }
        }
    }

    private void showEnableLocationDialog() {
        // TODO
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        Log.v(TAG, "Received location " + Utils.prettyPrint(newLocation) + " from " + newLocation.getProvider());
        if (!Utils.isBetterLocation(newLocation, mLastLocation)) {
            Log.v(TAG, "New location is not significantly better than previous one; ignoring it");
        }
        onNewLocation(newLocation, false);
    }

    @Override
    public void onProviderDisabled(String provider) {
        updateLocationManager();
    }

    @Override
    public void onProviderEnabled(String provider) {
        updateLocationManager();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
