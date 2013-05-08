package com.manuelpeinado.addressfragment;

import com.manuelpeinado.addressfragment.AddressView.State;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AddressFragment extends Fragment {

    protected static final String TAG = AddressFragment.class.getSimpleName();
    private AddressView mAddressView;
    private AttributeSet mAttrs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAddressView = new AddressView(getActivity(), mAttrs);
        mAttrs = null;
        if (savedInstanceState != null && savedInstanceState.containsKey("addressViewState")) {
            mAddressView.setState((State) savedInstanceState.getParcelable("addressViewState"));
        }
        return mAddressView;
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        mAttrs = attrs;
    }

    public AddressView getAddressView() {
        return mAddressView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAddressView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAddressView.pause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("addressViewState", mAddressView.getState());
    }
}
