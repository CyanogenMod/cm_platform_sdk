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
import android.os.Binder;
import android.os.Parcel;
import android.os.UserHandle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CustomTile;
import cyanogenmod.app.StatusBarPanelCustomTile;
import org.cyanogenmod.tests.R;
import org.cyanogenmod.tests.customtiles.DummySettings;

import java.util.ArrayList;

/**
 * Created by adnan on 7/15/15.
 */
public class StatusBarPanelCustomTileTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Only run this if we support cm status bar service
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.STATUSBAR));
    }

    @SmallTest
    public void testUnravelFromParcel() {
        String expectedPackage = "expectedPackage";
        String expectedOpresPackage = "resPackage";
        String expectedOpPackage = "opPackage";
        int expectedId = 1337;
        String expectedTag = "TAG";
        int expectedUid = mContext.getUserId();
        int expectedPid = Binder.getCallingPid();
        CustomTile expectedCustomTile = createSampleCustomTile();
        UserHandle expectedUserHandle = new UserHandle(mContext.getUserId());
        long expectedPostTime = System.currentTimeMillis();

        // public StatusBarPanelCustomTile(String pkg, String resPkg, String opPkg,
        // int id, String tag, int uid, int initialPid, CustomTile customTile, UserHandle user,
        // long postTime)
        StatusBarPanelCustomTile statusBarPanelCustomTile =
                new StatusBarPanelCustomTile(expectedPackage, expectedOpresPackage,
                        expectedOpPackage, expectedId, expectedTag, expectedUid, expectedPid,
                        expectedCustomTile, expectedUserHandle, expectedPostTime);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        statusBarPanelCustomTile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        StatusBarPanelCustomTile fromParcel
                = StatusBarPanelCustomTile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedPackage, fromParcel.getPackage());
        assertEquals(expectedPostTime, fromParcel.getPostTime());
        assertEquals(expectedOpresPackage, fromParcel.getResPkg());
        assertEquals(expectedOpPackage, fromParcel.getOpPkg());
        assertEquals(expectedId, fromParcel.getId());
        assertNotNull(fromParcel.getKey());
        assertEquals(expectedTag, fromParcel.getTag());
        assertEquals(expectedUid, fromParcel.getUid());
        assertEquals(expectedPid, fromParcel.getInitialPid());
        //CustomTile validation is excessive
        assertEquals(expectedUserHandle.getIdentifier(), fromParcel.getUser().getIdentifier());
        assertEquals(expectedUserHandle.getIdentifier(), fromParcel.getUserId());
    }

    @SmallTest
    public void testDefaultConstructor() {
        String expectedPackage = "expectedPackage";
        String expectedOpresPackage = "resPackage";
        String expectedOpPackage = "opPackage";
        int expectedId = 1337;
        String expectedTag = "TAG";
        int expectedUid = mContext.getUserId();
        int expectedPid = Binder.getCallingPid();
        CustomTile expectedCustomTile = createSampleCustomTile();
        UserHandle expectedUserHandle = new UserHandle(mContext.getUserId());

        // public StatusBarPanelCustomTile(String pkg, String resPkg, String opPkg,
        // int id, String tag, int uid, int initialPid, CustomTile customTile, UserHandle user)
        StatusBarPanelCustomTile statusBarPanelCustomTile =
                new StatusBarPanelCustomTile(expectedPackage, expectedOpresPackage,
                        expectedOpPackage, expectedId, expectedTag, expectedUid, expectedPid,
                        expectedCustomTile, expectedUserHandle);

        assertNotNull(statusBarPanelCustomTile);
        assertEquals(expectedPackage, statusBarPanelCustomTile.getPackage());
        assertEquals(expectedOpresPackage, statusBarPanelCustomTile.getResPkg());
        assertEquals(expectedOpPackage, statusBarPanelCustomTile.getOpPkg());
        assertEquals(expectedId, statusBarPanelCustomTile.getId());
        assertNotNull(statusBarPanelCustomTile.getKey());
        assertEquals(expectedTag, statusBarPanelCustomTile.getTag());
        assertEquals(expectedUid, statusBarPanelCustomTile.getUid());
        assertEquals(expectedPid, statusBarPanelCustomTile.getInitialPid());
        //CustomTile validation is excessive
        assertEquals(expectedUserHandle.getIdentifier(),
                statusBarPanelCustomTile.getUser().getIdentifier());
        assertEquals(expectedUserHandle.getIdentifier(),
                statusBarPanelCustomTile.getUserId());
    }

    private CustomTile createSampleCustomTile() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        return new CustomTile.Builder(mContext)
                .setOnClickIntent(pendingIntent)
                .build();
    }
}
