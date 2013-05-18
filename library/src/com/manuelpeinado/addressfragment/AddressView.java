package com.manuelpeinado.addressfragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
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
    static final int DEF_STYLE = R.attr.av__addressViewStyle;
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
    private boolean mShowMyLocation = true;
    private Address mAddress;
    private String mPrettyAddress;
    private GeocodingTask mGeocodingTask;
    private ReverseGeocodingTask mReverseGeocodingTask;
    private static boolean MOCK_REVERSE_GEOCODING_FAILURE = false;
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
    private ImageView mMyLocationButton;
    private ProgressBar mProgressBar;
    private boolean mWaitingForFirstLocationFix;
    private boolean mShowingProgressBar;
    private boolean mPaused = true;
    private boolean mReadOnly;
    /**
     * If false, the my location button is not shown. This is useful in
     * applications where for some reason the device location is not available,
     * and so manual input is the only choice.
     */
    private boolean mShowMyLocationButton = true;
    private boolean mIsSingleShot;
    private boolean mLocationProviderDisabled;
    private boolean mHideButtonAutomatically = true;
    private boolean mHasFocus;
    private int[] mAddressEditTextPadding;
    private float mButtonSize;
    private boolean mInitializing = true;
    private int mCancelIcon;
    private int mMyLocationIcon;
    private int mTextAppearance;
    private int mButtonBackground;
    private int mButtonPadding;
    /**
     * If true, instead of using the real location of the device we use a
     * simulated location that goes moves from Washington Square to Marcus
     * Garvey Park, in NYC. This is useful during developent, to see what
     * happens if the location changes fast without having to leave home
     */
    private static final boolean USE_MOCK_BUILT_IN_LOCATION_PROVIDER = false;

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
        this(context, attrs, DEF_STYLE);
    }

    public AddressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setOrientation(LinearLayout.HORIZONTAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LayoutInflater.from(context).inflate(R.layout.aet__default_layout, this);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mAddressEditText = (AutoCompleteTextView) findViewById(R.id.addressEditText);
        initAddressEditText();
        mAddressEditTextPadding = Utils.getPadding(mAddressEditText);
        mButtonSize = getResources().getDimension(R.dimen.aet__action_button_size);

        mMyLocationButton = (ImageView) findViewById(R.id.useMyLocationButton);
        mMyLocationButton.setOnClickListener(this);

        parseAttrs(attrs, defStyle);

        mInitializing = false;
    }

    private void initAddressEditText() {
        mAddressEditText.setFocusable(true);
        mAddressEditText.setFocusableInTouchMode(true);
        mAddressEditText.setClickable(true);
        mAddressEditText.setThreshold(2);
        mAddressEditText.setHint(R.string.aet__address_edit_text_hint);
        mAddressEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mAddressEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mAddressEditText.setMaxLines(1);
        mAddressEditText.setSingleLine(true);
        mAddressEditText.setSelectAllOnFocus(true);

        // This is important or else we get bitten by this:
        // http://stackoverflow.com/questions/15024892/two-searchviews-in-one-activity-and-screen-rotation
        mAddressEditText.setSaveEnabled(false);
        mAddressEditText.setTag(getTag());
        mAddressEditText.setAdapter(new AutocompleteAddressAdapter(getContext()));
        // We use this listener to find out when the user has clicked "Done" on the virtual keyboard
        mAddressEditText.setOnEditorActionListener(this);
        // We use this listener to find out when the user has selected a item from the autocompletion dropdown
        mAddressEditText.setOnItemClickListener(this);
        mAddressEditText.setOnFocusChangeListener(this);
    }

    void parseAttrs(AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AddressView, defStyle,
                    R.style.Widget_AV_AddressView);
            mReadOnly = a.getBoolean(R.styleable.AddressView_av__readOnly, mReadOnly);
            mShowMyLocation = a.getBoolean(R.styleable.AddressView_av__showMyLocation, mShowMyLocation);
            mCancelIcon = a.getResourceId(R.styleable.AddressView_av__cancelIcon, 0);
            mMyLocationIcon = a.getResourceId(R.styleable.AddressView_av__myLocationIcon, 0);
            mTextAppearance = a.getResourceId(R.styleable.AddressView_av__textAppearance, 0);
            mButtonBackground = a.getResourceId(R.styleable.AddressView_av__buttonBackground, 0);
            mButtonPadding = (int) a.getDimension(R.styleable.AddressView_av__buttonPadding, 0);
            a.recycle();
        }

        // Default visibility of the "my location" button is determined by whether we are in read-only mode
        mShowMyLocationButton = !mReadOnly;
        mMyLocationButton.setImageResource(mMyLocationIcon);
        setReadOnly(mReadOnly);
        mAddressEditText.setTextAppearance(getContext(), mTextAppearance);
        mMyLocationButton.setBackgroundResource(mButtonBackground);
        mMyLocationButton.setPadding(mButtonPadding, mButtonPadding, mButtonPadding, mButtonPadding);
        setShowMyLocation(mShowMyLocation, mInitializing);
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
        if (mShowMyLocation) {
            // If we were showing "My location" now we need to show the hint "Waiting for location"
            setText("");
            if (!mPaused) {
                resumeLocationProvider();
            }
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
            mShowMyLocation = false;
            updateButtonVisibility();
            pauseLocationProvider();
        } else if (!mShowMyLocation || mLocationProviderDisabled) {
            // We shouldn't receive non user-initiated locations from the provider, but in case
            // we receive one anyway we have to ignore it
            return;
        }

        Utils.logv(TAG, "setLocation", Utils.prettyPrint(newLocation));
        if (mShowMyLocation && mLocationProvider != null && mIsSingleShot) {
            disableLocationProvider();
        }

        onNewLocation(newLocation, isUserInitiated);
    }

    public void setHandlesOwnLocation(boolean value) {
        Log.v(TAG, "Handling own location: " + value);
        setLocationProvider(createBuiltInLocationProvider());
    }

    private LocationProvider createBuiltInLocationProvider() {
        return USE_MOCK_BUILT_IN_LOCATION_PROVIDER ? new MockBuiltInLocationProvider() : new BuiltInLocationProvider();
    }

    public void setUsingMyLocation(boolean value) {
        setShowMyLocation(value, false);
    }

    private void setShowMyLocation(boolean value, boolean force) {
        if (!force && mShowMyLocation == value) {
            return;
        }
        mShowMyLocation = value;
        updateButtonVisibility();
        if (mShowMyLocation) {
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

    public boolean getShowMyLocation() {
        return mShowMyLocation;
    }

    public void setSingleShot(boolean value) {
        if (mIsSingleShot == value) {
            return;
        }
        mIsSingleShot = value;
        if (!mShowMyLocation || mLocationProvider == null) {
            // If single shot mode is activated but we are not using my location
            // or we don't have a location provider then there's nothing to do
            return;
        }
        if (mIsSingleShot) {
            if (mLastLocation != null) {
                // If single shot mode is activated and we already had a location,
                // we don't need more so we disable the location provider
                disableLocationProvider();
            }
        } else {
            // If single shot mode is deactivated while in my location mode, we 
            // need to resume location updates
            enableLocationProvider();
        }
    }

    private void enableLocationProvider() {
        if (!mLocationProviderDisabled) {
            return;
        }
        mLocationProviderDisabled = false;
        resumeLocationProvider();
    }

    private void disableLocationProvider() {
        if (mLocationProviderDisabled) {
            return;
        }
        mLocationProviderDisabled = true;
        pauseLocationProvider();
    }

    /**
     * It is mandatory to call this method from your Activity's onResume method
     * unless you don't use the "handlesOwnLocation" mode
     */
    public void resume() {
        mPaused = false;
        resumeLocationProvider();
    }

    /**
     * It is mandatory to call this method from your Activity's onResume method
     * unless you don't use the "handlesOwnLocation" mode
     */
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
        boolean mShowMyLocation;
        String mEditTextContent;

        public State() {
        }

        private State(Parcel in) {
            readFromParcel(in);
        }

        private void readFromParcel(Parcel in) {
            mShowMyLocation = in.readInt() == 1;
            mEditTextContent = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(mShowMyLocation ? 1 : 0);
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
        if (newState.mShowMyLocation) {
            setShowMyLocation(true, true);
        } else {
            search(newState.mEditTextContent, false);
        }
    }

    public State getState() {
        State state = new State();
        state.mShowMyLocation = mShowMyLocation;
        if (!mShowMyLocation) {
            state.mEditTextContent = getAddressText();
        }
        return state;
    }

    private void updateTextInMyLocationMode() {
        if (!mShowMyLocation) {
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
        // We use the hasFocus test to prevent annoying the user by changing the 
        // contents of the edittext when she is in the middle of writing an address
        if (!mAddressEditText.hasFocus() || isUserInitiated) {
            if (Utils.isDifferentLocation(mLastLocation, newLocation)) {
                startReverseGeocodingTask(newLocation, isUserInitiated);
            } else {
                Log.v(TAG, "New location is too similar to previous one; ignoring it");
            }
        }
        // This seems to improve usability
        if (mAddressEditText.hasFocus() && isUserInitiated) {
            clearEditTextFocus();
        }
    }

    private void startReverseGeocodingTask(Location location, boolean isUserInitiated) {
        cancelPendingTasks();
        Log.v(TAG, "Starting reverse geocoding of location " + Utils.prettyPrint(location));
        mIsReverseGeocodingTaskUserInitiated = isUserInitiated;
        mReverseGeocodingTask = new ReverseGeocodingTask(getContext());
        if (MOCK_REVERSE_GEOCODING_FAILURE) {
            mReverseGeocodingTask.setMockFailure(true);
        }
        mReverseGeocodingTask.setListener(this);
        mReverseGeocodingTask.execute(location);
        showProgressBar();
    }

    @Override
    public void onReverseGeocodingResultReady(ReverseGeocodingTask sender, Address result) {
        mReverseGeocodingTask = null;
        hideProgressBar();
        // TODO use a different method to notify error to listeners
        if (result == null) {
            Utils.logv(TAG, "onReverseGeocodingResultReady", "Error in reverse geocoding");
            Utils.longToast(getContext(), R.string.aet__geocoding_error_toast);
            showDefaultHint();
            updateButtonVisibility();
            disableLocationProvider();
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
        if (!mInitializing && mReadOnly == value) {
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
        boolean shouldHideButton;
        if (mHideButtonAutomatically) {
            if (mHasFocus) {
                shouldHideButton = !mShowMyLocationButton;
            } else if (mShowMyLocation) {
                shouldHideButton = hasAddress() && !mShowingProgressBar && !mIsSingleShot;
            } else {
                shouldHideButton = !mShowingProgressBar && !mShowMyLocationButton;
            }
            int additionalRightPadding = (int) (shouldHideButton ? 0 : mButtonSize);
            mAddressEditText.setPadding(mAddressEditTextPadding[0], mAddressEditTextPadding[1],
                    mAddressEditTextPadding[2] + additionalRightPadding, mAddressEditTextPadding[3]);
            if (shouldHideButton) {
                mMyLocationButton.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                return;
            }
        }
        boolean shouldShowMyLocationBtn = mShowMyLocationButton && !mShowingProgressBar;
        mMyLocationButton.setVisibility(shouldShowMyLocationBtn ? View.VISIBLE : View.INVISIBLE);
        mProgressBar.setVisibility(mShowingProgressBar ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean hasAddress() {
        return mLastLocation != null && mAddress != null;
    }

    private void search(String text, boolean showDisambiguationDialog) {
        setText(text);
        clearEditTextFocus();
        mShowMyLocation = false;
        if (!TextUtils.isEmpty(text)) {
            startGeocoding(text, showDisambiguationDialog);
        }
    }

    // TODO comment this
    public void resolveAddress(final Callback<Location> callback) {
        if (!mShowMyLocation || mLocationProvider != null) {
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
        mHasFocus = hasFocus;
        if (hasFocus) {
            if (!mReadOnly) {
                Utils.forceShowVirtualKeyboard(getContext());
            }
            hideProgressBar();
            mMyLocationButton.setImageResource(mCancelIcon);
            pauseLocationProvider();
        } else {
            mMyLocationButton.setImageResource(mMyLocationIcon);
        }
        updateButtonVisibility();
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
    public void onGeocodingSuccess(GeocodingTask sender, Address result) {
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
    public void onGeocodingFailure(GeocodingTask sender) {
        Log.w(TAG, "Geocoding of address " + sender.getAddressText() + " failed");
        hideProgressBar();
        cancelCurrentEdit();
    }

    @Override
    public void onGeocodingCancel(GeocodingTask sender) {
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
            if (mShowMyLocation) {
                if ((!hasAddress() || mIsSingleShot) && mLocationProviderDisabled) {
                    // If the provider is disabled due to us being on single shot mode, the 
                    // user clicking on the my location button is interpreted as a request to
                    // obtain a new location fix. Also, if the last reverse geocoding failed (that's
                    // what the !hasAddress() is for)
                    enableLocationProvider();
                } else {
                    if (mOnMyLocationClickIgnoredListener != null) {
                        mOnMyLocationClickIgnoredListener.onMyLocationClickIgnored(this);
                    }
                }
            } else {
                setUsingMyLocation(true);
            }
        }
    }

    private void cancelCurrentEdit() {
        if (mShowMyLocation) {
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
        Utils.hideVirtualKeyboard(getContext(), mAddressEditText);
        mAddressEditText.clearFocus();
    }

    private void resumeLocationProvider() {
        if (mShowMyLocation && mLocationProvider != null) {
            Log.v(TAG, "Resuming location provider");
            mLocationProvider.setAddressView(this);
            applyMostRecentLocation();
        }
    }

    private void pauseLocationProvider() {
        if (mWaitingForFirstLocationFix) {
            mWaitingForFirstLocationFix = false;
            showDefaultHint();
        }
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
