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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.hardware.CMHardwareManager;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class PersistentStorageTest extends AndroidTestCase {

    private CMHardwareManager mHardwareManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHardwareManager = CMHardwareManager.getInstance(mContext);
    }

    @SmallTest
    public boolean testPersistentString() {
        assertTrue(mHardwareManager.isSupported(CMHardwareManager.FEATURE_PERSISTENT_STORAGE));

        String testKey = UUID.randomUUID().toString();
        String testString = "IM IN UR STORAGE";

        // write + read
        assertTrue(mHardwareManager.writePersistentString(testKey, testString));
        assertEquals(testString, mHardwareManager.readPersistentString(testKey));

        // rewrite + read
        assertTrue(mHardwareManager.writePersistentString(testKey, testString + " AGAIN"));
        assertEquals(testString + " AGAIN", mHardwareManager.readPersistentString(testKey));

        // erase + read
        assertTrue(mHardwareManager.deletePersistentObject(testKey));
        assertNull(mHardwareManager.readPersistentString(testKey));

        return true;
    }

    @SmallTest
    public boolean testPersistentInteger() {
        assertTrue(mHardwareManager.isSupported(CMHardwareManager.FEATURE_PERSISTENT_STORAGE));

        String testKey = UUID.randomUUID().toString();
        int testInt = 49152;

        // write + read
        assertTrue(mHardwareManager.writePersistentInt(testKey, testInt));
        assertEquals(testInt, mHardwareManager.readPersistentInt(testKey));

        // rewrite + read
        assertTrue(mHardwareManager.writePersistentInt(testKey, testInt * 2));
        assertEquals(testInt * 2, mHardwareManager.readPersistentInt(testKey));

        // erase + read
        assertTrue(mHardwareManager.deletePersistentObject(testKey));
        assertEquals(0, mHardwareManager.readPersistentInt(testKey));

        return true;
    }

    @SmallTest
    public boolean testPersistentBytes() {
        assertTrue(mHardwareManager.isSupported(CMHardwareManager.FEATURE_PERSISTENT_STORAGE));

        String testKey = UUID.randomUUID().toString();
        byte[] testArray = new byte[1024];
        byte[] testArray2 = new byte[4096];

        new Random().nextBytes(testArray);
        new Random().nextBytes(testArray2);

        // write + read
        assertTrue(mHardwareManager.writePersistentBytes(testKey, testArray));
        assertTrue(Arrays.equals(testArray, mHardwareManager.readPersistentBytes(testKey)));

        // write + read
        assertTrue(mHardwareManager.writePersistentBytes(testKey, testArray2));
        assertTrue(Arrays.equals(testArray2, mHardwareManager.readPersistentBytes(testKey)));

        // erase + read
        assertTrue(mHardwareManager.deletePersistentObject(testKey));
        assertNull(mHardwareManager.readPersistentBytes(testKey));

        return true;
    }
}
