package com.manuelpeinado.addressfragment.demo;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.manuelpeinado.addressfragment.AddressFragment;
import com.manuelpeinado.addressfragment.AddressView;

public class PopularSearchesActivity extends SherlockFragmentActivity implements OnItemClickListener {

    private ListView mListView;
    private AddressView mAddressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_searches);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(this);

        FragmentManager fm = getSupportFragmentManager();
        AddressFragment addressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressView = addressFragment.getAddressView();
        mAddressView.setUsingMyLocation(false);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String text = (String) mListView.getAdapter().getItem(position);
        mAddressView.search(text);
    }
}