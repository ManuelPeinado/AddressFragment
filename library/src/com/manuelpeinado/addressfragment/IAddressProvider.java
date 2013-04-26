package com.manuelpeinado.addressfragment;

import android.location.Location;

import com.manuelpeinado.addressfragment.AddressView.LocationProvider;
import com.manuelpeinado.addressfragment.AddressView.OnMyLocationClickIgnoredListener;
import com.manuelpeinado.addressfragment.AddressView.OnNewAddressListener;

public interface IAddressProvider {
    void setHandlesOwnLocation(boolean value);

    void setOnNewAddressListener(OnNewAddressListener listener);

    void setLocationProvider(LocationProvider provider);

    void setOnMyLocationClickIgnoredListener(OnMyLocationClickIgnoredListener listener);

    void setLocation(Location newLocation, boolean isUserInitiated);
}