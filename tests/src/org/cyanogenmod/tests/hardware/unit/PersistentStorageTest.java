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
    public void testUdidFailure() {
        String key = "udid";
        String value = "542bc67e510e82bd6d44e4f7015d7970";
        assertTrue(mHardwareManager.writePersistentString(key, value));
    }

    @SmallTest
    public void testPersistentStringInvalidInput() {
        String testKey = UUID.randomUUID().toString();
        String testString = "IM IN UR STORAGE";
        String testKeyTooLong = getStringOfLength(65);
        String testStringTooLong = getStringOfLength(4097);

        assertFalse(mHardwareManager.writePersistentString(null, testString));
        assertFalse(mHardwareManager.writePersistentString("", testString));
        assertFalse(mHardwareManager.writePersistentString(testKeyTooLong, testString));
        assertFalse(mHardwareManager.writePersistentString(testKey, testStringTooLong));
        assertFalse(mHardwareManager.writePersistentString(testKey, ""));
        assertNull(mHardwareManager.readPersistentString(testKey));
        assertNull(mHardwareManager.readPersistentString(testKeyTooLong));
    }

    @SmallTest
    public void testPersistentIntInvalidInput() {
        String testKey = UUID.randomUUID().toString();
        String testString = "IM IN UR STORAGE";
        String testKeyTooLong = getStringOfLength(65);

        assertFalse(mHardwareManager.writePersistentInt(null, 49152));
        assertFalse(mHardwareManager.writePersistentInt("", 49152));
        assertFalse(mHardwareManager.writePersistentInt(testKeyTooLong, 49152));
        assertEquals(0, mHardwareManager.readPersistentInt(testKey));
        assertEquals(0, mHardwareManager.readPersistentInt(testKeyTooLong));
    }

    @SmallTest
    public void testPersistentBytesInvalidInput() {
        String testKey = UUID.randomUUID().toString();
        byte[] testArray = new byte[1024];
        byte[] testArrayTooLong = new byte[4097];
        String testKeyTooLong = getStringOfLength(65);

        assertFalse(mHardwareManager.writePersistentBytes(null, testArray));
        assertFalse(mHardwareManager.writePersistentBytes("", testArray));
        assertFalse(mHardwareManager.writePersistentBytes(testKeyTooLong, testArray));
        assertFalse(mHardwareManager.writePersistentBytes(testKey, testArrayTooLong));
        assertFalse(mHardwareManager.writePersistentBytes(testKey, new byte[0]));
        assertNull(mHardwareManager.readPersistentBytes(testKey));
        assertNull(mHardwareManager.readPersistentBytes(testKeyTooLong));
    }

    @SmallTest
    public void testPersistentString() {
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

        // erase through write null
        assertTrue(mHardwareManager.writePersistentString(testKey, testString + " AGAIN"));
        assertEquals(testString + " AGAIN", mHardwareManager.readPersistentString(testKey));
        assertTrue(mHardwareManager.writePersistentString(testKey, null));
        assertNull(mHardwareManager.readPersistentString(testKey));
    }

    @SmallTest
    public void testPersistentInteger() {
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
    }

    @SmallTest
    public void testPersistentBytes() {
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

        // erase through write null
        assertTrue(mHardwareManager.writePersistentBytes(testKey, testArray));
        assertTrue(Arrays.equals(testArray, mHardwareManager.readPersistentBytes(testKey)));
        assertTrue(mHardwareManager.writePersistentBytes(testKey, null));
        assertNull(mHardwareManager.readPersistentBytes(testKey));
    }

    private String getStringOfLength(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'z');
        return new String(chars);
    }
}
