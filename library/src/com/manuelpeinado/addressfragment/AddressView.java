package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 *
 */
public class AddressView extends LinearLayout implements IAddressProvider, OnClickListener,
        ReverseGeocodingTask.Listener, OnEditorActionListener, GeocodingTask.Listener, OnItemClickListener,
        OnFocusChangeListener {

    protected static final String TAG = AddressView.class.getSimpleName();
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
    private boolean mIsLocationProviderPaused = true;
    private boolean mWaitingForFirstLocationFix;
    private boolean mShowingProgressBar;

    /**
     * An objects that implements this interface can provide location fixes to
     * the fragment, both on demand (through the {@code getLocation}) method and
     * when they become available (through its own interface
     * {@code LocationListener})
     */
    public interface LocationProvider {
        void setAddressView(AddressView av);

        Location getLocation();
    }

    /**
     * An object that implements this interface can receive a notification every
     * time the fragment changes its address
     */
    public interface OnNewAddressListener {
        void onNewAddress(AddressView sender, Address address, Location location, boolean isUserProvided);
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
        void onMyLocationClickIgnored(AddressView sender);
    }

    public AddressView(Context context) {
        this(context, null);
    }
    
    public AddressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setOrientation(LinearLayout.HORIZONTAL);

        LayoutInflater.from(context).inflate(R.layout.aet__default_layout, this);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mAddressEditText = (AutoCompleteTextView) findViewById(R.id.addressEditText);
        mAddressEditText.setAdapter(new AutocompleteAddressAdapter(context));
        // We use this listener to find out when the user has clicked "Done" on the virtual keyboard
        mAddressEditText.setOnEditorActionListener(this);
        // We use this listener to find out when the user has selected a item from the autocompletion dropdown
        mAddressEditText.setOnItemClickListener(this);
        mAddressEditText.setOnFocusChangeListener(this);
        mUseMyLocationBtn = (ImageView) findViewById(R.id.useMyLocationButton);
        mUseMyLocationBtn.setOnClickListener(this);
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
        if (!mIsLocationProviderPaused) {
            resumeLocationProvider();
        }
        Log.v(TAG, "Setting new location provider");
    }

    /**
     * This method is used by the location provider to inform us that the device
     * location has changed
     */
    public void setLocation(Location newLocation, boolean isUserInitiated) {
        if (mWaitingForFirstLocationFix) {
            mWaitingForFirstLocationFix = false;
            hideProgressBar();
        }
        if (isUserInitiated) {
            // If the provider sends a user-initiated location, we can no longer be in "using 
            // my location", as any new fixes would overwrite the location set by the user
            mIsUsingMyLocation = false;
            pauseLocationProvider();
        } else if (mIsLocationProviderPaused) {
            // We shouldn't receive non user-initiated locations from the provider, but in case
            // we receive one anyway we have to ignore it
            return;
        }
        onNewLocation(newLocation, isUserInitiated);
    }

    public void setHandlesOwnLocation(boolean value) {
        Log.v(TAG, "Handling own location: " + value);
        setLocationProvider(new BuiltInLocationProvider());
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
     */
    private void onNewLocation(Location newLocation, boolean isUserInitiated) {
        // We use the hasFocus test to find out when the user has started interaction with the edit text, 
        // at which point we must stop listening to location updates because we don't want to annoy the
        // user by changing the contents of the edittext when she is in the middle of writing an address
        if (!mAddressEditText.hasFocus()) {
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
        mIsReverseGeocodingTaskUserInitiated = isUserInitiated;
        mReverseGeocodingTask = new ReverseGeocodingTask(getContext());
        mReverseGeocodingTask.setListener(this);
        mReverseGeocodingTask.execute(location);
    }

    @Override
    public void onReverseGeocodingResultReady(ReverseGeocodingTask sender, Address result) {
        mReverseGeocodingTask = null;
        if (result == null) {
            // TODO use a resource for this string
            Utils.longToast(getContext(), "Location could not be resolved");
            return;
        }
        mLastLocation = sender.getLocation();
        String newPrettyAddress = Utils.prettyPrint(result);
        Log.v(TAG, "Reverse geocoding of location " + Utils.prettyPrint(mLastLocation) + " finished; address is "
                + newPrettyAddress);
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
            Location location = Utils.addressToLocation(mAddress);
            mOnNewAddressListener.onNewAddress(this, mAddress, location, isUserProvided);
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
            pauseLocationProvider();
        } else {
            mUseMyLocationBtn.setImageResource(R.drawable.ic_device_access_location_found);
        }
    }

    private void startGeocoding(String addressText) {
        Log.v(TAG, "Starting geocoding of address " + addressText);
        cancelPendingTasks();
        mGeocodingTask = new GeocodingTask((FragmentActivity) getContext());
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
        if (mShowingProgressBar) {
            return;
        }
        mShowingProgressBar = true;
        mProgressBar.setVisibility(View.VISIBLE);
        mUseMyLocationBtn.setVisibility(View.INVISIBLE);
    }

    private void hideProgressBar() {
        if (!mShowingProgressBar) {
            return;
        }
        mShowingProgressBar = false;
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
                mLastLocation = null;
                resumeLocationProvider();
            }
        }
    }

    private void cancelCurrentEdit() {
        if (mIsUsingMyLocation) {
            clearEditTextFocus();
            resumeLocationProvider();
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
        if (mLocationProvider != null) {
            Location mostRecentLocation = mLocationProvider.getLocation();
            if (mostRecentLocation == null) {
                mWaitingForFirstLocationFix = true;
                showProgressBar();
            } else {
                // We set these two variables to null to force the update of everything, otherwise
                // inside "onNewLocation" we would detect that things haven't changed and we would 
                // not do anything
                mLastLocation = null;
                mPrettyAddress = null;
                onNewLocation(mostRecentLocation, true);
            }
        }
    }

    private void clearEditTextFocus() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAddressEditText.getWindowToken(), 0);
        mAddressEditText.clearFocus();
    }

    public boolean isUsingMyLocation() {
        return mIsUsingMyLocation;
    }

    public void resume() {
        resumeLocationProvider();
    }

    public void pause() {
        pauseLocationProvider();
    }

    private void resumeLocationProvider() {
        mIsLocationProviderPaused = false;
        if (mIsUsingMyLocation && mLocationProvider != null) {
            Log.v(TAG, "Resuming location provider");
            mLocationProvider.setAddressView(this);
            applyMostRecentLocation();
        }
    }

    private void pauseLocationProvider() {
        if (mWaitingForFirstLocationFix) {
            mWaitingForFirstLocationFix = false;
            hideProgressBar();
        }
        mIsLocationProviderPaused = true;
        if (mLocationProvider != null) {
            Log.v(TAG, "Pausing location provider");
            mLocationProvider.setAddressView(null);
        }
    }
}
