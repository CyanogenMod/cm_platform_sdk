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

package org.cyanogenmod.tests.customtiles.unit;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.test.AndroidTestCase;

import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.RemoteViews;
import cyanogenmod.app.CustomTile;

import org.cyanogenmod.tests.R;
import org.cyanogenmod.tests.customtiles.DummySettings;

import java.util.ArrayList;

/**
 * Created by adnan on 7/15/15.
 */
public class CustomTileTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @SmallTest
    public void testCustomTileOnClickIntentUnravelFromParcel() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setOnClickIntent(pendingIntent)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.onClick);
        assertEquals(expectedCustomTile.onClick.getIntent().toString(),
                fromParcel.onClick.getIntent().toString());
    }

    @SmallTest
    public void testCustomTileOnSettingsClickIntentUnravelFromParcel() {
        Intent intent = new Intent(mContext, DummySettings.class);
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setOnSettingsClickIntent(intent)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.onSettingsClick);
        assertEquals(expectedCustomTile.onSettingsClick.toString(),
                fromParcel.onSettingsClick.toString());
    }

    @SmallTest
    public void testCustomTileUriUnravelFromParcel() {
        //TASKER!
        Uri uri = Uri.parse("http://tasker.dinglisch.net");
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setOnClickUri(uri)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.onClickUri);
        assertEquals(expectedCustomTile.onClickUri, fromParcel.onClickUri);
    }

    @SmallTest
    public void testCustomTileLabelUnravelFromParcel() {
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setLabel("Test Text SDK")
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.label);
        assertEquals(expectedCustomTile.label, fromParcel.label);
    }

    @SmallTest
    public void testCustomTileContentDescriptionUnravelFromParcel() {
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setContentDescription("Test Context Description SDK")
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.contentDescription);
        assertEquals(expectedCustomTile.contentDescription, fromParcel.contentDescription);
    }

    @SmallTest
    public void testCustomTileIconUnravelFromParcel() {
        int resourceInt = R.drawable.ic_launcher;
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setIcon(resourceInt)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.icon);
        assertNotSame(fromParcel.icon, 0);
        assertEquals(expectedCustomTile.icon, fromParcel.icon);
    }

    @SmallTest
    public void testCustomTileCollapsePanelUnravelFromParcel() {
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .shouldCollapsePanel(true)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertEquals(expectedCustomTile.collapsePanel, fromParcel.collapsePanel);
    }

    @SmallTest
    public void testCustomTileSensitiveDataUnravelFromParcel() {
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .hasSensitiveData(true)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertEquals(expectedCustomTile.sensitiveData, fromParcel.sensitiveData);
    }

    @MediumTest
    public void testCustomTileExpandedListStyleUnravelFromParcel() {
        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, DummySettings.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
        ArrayList<CustomTile.ExpandedListItem> expandedListItems =
                new ArrayList<CustomTile.ExpandedListItem>();
        for (int i = 0; i < 100; i++) {
            CustomTile.ExpandedListItem expandedListItem =
                    new CustomTile.ExpandedListItem();
            expandedListItem.setExpandedListItemDrawable(R.drawable.ic_launcher);
            expandedListItem.setExpandedListItemTitle("Test: " + i);
            expandedListItem.setExpandedListItemSummary("Test item summary " + i);
            expandedListItem.setExpandedListItemOnClickIntent(intent);
            expandedListItems.add(expandedListItem);
        }

        CustomTile.ListExpandedStyle listExpandedStyle =
                new CustomTile.ListExpandedStyle();
        listExpandedStyle.setListItems(expandedListItems);
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setExpandedStyle(listExpandedStyle)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.expandedStyle);
        assertEquals(expectedCustomTile.expandedStyle.getStyle(),
                fromParcel.expandedStyle.getStyle());
        assertNotNull(fromParcel.expandedStyle.getExpandedItems());
        for (int j = 0; j < 100; j++) {
            CustomTile.ExpandedItem itemExpected = expandedListItems.get(j);
            CustomTile.ExpandedItem itemReal = fromParcel.expandedStyle.getExpandedItems()[j];
            assertEquals(itemExpected.onClickPendingIntent, itemReal.onClickPendingIntent);
            assertEquals(itemExpected.itemDrawableResourceId, itemReal.itemDrawableResourceId);
            assertEquals(itemExpected.itemTitle, itemReal.itemTitle);
            assertEquals(itemExpected.itemSummary, itemReal.itemSummary);
        }
    }

    @MediumTest
    public void testCustomTileExpandedGridStyleUnravelFromParcel() {
        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, DummySettings.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
        ArrayList<CustomTile.ExpandedGridItem> expandedGridItems =
                new ArrayList<CustomTile.ExpandedGridItem>();
        for (int i = 0; i < 100; i++) {
            CustomTile.ExpandedGridItem expandedGridItem =
                    new CustomTile.ExpandedGridItem();
            expandedGridItem.setExpandedGridItemDrawable(R.drawable.ic_launcher);
            expandedGridItem.setExpandedGridItemTitle("Test: " + i);
            expandedGridItem.setExpandedGridItemOnClickIntent(intent);
            expandedGridItems.add(expandedGridItem);
        }

        CustomTile.GridExpandedStyle gridExpandedStyle =
                new CustomTile.GridExpandedStyle();
        gridExpandedStyle.setGridItems(expandedGridItems);
        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setExpandedStyle(gridExpandedStyle)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.expandedStyle);
        assertEquals(expectedCustomTile.expandedStyle.getStyle(),
                fromParcel.expandedStyle.getStyle());
        assertNotNull(fromParcel.expandedStyle.getExpandedItems());
        for (int j = 0; j < 100; j++) {
            CustomTile.ExpandedItem itemExpected = expandedGridItems.get(j);
            CustomTile.ExpandedItem itemReal = fromParcel.expandedStyle.getExpandedItems()[j];
            assertEquals(itemExpected.onClickPendingIntent, itemReal.onClickPendingIntent);
            assertEquals(itemExpected.itemDrawableResourceId, itemReal.itemDrawableResourceId);
            assertEquals(itemExpected.itemTitle, itemReal.itemTitle);
            assertEquals(itemExpected.itemSummary, itemReal.itemSummary);
        }
    }

    @MediumTest
    public void testCustomTileExpandedRemoteStyleUnravelFromParcel() {
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.remote_view);

        CustomTile.RemoteExpandedStyle remoteExpandedStyle =
                new CustomTile.RemoteExpandedStyle();
        remoteExpandedStyle.setRemoteViews(contentView);

        CustomTile expectedCustomTile = new CustomTile.Builder(mContext)
                .setExpandedStyle(remoteExpandedStyle)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        CustomTile fromParcel = CustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel.expandedStyle);
        assertEquals(expectedCustomTile.expandedStyle.getStyle(),
                fromParcel.expandedStyle.getStyle());
        assertNotNull(fromParcel.expandedStyle.getContentViews());
    }
}
