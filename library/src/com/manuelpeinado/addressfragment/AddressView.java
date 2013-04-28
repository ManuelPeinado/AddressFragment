package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
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

import com.manuelpeinado.addressfragment.GeocodingTask.GeocodingTaskListener;
import com.manuelpeinado.addressfragment.SingleShotLocationTask.SingleShotLocationListener;

/**
 *
 */
public class AddressView extends LinearLayout implements IAddressProvider, OnClickListener,
        ReverseGeocodingTask.ReverseGeocodingListener, OnEditorActionListener, GeocodingTaskListener,
        OnItemClickListener, OnFocusChangeListener {

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
    private boolean mPaused = true;
    private boolean mReadOnly;
    private boolean mShowMyLocationButton = true;

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
        // This is important or else we get bitten by this:
        // http://stackoverflow.com/questions/15024892/two-searchviews-in-one-activity-and-screen-rotation
        mAddressEditText.setSaveEnabled(false);
        mAddressEditText.setTag(getTag());
        mAddressEditText.setAdapter(new AutocompleteAddressAdapter(context));
        // We use this listener to find out when the user has clicked "Done" on the virtual keyboard
        mAddressEditText.setOnEditorActionListener(this);
        // We use this listener to find out when the user has selected a item from the autocompletion dropdown
        mAddressEditText.setOnItemClickListener(this);
        mAddressEditText.setOnFocusChangeListener(this);
        mUseMyLocationBtn = (ImageView) findViewById(R.id.useMyLocationButton);
        mUseMyLocationBtn.setOnClickListener(this);

        updateTextInMyLocationMode();
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
        if (mIsUsingMyLocation) {
            // If we were showing "My location" now we need to show the hint "Waiting for location"
            setText("");
        }
        if (!mIsLocationProviderPaused) {
            resumeLocationProvider();
        }
        Log.v(TAG, "Setting new location provider");
    }

    private void setText(String text) {
        mAddressEditText.setText(text);
    }

    /**
     * This method is used by the location provider to inform us that the device
     * location has changed
     */
    public void setLocation(Location newLocation, boolean isUserInitiated) {
        if (mWaitingForFirstLocationFix) {
            mWaitingForFirstLocationFix = false;
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

    public void setUsingMyLocation(boolean value) {
        setUsingMyLocation(value, false);
    }

    private void setUsingMyLocation(boolean value, boolean force) {
        if (!force && mIsUsingMyLocation == value) {
            return;
        }
        mIsUsingMyLocation = value;
        if (mIsUsingMyLocation) {
            if (mLocationProvider != null && !mPaused) {
                resumeLocationProvider();
            }
            updateTextInMyLocationMode();
        } else {
            if (mLocationProvider == null) {
                setText("");
                showDefaultHint();
            } else {
                pauseLocationProvider();
            }
        }
    }

    public boolean isUsingMyLocation() {
        return mIsUsingMyLocation;
    }

    public void resume() {
        mPaused = false;
        resumeLocationProvider();
    }

    public void pause() {
        mPaused = true;
        pauseLocationProvider();
    }

    /**
     * This class allows us to save the state of an address view as a parcelable
     * instance It is for example possible to save the instance state of an
     * address view like this: Bundle out = new Bundle();
     * out.putParcelable("address", addressView.getState()); And then to restore
     * the state: // Bundle in addressView.setState(in);
     */
    public static class State implements Parcelable {
        boolean mIsUsingMyLocation;
        String mEditTextContent;

        public State() {
        }

        private State(Parcel in) {
            readFromParcel(in);
        }

        private void readFromParcel(Parcel in) {
            mIsUsingMyLocation = in.readInt() == 1;
            mEditTextContent = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(mIsUsingMyLocation ? 1 : 0);
            out.writeString(mEditTextContent);
        }

        public static final Parcelable.Creator<State> CREATOR = new Parcelable.Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                return new State(in);
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }

    public void swapWith(AddressView other) {
        State otherState = other.getState();
        State thisState = getState();
        setState(otherState);
        other.setState(thisState);
    }

    public void setState(State newState) {
        if (newState.mIsUsingMyLocation) {
            setUsingMyLocation(true, true);
        } else {
            search(newState.mEditTextContent, false);
        }
    }

    public State getState() {
        State state = new State();
        state.mIsUsingMyLocation = mIsUsingMyLocation;
        if (!mIsUsingMyLocation) {
            state.mEditTextContent = getAddressText();
        }
        return state;
    }

    private void updateTextInMyLocationMode() {
        if (!mIsUsingMyLocation) {
            return;
        }
        if (mLocationProvider == null) {
            setText(getResources().getString(R.string.aet__my_location));
        } else if (mWaitingForFirstLocationFix) {
            showWaitingForLocationHint();
        }
    }

    private void updateText() {
        AutocompleteAddressAdapter adapter = (AutocompleteAddressAdapter) mAddressEditText.getAdapter();
        mAddressEditText.setAdapter(null);
        setText(mPrettyAddress);
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
        showProgressBar();
    }

    @Override
    public void onReverseGeocodingResultReady(ReverseGeocodingTask sender, Address result) {
        mReverseGeocodingTask = null;
        hideProgressBar();
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
        notifyListener(mLastLocation, mIsReverseGeocodingTaskUserInitiated);
    }

    private void notifyListener(Location location, boolean isUserProvided) {
        if (mOnNewAddressListener != null) {
            if (location == null) {
                location = Utils.addressToLocation(mAddress);
            }
            mOnNewAddressListener.onNewAddress(this, mAddress, location, isUserProvided);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
            Log.v(TAG, "Editor action IME_ACTION_SEARCH");
            String addressText = getAddressText();
            search(addressText, true);
            return true;
        }
        return false;
    }

    /**
     * Invoking this method is equivalent to the user entering text and clicking
     * "search"
     */
    public void search(String text) {
        search(text, false);
    }

    /**
     * Prevent the user from editing the address, but the address is still
     * modifiable programmatically
     */
    public void setReadOnly(boolean value) {
        if (mReadOnly == value) {
            return;
        }
        mReadOnly = value;
        Utils.setEditTextReadOnly(mAddressEditText, mReadOnly);
    }

    public void setShowMyLocationButton(boolean value) {
        if (mShowMyLocationButton == value) {
            return;
        }
        mShowMyLocationButton = value;
        updateButtonVisibility();
    }

    /**
     * Changes the text that is shown when the input field is empty. By default
     * the hint is "Enter address here"
     */
    public void setHint(int resId) {
        mAddressEditText.setHint(resId);
    }

    private void updateButtonVisibility() {
        boolean shouldShowMyLocationBtn = mShowMyLocationButton && !mShowingProgressBar;
        mUseMyLocationBtn.setVisibility(shouldShowMyLocationBtn ? View.VISIBLE : View.GONE);
        mProgressBar.setVisibility(mShowingProgressBar ? View.VISIBLE : View.GONE);
    }

    private void search(String text, boolean showDisambiguationDialog) {
        setText(text);
        clearEditTextFocus();
        mIsUsingMyLocation = false;
        if (!TextUtils.isEmpty(text)) {
            startGeocoding(text, showDisambiguationDialog);
        }
    }

    // TODO comment this
    public void resolveAddress(final Callback<Location> callback) {
        if (!mIsUsingMyLocation || mLocationProvider != null) {
            callback.onResultReady(mLastLocation);
            return;
        }
        // This means that the text field is showing "My location", which is unknown. So we must 
        // resolve it using a single shot location request
        SingleShotLocationTask request = new SingleShotLocationTask(getContext());
        request.getLocation(new SingleShotLocationListener() {
            @Override
            public void onLocationReady(SingleShotLocationTask sender, Location location) {
                callback.onResultReady(location);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        onEditorAction(mAddressEditText, EditorInfo.IME_ACTION_DONE, null);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            hideProgressBar();
            mUseMyLocationBtn.setImageResource(R.drawable.ic_navigation_cancel);
            pauseLocationProvider();
        } else {
            mUseMyLocationBtn.setImageResource(R.drawable.ic_device_access_location_found);
        }
    }

    private void startGeocoding(String addressText, boolean showDisambiguationDialog) {
        Log.v(TAG, "Starting geocoding of address " + addressText);
        cancelPendingTasks();
        mGeocodingTask = new GeocodingTask((FragmentActivity) getContext());
        mGeocodingTask.showDisambiguationDialog(showDisambiguationDialog);
        mGeocodingTask.setListener(this);
        mGeocodingTask.execute(addressText);
        showProgressBar();
    }

    @Override
    public void onGeocodingResultReady(GeocodingTask sender, Address result) {
        mGeocodingTask = null;
        hideProgressBar();
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
        notifyListener(null, true);
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
        return mAddressEditText.getText().toString().trim();
    }

    private void showProgressBar() {
        if (mShowingProgressBar) {
            return;
        }
        mShowingProgressBar = true;
        updateButtonVisibility();
    }

    private void hideProgressBar() {
        if (!mShowingProgressBar) {
            return;
        }
        mShowingProgressBar = false;
        updateButtonVisibility();
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
                setUsingMyLocation(true);
            }
        }
    }

    private void cancelCurrentEdit() {
        if (mIsUsingMyLocation) {
            clearEditTextFocus();
            updateTextInMyLocationMode();
            resumeLocationProvider();
        } else {
            // If we were not on "my location" mode when the edit began, we restore the address
            // that was in the edittext when the edit began
            setText(mPrettyAddress);
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
                updateTextInMyLocationMode();
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

    private void showWaitingForLocationHint() {
        mAddressEditText.setHint(R.string.aet__waiting_for_location);
    }

    private void showDefaultHint() {
        mAddressEditText.setHint(R.string.aet__address_edit_text_hint);
    }

    private void clearEditTextFocus() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAddressEditText.getWindowToken(), 0);
        mAddressEditText.clearFocus();
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
        if (mIsLocationProviderPaused) {
            return;
        }
        if (mWaitingForFirstLocationFix) {
            mWaitingForFirstLocationFix = false;
            showDefaultHint();
        }
        mIsLocationProviderPaused = true;
        if (mLocationProvider != null) {
            Log.v(TAG, "Pausing location provider");
            mLocationProvider.setAddressView(null);
        }
    }

    private static class SavedState extends BaseSavedState {
        private State stateToSave;

        public SavedState(Parcelable superState, State state) {
            super(superState);
            stateToSave = state;
        }

        private SavedState(Parcel in) {
            super(in);
            this.stateToSave = in.readParcelable(State.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(this.stateToSave, flags);
        }

        //required field that makes Parcelables from a Parcel
        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Log.v(TAG, "Saving instance state " + getTag());
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getState());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Log.v(TAG, "Restoring instance state " + getTag());
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setState(ss.stateToSave);
    }
}
