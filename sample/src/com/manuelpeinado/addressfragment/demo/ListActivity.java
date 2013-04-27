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
import com.manuelpeinado.addressfragment.AddressView;
import com.manuelpeinado.addressfragment.AddressView.OnNewAddressListener;
import com.manuelpeinado.addressfragment.Callback;
import com.manuelpeinado.addressfragment.demo.apiclients.wikilocation.Article;
import com.manuelpeinado.addressfragment.demo.apiclients.wikilocation.WikiLocationClient;

public class ListActivity extends SherlockFragmentActivity implements OnNewAddressListener, Callback<List<Article>>,
        OnItemClickListener {

    private ListView mListView;
    private AddressView mAddressView;
    private WikiLocationClient client = new WikiLocationClient();
    private ArrayAdapter<Article> mAdapter;
    private View mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mProgress = findViewById(R.id.progress);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(this);

        FragmentManager fm = getSupportFragmentManager();
        AddressFragment addressFragment = (AddressFragment) fm.findFragmentById(R.id.address);
        mAddressView = addressFragment.getAddressView();
        mAddressView.setHandlesOwnLocation(true);
        mAddressView.setOnNewAddressListener(this);
    }

    @Override
    public void onNewAddress(AddressView sender, Address address, Location location, boolean isUserProvided) {
        showProgress();
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
        showList();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Article article = mAdapter.getItem(position);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(article.mobileUrl));
        startActivity(i);
    }
    
    private void showList() {
        mListView.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.INVISIBLE);
    }
    
    private void showProgress() {
        mListView.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.VISIBLE);
    }
}
