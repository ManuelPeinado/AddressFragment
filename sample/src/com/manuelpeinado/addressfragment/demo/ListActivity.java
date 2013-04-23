package com.manuelpeinado.addressfragment.demo;

import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.manuelpeinado.addressfragment.AddressFragment;
import com.manuelpeinado.addressfragment.AddressFragment.OnNewAddressListener;

public class ListActivity extends SherlockFragmentActivity implements OnNewAddressListener {

    private ListView mListView;
    private AddressFragment mAddressFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mListView = (ListView)findViewById(R.id.list);
        
        FragmentManager fm = getSupportFragmentManager();
        mAddressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressFragment.setHandlesOwnLocation(true);
        mAddressFragment.setOnNewAddressListener(this);
    }

    @Override
    public void onNewAddress(AddressFragment sender, Address address, boolean isUserProvided) {
        
    }
}
