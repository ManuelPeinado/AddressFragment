package com.manuelpeinado.addressfragment.demo;

import java.util.List;

import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.manuelpeinado.addressfragment.AddressFragment;
import com.manuelpeinado.addressfragment.AddressFragment.OnNewAddressListener;
import com.manuelpeinado.addressfragment.Callback;
import com.manuelpeinado.addressfragment.demo.apiclients.wikilocation.Article;
import com.manuelpeinado.addressfragment.demo.apiclients.wikilocation.WikiLocationClient;

public class ListActivity extends SherlockFragmentActivity implements OnNewAddressListener, Callback<List<Article>>,
        OnItemClickListener {

    private ListView mListView;
    private AddressFragment mAddressFragment;
    private WikiLocationClient client = new WikiLocationClient();
    private ArrayAdapter<Article> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(this);

        FragmentManager fm = getSupportFragmentManager();
        mAddressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressFragment.setHandlesOwnLocation(true);
        mAddressFragment.setOnNewAddressListener(this);
    }

    @Override
    public void onNewAddress(AddressFragment sender, Address address, Location location, boolean isUserProvided) {
        client.sendRequest(location.getLatitude(), location.getLongitude(), 1000, 10, this);
    }

    @Override
    public void onResultReady(List<Article> result) {
        if (result.size() == 0) {
            Utils.longToast(this, "No items have been found near the specified address");
        }
        int layout = android.R.layout.simple_list_item_1;
        mAdapter = new ArrayAdapter<Article>(this, layout, android.R.id.text1, result);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Article article = mAdapter.getItem(position);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(article.mobileUrl));
        startActivity(i);
    }
}
