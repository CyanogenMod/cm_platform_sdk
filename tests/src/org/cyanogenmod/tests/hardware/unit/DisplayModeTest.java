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

package org.cyanogenmod.tests.hardware.unit;

import android.os.Parcel;
import android.test.AndroidTestCase;

import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.hardware.DisplayMode;

/**
 * Created by adnan on 9/1/15.
 */
public class DisplayModeTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @SmallTest
    public void testDisplayModeUnravelFromParcel() {
        int expectedId = 1337;
        String expectedName = "test";
        DisplayMode expectedDisplayMode = new DisplayMode(expectedId, expectedName);
        // Write to parcel
        Parcel parcel = Parcel.obtain();
        expectedDisplayMode.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        DisplayMode fromParcel = DisplayMode.CREATOR.createFromParcel(parcel);

        assertNotNull(expectedDisplayMode.id);
        assertNotNull(expectedDisplayMode.name);

        assertEquals(expectedDisplayMode.id, fromParcel.id );
        assertEquals(expectedDisplayMode.name,
                fromParcel.name);
    }
}
