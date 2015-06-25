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

package org.cyanogenmod.tests.customtiles;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;

import cyanogenmod.app.CustomTile;
import cyanogenmod.app.CMStatusBarManager;

import org.cyanogenmod.tests.R;

import org.cyanogenmod.tests.TestActivity;

import java.util.ArrayList;

public class CMStatusBarTest extends TestActivity {

    private static final int CUSTOM_TILE_ID = 1337;
    private static final int CUSTOM_TILE_SETTINGS_ID = 1336;
    private CustomTile mCustomTile;
    private CMStatusBarManager mCMStatusBarManager;

    Handler mHandler = new Handler();

    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        mCMStatusBarManager = CMStatusBarManager.getInstance(this);
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("test publish swagger") {
                public void run() {
                    PendingIntent intent = PendingIntent.getActivity(CMStatusBarTest.this, 0,
                            new Intent(CMStatusBarTest.this, CMStatusBarTest.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
                    mCustomTile = new CustomTile.Builder(CMStatusBarTest.this)
                            .setLabel("Swagger")
                            .setIcon(R.drawable.ic_swagger)
                            .setOnClickIntent(intent)
                            .setContentDescription("Swag description")
                            .build();
                    mCMStatusBarManager.publishTile(CUSTOM_TILE_ID, mCustomTile);
                }
            },

            new Test("test publish swagger in 3 seconds") {
                public void run() {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            PendingIntent intent = PendingIntent.getActivity(CMStatusBarTest.this, 0,
                                    new Intent(CMStatusBarTest.this, CMStatusBarTest.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
                            mCustomTile = new CustomTile.Builder(CMStatusBarTest.this)
                                    .setLabel("Swag 3 seconds")
                                    .setIcon(R.drawable.ic_swagger)
                                    .setOnClickIntent(intent)
                                    .setContentDescription("Swag description")
                                    .build();
                            mCMStatusBarManager.publishTile(CUSTOM_TILE_ID, mCustomTile);
                        }
                    }, 3000);
                }
            },

            new Test("test update tile") {
                public void run() {
                    if (mCustomTile != null) {
                        mCustomTile.label = "Update From SDK";
                        mCMStatusBarManager.publishTile(CUSTOM_TILE_ID, mCustomTile);
                    }
                }
            },

            new Test("test remove swagger - NO. DON\'T PRESS THIS") {
                public void run() {
                    mCMStatusBarManager.removeTile(CUSTOM_TILE_ID);
                }
            },

            new Test("test remove swagger in 3 seconds") {
                public void run() {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            mCMStatusBarManager.removeTile(CUSTOM_TILE_ID);
                        }
                    }, 3000);
                }
            },

            new Test("test publish swagger with settings") {
                public void run() {
                    CustomTile customTile = new CustomTile.Builder(CMStatusBarTest.this)
                            .setLabel("Test Swag Settings From SDK")
                            .setIcon(R.drawable.ic_swagger)
                            .setOnSettingsClickIntent(new Intent(CMStatusBarTest.this,
                                    DummySettings.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            .setContentDescription("Swag description")
                            .build();
                    CMStatusBarManager.getInstance(CMStatusBarTest.this)
                            .publishTile(CUSTOM_TILE_SETTINGS_ID, customTile);
                }
            },

            new Test("test publish swagger with expanded list") {
                public void run() {
                    PendingIntent intent = PendingIntent.getActivity(CMStatusBarTest.this, 0,
                            new Intent(CMStatusBarTest.this, CMStatusBarTest.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
                    ArrayList<CustomTile.ExpandedListItem> expandedListItems =
                            new ArrayList<CustomTile.ExpandedListItem>();
                    for (int i = 0; i < 100; i++) {
                        CustomTile.ExpandedListItem expandedListItem =
                                new CustomTile.ExpandedListItem();
                        expandedListItem.setExpandedListItemDrawable(R.drawable.ic_swagger);
                        expandedListItem.setExpandedListItemTitle("Swag level: " + i);
                        if (i==99)
                            expandedListItem.setExpandedListItemSummary("Set the swaggest swag level");
                        else
                            expandedListItem.setExpandedListItemSummary("Set swag level to " + i);
                        expandedListItem.setExpandedListItemOnClickIntent(intent);
                        expandedListItems.add(expandedListItem);
                    }

                    CustomTile.ListExpandedStyle listExpandedStyle =
                            new CustomTile.ListExpandedStyle();
                    listExpandedStyle.setListItems(expandedListItems);
                    CustomTile customTile = new CustomTile.Builder(CMStatusBarTest.this)
                            .setLabel("Swagger")
                            .setIcon(R.drawable.ic_swagger)
                            .setExpandedStyle(listExpandedStyle)
                            .setOnSettingsClickIntent(new Intent(CMStatusBarTest.this,
                                    DummySettings.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            .setContentDescription("Swag description")
                            .build();
                    CMStatusBarManager.getInstance(CMStatusBarTest.this)
                            .publishTile(CUSTOM_TILE_SETTINGS_ID, customTile);
                }
            },

            new Test("test publish swagger with expanded grid") {
                public void run() {
                    PendingIntent intent = PendingIntent.getActivity(CMStatusBarTest.this, 0,
                            new Intent(CMStatusBarTest.this, CMStatusBarTest.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
                    ArrayList<CustomTile.ExpandedGridItem> expandedGridItems =
                            new ArrayList<CustomTile.ExpandedGridItem>();
                    for (int i = 0; i < 6; i++) {
                        CustomTile.ExpandedGridItem expandedGridItem =
                                new CustomTile.ExpandedGridItem();
                        expandedGridItem.setExpandedGridItemDrawable(R.drawable.ic_swagger);
                        if (i==5)
                            expandedGridItem.setExpandedGridItemTitle("No moar swag icons :( ");
                        else
                            expandedGridItem.setExpandedGridItemTitle("Swag icon: " + i);
                        expandedGridItem.setExpandedGridItemOnClickIntent(intent);
                        expandedGridItems.add(expandedGridItem);
                    }

                    CustomTile.GridExpandedStyle gridExpandedStyle =
                            new CustomTile.GridExpandedStyle();
                    gridExpandedStyle.setGridItems(expandedGridItems);
                    CustomTile customTile = new CustomTile.Builder(CMStatusBarTest.this)
                            .setLabel("Swagger Grid")
                            .setIcon(R.drawable.ic_swagger)
                            .setExpandedStyle(gridExpandedStyle)
                            .setOnSettingsClickIntent(new Intent(CMStatusBarTest.this,
                                    DummySettings.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            .setContentDescription("Swag description")
                            .build();
                    CMStatusBarManager.getInstance(CMStatusBarTest.this)
                            .publishTile(CUSTOM_TILE_SETTINGS_ID, customTile);
                }
            }
    };
}
