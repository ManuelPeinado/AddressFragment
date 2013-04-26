package com.manuelpeinado.addressfragment.demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.manuelpeinado.addressfragment.AddressView;

public class DirectionsDialog extends DialogFragment implements OnClickListener {

    private AddressView mStartAddressView;
    private AddressView mEndAddressView;

    public static DirectionsDialog newInstance() {
        return new DirectionsDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_title_directions);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, null);
        ViewGroup view = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_directions, null);
        builder.setView(view);

        mStartAddressView = (AddressView)view.findViewById(R.id.start);
        mEndAddressView = (AddressView)view.findViewById(R.id.end);
        mEndAddressView.setUsingMyLocation(false);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }
}