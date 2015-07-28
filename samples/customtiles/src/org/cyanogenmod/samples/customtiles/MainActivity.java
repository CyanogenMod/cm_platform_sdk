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
    public static final int CUSTOM_TILE_ID = 1;
    public static final int CUSTOM_TILE_LIST_ID = 2;
    public static final int CUSTOM_TILE_GRID_ID = 3;
    public static final String ACTION_TOGGLE_STATE =
            "org.cyanogenmod.samples.customtiles.ACTION_TOGGLE_STATE";
    public static final String STATE = "state";

    private Button mCustomTileButton;
    private Button mCustomTileButtonExpandedStyleList;
    private Button mCustomTileButtonExpandedStyleGrid;
    private CustomTile mCustomTile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCustomTileButton = (Button) findViewById(R.id.custom_tile_button);
        mCustomTileButton.setOnClickListener(this);

        mCustomTileButtonExpandedStyleList =
                (Button) findViewById(R.id.custom_tile_list_expanded_button);
        mCustomTileButtonExpandedStyleList.setOnClickListener(this);

        mCustomTileButtonExpandedStyleGrid =
                (Button) findViewById(R.id.custom_tile_grid_expanded_button);
        mCustomTileButtonExpandedStyleGrid.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.setAction(ACTION_TOGGLE_STATE);
        intent.putExtra(MainActivity.STATE, States.STATE_OFF);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(this, 0,
                        intent , PendingIntent.FLAG_UPDATE_CURRENT);

        switch (v.getId()) {
            case R.id.custom_tile_button:
                mCustomTile = new CustomTile.Builder(this)
                        .setOnClickIntent(pendingIntent)
                        .setContentDescription("Generic content description")
                        .setLabel("CustomTile " + States.STATE_OFF)
                        .shouldCollapsePanel(false)
                        .setIcon(R.drawable.ic_launcher)
                        .build();
                CMStatusBarManager.getInstance(this)
                        .publishTile(CUSTOM_TILE_ID, mCustomTile);
                break;
            case R.id.custom_tile_list_expanded_button:
                ArrayList<CustomTile.ExpandedListItem> expandedListItems =
                        new ArrayList<CustomTile.ExpandedListItem>();
                for (int i = 0; i < 100; i++) {
                    CustomTile.ExpandedListItem expandedListItem = new CustomTile.ExpandedListItem();
                    expandedListItem.setExpandedListItemDrawable(R.drawable.ic_launcher);
                    expandedListItem.setExpandedListItemTitle("Test: " + i);
                    expandedListItem.setExpandedListItemSummary("Test item summary " + i);
                    expandedListItem.setExpandedListItemOnClickIntent(pendingIntent);
                    expandedListItems.add(expandedListItem);
                }

                CustomTile.ListExpandedStyle listExpandedStyle = new CustomTile.ListExpandedStyle();
                listExpandedStyle.setListItems(expandedListItems);

                mCustomTile = new CustomTile.Builder(this)
                        .setExpandedStyle(listExpandedStyle)
                        .setContentDescription("Generic content description")
                        .setLabel("CustomTile Expanded List")
                        .setIcon(R.drawable.ic_launcher)
                        .build();
                CMStatusBarManager.getInstance(this)
                        .publishTile(CUSTOM_TILE_LIST_ID, mCustomTile);
                break;
            case R.id.custom_tile_grid_expanded_button:
                ArrayList<CustomTile.ExpandedGridItem> expandedGridItems =
                        new ArrayList<CustomTile.ExpandedGridItem>();
                for (int i = 0; i < 8; i++) {
                    CustomTile.ExpandedGridItem expandedGridItem = new CustomTile.ExpandedGridItem();
                    expandedGridItem.setExpandedGridItemDrawable(R.drawable.ic_launcher);
                    expandedGridItem.setExpandedGridItemTitle("Test: " + i);
                    expandedGridItem.setExpandedGridItemOnClickIntent(pendingIntent);
                    expandedGridItems.add(expandedGridItem);
                }

                CustomTile.GridExpandedStyle gridExpandedStyle = new CustomTile.GridExpandedStyle();
                gridExpandedStyle.setGridItems(expandedGridItems);
                mCustomTile = new CustomTile.Builder(this)
                        .setExpandedStyle(gridExpandedStyle)
                        .setContentDescription("Generic content description")
                        .setLabel("CustomTile Expanded Grid")
                        .setIcon(R.drawable.ic_launcher)
                        .build();
                CMStatusBarManager.getInstance(this)
                        .publishTile(CUSTOM_TILE_GRID_ID, mCustomTile);
                break;
        }
    }
}
