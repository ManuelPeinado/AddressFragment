package com.manuelpeinado.addressfragment.demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.manuelpeinado.addressfragment.AddressView;
import com.manuelpeinado.addressfragment.AddressView.State;

public class DirectionsDialog extends DialogFragment implements android.view.View.OnClickListener {

    private AddressView mStartAddressView;
    private AddressView mEndAddressView;
    private View mSwapButton;
    private OnAcceptButtonClickListener mListener;

    public interface OnAcceptButtonClickListener {
        void onAcceptButtonClick(DirectionsDialog sender);
    }

    public static DirectionsDialog newInstance(State startAddress, State endAddress) {
        DirectionsDialog dlg = new DirectionsDialog();
        Bundle args = new Bundle();
        if (startAddress != null) {
            args.putParcelable("startAddress", startAddress);
        }
        if (endAddress != null) {
            args.putParcelable("endAddress", endAddress);
        }
        dlg.setArguments(args);
        return dlg;
    }

    public void setOnAcceptButtonClickListener(OnAcceptButtonClickListener listener) {
        mListener = listener;
    }

    public AddressView getStartAddressView() {
        return mStartAddressView;
    }

    public AddressView getEndAddressView() {
        return mEndAddressView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_title_directions);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO check that both addresses have been introduced
                if (mListener != null) {
                    mListener.onAcceptButtonClick(DirectionsDialog.this);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        ViewGroup view = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_directions, null);
        builder.setView(view);

        mStartAddressView = (AddressView) view.findViewById(R.id.start);
        mEndAddressView = (AddressView) view.findViewById(R.id.end);
        Bundle args = getArguments();
        if (args.containsKey("startAddress")) {
            mStartAddressView.setState((AddressView.State) args.getParcelable("startAddress"));
        }
        if (args.containsKey("endAddress")) {
            mEndAddressView.setState((AddressView.State) args.getParcelable("endAddress"));
        } else if (savedInstanceState == null) {
            mEndAddressView.setUsingMyLocation(false);
        }
        mSwapButton = view.findViewById(R.id.swap);
        mSwapButton.setOnClickListener(this);

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        swapAddresses();
    }

    private void swapAddresses() {
        mStartAddressView.swapWith(mEndAddressView);
    }
}