/*
 * Copyright (C) 2013 Manuel Peinado
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.manuelpeinado.addressfragment.demo;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;

public class HomeActivity extends SherlockListActivity {
    private List<ActivityInfo> activitiesInfo = Arrays.asList(
            new ActivityInfo(MapReadOnlyActivity.class, R.string.activity_title_map_read_only),
            new ActivityInfo(MapActivity.class, R.string.activity_title_map),
            new ActivityInfo(VirtualWalkActivity.class, R.string.activity_title_virtual_walk),
            new ActivityInfo(SingleShotVirtualWalkActivity.class, R.string.activity_title_virtual_walk_single_shot),
            new ActivityInfo(ListActivity.class, R.string.activity_title_list),
            new ActivityInfo(DirectionsActivity.class, R.string.activity_title_directions),
            new ActivityInfo(PopularSearchesActivity.class, R.string.activity_title_popular_searches),
            new ActivityInfo(ActionBarActivity.class, R.string.activity_title_action_bar),
            new ActivityInfo(StylingActivity.class, R.string.activity_title_styling));
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        String[] titles = getActivityTitles();
        setListAdapter(new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, android.R.id.text1, titles));
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Class<? extends Activity> class_ = activitiesInfo.get(position).activityClass;
        Intent intent = new Intent(this, class_);
        startActivity(intent);
    }

    private String[] getActivityTitles() {
        String[] result = new String[activitiesInfo.size()];
        int i = 0;
        for (ActivityInfo info : activitiesInfo) {
            result[i++] = getString(info.titleResourceId);
        }
        return result;
    }
}
