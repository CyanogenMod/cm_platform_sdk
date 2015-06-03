/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.samples.customtiles;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;


import android.view.View;
import android.widget.Button;

import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;

import org.cyanogenmod.samples.customtiles.R;

import java.util.ArrayList;

/**
 * Example sample activity to publish a tile with a toggle state
 */
public class MainActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE = 0;
    public static final int CUSTOM_TILE_ID = 23;
    public static final String ACTION_TOGGLE_STATE =
            "org.cyanogenmod.samples.customtiles.ACTION_TOGGLE_STATE";
    public static final String STATE = "state";

    private Button mCustomTileButton;
    private CustomTile mCustomTile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCustomTileButton = (Button) findViewById(R.id.custom_tile_button);

        mCustomTileButton.setOnClickListener(this);
        Intent intent = new Intent();
        intent.setAction(ACTION_TOGGLE_STATE);
        intent.putExtra(MainActivity.STATE, States.STATE_OFF);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(this, 0,
                        intent , PendingIntent.FLAG_UPDATE_CURRENT);

        ArrayList<CustomTile.ExpandedListItem> expandedGridItems =
                new ArrayList<CustomTile.ExpandedListItem>();
        for (int i = 0; i < 4; i++) {
            CustomTile.ExpandedListItem expandedGridItem = new CustomTile.ExpandedListItem();
            expandedGridItem.setExpandedListItemDrawable(R.drawable.ic_launcher);
            expandedGridItem.setExpandedListItemTitle("Test: " + i);
            expandedGridItem.setExpandedListItemSummary("FUUCK YAAAAHH");
            expandedGridItems.add(expandedGridItem);
        }

        CustomTile.ListExpandedStyle gridExpandedStyle = new CustomTile.ListExpandedStyle();
        gridExpandedStyle.setListItems(expandedGridItems);

        mCustomTile = new CustomTile.Builder(this)
                .setContentDescription("Generic content description")
                .setLabel("CustomTile " + States.STATE_OFF)
                .setExpandedStyle(gridExpandedStyle)
                .setIcon(R.drawable.ic_launcher)
                .build();
    }

    @Override
    public void onClick(View v) {
        CMStatusBarManager.getInstance(this)
                .publishTile(CUSTOM_TILE_ID, mCustomTile);
    }
}
