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

public class DirectionsDialog extends DialogFragment implements android.view.View.OnClickListener {

    private AddressView mStartAddressView;
    private AddressView mEndAddressView;
    private View mSwapButton;
    private OnAcceptButtonClickListener mListener;

    public interface OnAcceptButtonClickListener {
        void onAcceptButtonClick();
    }

    public static DirectionsDialog newInstance() {
        return new DirectionsDialog();
    }
    
    public void setOnAcceptButtonClickListener(OnAcceptButtonClickListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_title_directions);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO check that both addresses has been introduced
                if (mListener != null) {
                    mListener.onAcceptButtonClick();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        ViewGroup view = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_directions, null);
        builder.setView(view);

        mStartAddressView = (AddressView) view.findViewById(R.id.start);
        mEndAddressView = (AddressView) view.findViewById(R.id.end);
        mEndAddressView.setUsingMyLocation(false);
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