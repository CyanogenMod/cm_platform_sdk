/**
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.cyanogenmod.tests.livelockscreen.unit;

import android.content.ComponentName;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.app.LiveLockScreenInfo;

public class LiveLockScreenInfoTest extends AndroidTestCase {

    @SmallTest
    public void testConstructor() {
        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder().build();
        assertNotNull(llsInfo);
    }

    @SmallTest
    public void testNonNullComponentFromParcel() {
        ComponentName cn = new ComponentName("package", "class");

        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder()
                .setComponent(cn)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        llsInfo.writeToParcel(parcel, 0);

        // Set data position of parcel to 0
        parcel.setDataPosition(0);

        // Verify un-parceled LiveLockScreenInfo
        LiveLockScreenInfo llsFromParcel = LiveLockScreenInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(llsFromParcel);
        assertEquals(cn, llsFromParcel.component);
    }

    @SmallTest
    public void testNullComponentFromParcel() {
        ComponentName cn = null;

        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder().build();
        llsInfo.component = cn;

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        llsInfo.writeToParcel(parcel, 0);

        // Set data position of parcel to 0
        parcel.setDataPosition(0);

        // Verify un-parceled LiveLockScreenInfo
        LiveLockScreenInfo llsFromParcel = LiveLockScreenInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(llsFromParcel);
        assertEquals(cn, llsFromParcel.component);
    }

    @SmallTest
    public void testPriorityFromParcel() {
        int priority = LiveLockScreenInfo.PRIORITY_HIGH;

        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder()
                .setPriority(priority)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        llsInfo.writeToParcel(parcel, 0);

        // Set data position of parcel to 0
        parcel.setDataPosition(0);

        // Verify un-parceled LiveLockScreenInfo
        LiveLockScreenInfo llsFromParcel = LiveLockScreenInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(llsFromParcel);
        assertEquals(priority, llsFromParcel.priority);
    }
}
