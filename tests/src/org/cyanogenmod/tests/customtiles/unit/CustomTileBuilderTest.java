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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CustomTile;
import org.cyanogenmod.tests.R;
import org.cyanogenmod.tests.customtiles.CMStatusBarTest;
import org.cyanogenmod.tests.customtiles.DummySettings;

import java.util.ArrayList;

/**
 * Created by adnan on 7/14/15.
 */
public class CustomTileBuilderTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Only run this if we support cm status bar service
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.STATUSBAR));
    }

    @SmallTest
    public void testConstructor() {
        new CustomTile.Builder(mContext);
    }

    @SmallTest
    public void testCustomTileBuilderOnClickIntent() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setOnClickIntent(pendingIntent)
                .build();
        assertNotNull(customTile.onClick);
        assertEquals(pendingIntent, customTile.onClick);
    }

    @SmallTest
    public void testCustomTileBuilderOnLongClickIntent() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setOnLongClickIntent(pendingIntent)
                .build();
        assertNotNull(customTile.onLongClick);
        assertEquals(pendingIntent, customTile.onLongClick);
    }

    @SmallTest
    public void testCustomTileBuilderOnSettingsClickIntent() {
        Intent intent = new Intent(mContext, DummySettings.class);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setOnSettingsClickIntent(intent)
                .build();
        assertNotNull(customTile.onSettingsClick);
        assertEquals(intent, customTile.onSettingsClick);
    }

    @SmallTest
    public void testCustomTileBuilderDeleteIntent() {
        Intent intent = new Intent(mContext, DummySettings.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setDeleteIntent(pendingIntent)
                .build();
        assertNotNull(customTile.deleteIntent);
        assertEquals(pendingIntent, customTile.deleteIntent);
    }

    @SmallTest
    public void testCustomTileBuilderOnClickUri() {
        //Calling Mike Jones, WHO!? MIKE JONES.
        Uri uri = Uri.parse("2813308004");
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setOnClickUri(uri)
                .build();
        assertNotNull(uri);
        assertEquals(uri, customTile.onClickUri);
    }

    @SmallTest
    public void testCustomTileBuilderLabel() {
        String message = "Test label";
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setLabel(message).build();
        assertNotNull(customTile);
        assertEquals(message, customTile.label);
    }

    @SmallTest
    public void testCustomTileBuilderLabelAsRes() {
        String message = mContext.getString(R.string.app_name);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setLabel(R.string.app_name).build();
        assertNotNull(customTile);
        assertEquals(message, customTile.label);
    }

    @SmallTest
    public void testCustomTileBuilderContentDescription() {
        String message = "Test content description";
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setContentDescription(message)
                .build();
        assertNotNull(customTile);
        assertEquals(message, customTile.contentDescription);
    }

    @SmallTest
    public void testCustomTileBuilderContentDescriptionAsRes() {
        String message = mContext.getString(R.string.app_name);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setContentDescription(R.string.app_name)
                .build();
        assertNotNull(customTile);
        assertEquals(message, customTile.contentDescription);
    }

    @SmallTest
    public void testCustomTileBuilderIconSet() {
        int resourceInt = R.drawable.ic_launcher;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setIcon(resourceInt)
                .build();
        assertNotNull(customTile.icon);
        assertNotSame(customTile.icon, 0);
        assertEquals(resourceInt, customTile.icon);
    }

    @SmallTest
    public void testCustomTileBuilderRemoteIconSet() {
        int resourceInt = R.drawable.ic_whatshot_white_24dp;
        Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(),
                resourceInt);
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setIcon(bitmap)
                .build();
        assertNotNull(customTile.remoteIcon);
        assertEquals(bitmap, customTile.remoteIcon);
    }

    @SmallTest
    public void testCustomTileBuilderCollapsePanelSet() {
        boolean collapsePanel = true;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .shouldCollapsePanel(collapsePanel)
                .build();
        assertEquals(collapsePanel, customTile.collapsePanel);
    }

    @SmallTest
    public void testCustomTileBuilderSensitiveDataSet() {
        boolean sensitiveData = true;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .hasSensitiveData(sensitiveData)
                .build();
        assertEquals(sensitiveData, customTile.sensitiveData);
    }

    @MediumTest
    public void testCustomTileBuilderExpandedListStyleSet() {
        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, CMStatusBarTest.class)
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
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setExpandedStyle(listExpandedStyle)
                .build();

        assertNotNull(customTile.expandedStyle);
        assertEquals(listExpandedStyle, customTile.expandedStyle);
        assertNotNull(customTile.expandedStyle.getExpandedItems());
        for (int j = 0; j < 100; j++) {
            CustomTile.ExpandedItem itemExpected = expandedListItems.get(j);
            CustomTile.ExpandedItem itemReal = customTile.expandedStyle.getExpandedItems()[j];
            assertEquals(itemExpected.onClickPendingIntent, itemReal.onClickPendingIntent);
            assertEquals(itemExpected.itemDrawableResourceId, itemReal.itemDrawableResourceId);
            assertEquals(itemExpected.itemTitle, itemReal.itemTitle);
            assertEquals(itemExpected.itemSummary, itemReal.itemSummary);
        }
    }

    @MediumTest
    public void testCustomTileBuilderExpandedGridStyleSet() {
        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, CMStatusBarTest.class)
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
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setExpandedStyle(gridExpandedStyle)
                .build();

        assertNotNull(customTile.expandedStyle);
        assertEquals(gridExpandedStyle, customTile.expandedStyle);
        assertNotNull(customTile.expandedStyle.getExpandedItems());
        for (int j = 0; j < 100; j++) {
            CustomTile.ExpandedItem itemExpected = expandedGridItems.get(j);
            CustomTile.ExpandedItem itemReal = customTile.expandedStyle.getExpandedItems()[j];
            assertEquals(itemExpected.onClickPendingIntent, itemReal.onClickPendingIntent);
            assertEquals(itemExpected.itemDrawableResourceId, itemReal.itemDrawableResourceId);
            assertEquals(itemExpected.itemTitle, itemReal.itemTitle);
            assertEquals(itemExpected.itemSummary, itemReal.itemSummary);
        }
    }
}
