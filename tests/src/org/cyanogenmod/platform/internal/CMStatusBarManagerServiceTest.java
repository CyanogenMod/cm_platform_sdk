/*
 * Copyright (c) 2016 The CyanogenMod Project
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

package org.cyanogenmod.platform.internal;

import android.os.RemoteException;
import android.os.UserHandle;
import android.test.AndroidTestCase;

import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;
import cyanogenmod.app.ICMStatusBarManager;

/**
 * Created by adnan on 1/5/16.
 */
public class CMStatusBarManagerServiceTest extends AndroidTestCase {
    private ICMStatusBarManager mInternalServiceInterface;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInternalServiceInterface = CMStatusBarManager.getInstance(mContext).getService();
    }

    @SmallTest
    public void testAddTileById() {
        //Create placeholder tile
        int resourceInt = org.cyanogenmod.tests.R.drawable.ic_launcher;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setIcon(resourceInt)
                .build();

        int[] idOut = new int[1];
        // Test adding a tile directly from the service interface.
        try {
           mInternalServiceInterface
                    .createCustomTileWithTag(mContext.getPackageName(), mContext.getPackageName(),
                            null, 1337, customTile, idOut, UserHandle.myUserId());
        } catch (RemoteException e) {
            fail("Remote exception when adding a tile by id");
        }
    }

    @SmallTest
    public void testAddAndRemoveTileById() {
        //Create placeholder tile
        int resourceInt = org.cyanogenmod.tests.R.drawable.ic_launcher;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setIcon(resourceInt)
                .build();

        int[] idOut = new int[1];
        try {
            // Test adding a tile directly from the service interface.
            mInternalServiceInterface
                    .createCustomTileWithTag(mContext.getPackageName(), mContext.getPackageName(),
                            null, 1337, customTile, idOut, UserHandle.myUserId());
        } catch (RemoteException e) {
            fail("Remote exception when adding a tile by id");
        }

        try {
            mInternalServiceInterface
                    .removeCustomTileWithTag(mContext.getPackageName(), null,
                            1337, UserHandle.myUserId());
        } catch (RemoteException e) {
            fail("Remote exception when removing a tile by id");
        }
    }

    @SmallTest
    public void testAddTileByTag() {
        //Create placeholder tile
        int resourceInt = org.cyanogenmod.tests.R.drawable.ic_launcher;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setIcon(resourceInt)
                .build();

        int[] idOut = new int[1];
        try {
            // Test adding a tile directly from the service interface.
            mInternalServiceInterface
                    .createCustomTileWithTag(mContext.getPackageName(), mContext.getPackageName()
                            , "fake-tag", 0, customTile, idOut, UserHandle.myUserId());
        }  catch (RemoteException e) {
            fail("Remote exception when adding a tile by tag");
        }
    }

    @SmallTest
    public void testAddAndRemoveTileByTag() {
        //Create placeholder tile
        int resourceInt = org.cyanogenmod.tests.R.drawable.ic_launcher;
        CustomTile customTile = new CustomTile.Builder(mContext)
                .setIcon(resourceInt)
                .build();

        int[] idOut = new int[1];
        try {
            // Test adding a tile directly from the service interface.
            mInternalServiceInterface
                    .createCustomTileWithTag(mContext.getPackageName(), mContext.getPackageName(),
                            "fake-tag", 0, customTile, idOut, UserHandle.myUserId());
        }  catch (RemoteException e) {
            fail("Remote exception when adding a tile by tag");
        }

        try {
            // Test removing tile directly from the service interface
            mInternalServiceInterface
                    .removeCustomTileWithTag(mContext.getPackageName(), mContext.getPackageName(),
                            0, UserHandle.myUserId());
        } catch (RemoteException e) {
            fail("Remote exception when removing a tile by tag");
        }
    }

    @SmallTest
    public void testFakePackageThrowsSecurityExceptionOnRemove() {
        try {
            mInternalServiceInterface
                    .removeCustomTileWithTag("fack-package-throw", "fake-package-throw",
                            0, UserHandle.myUserId());
            fail("Security Exception not thrown on fake package removing a tile");
        } catch (RemoteException e) {
            fail("Remote exception when removing a tile by tag");
        } catch (SecurityException e) {
            //Expected
        }
    }
}
