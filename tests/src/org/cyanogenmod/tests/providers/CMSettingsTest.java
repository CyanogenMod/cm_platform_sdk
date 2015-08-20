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

package org.cyanogenmod.tests.providers;

import android.content.ContentResolver;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import cyanogenmod.providers.CMSettings;

public class CMSettingsTest extends AndroidTestCase{
    private ContentResolver mContentResolver;

    @Override
    public void setUp() {
        mContentResolver = getContext().getContentResolver();
    }

    @MediumTest
    public void testPutAndGetSystemString() {
        final String key = "key";

        // put
        final String expectedValue = "systemTestValue1";
        boolean isPutSuccessful = CMSettings.System.putString(mContentResolver, key, expectedValue);
        assertTrue(isPutSuccessful);

        // get
        String actualValue = CMSettings.System.getString(mContentResolver, key);
        assertEquals(expectedValue, actualValue);

        // replace
        final String expectedReplaceValue = "systemTestValue2";
        isPutSuccessful = CMSettings.System.putString(mContentResolver, key, expectedReplaceValue);
        assertTrue(isPutSuccessful);

        // get
        actualValue = CMSettings.System.getString(mContentResolver, key);
        assertEquals(expectedReplaceValue, actualValue);

        // delete to clean up
        int rowsAffected = mContentResolver.delete(CMSettings.System.CONTENT_URI, Settings.NameValueTable.NAME + " = ?",
                new String[]{ key });
        assertEquals(1, rowsAffected);
    }

    @MediumTest
    public void testPutAndGetSecureString() {
        final String key = "key";

        // put
        final String expectedValue = "secureTestValue1";
        boolean isPutSuccessful = CMSettings.Secure.putString(mContentResolver, key, expectedValue);
        assertTrue(isPutSuccessful);

        // get
        String actualValue = CMSettings.Secure.getString(mContentResolver, key);
        assertEquals(expectedValue, actualValue);

        // replace
        final String expectedReplaceValue = "secureTestValue2";
        isPutSuccessful = CMSettings.Secure.putString(mContentResolver, key, expectedReplaceValue);
        assertTrue(isPutSuccessful);

        // get
        actualValue = CMSettings.Secure.getString(mContentResolver, key);
        assertEquals(expectedReplaceValue, actualValue);

        // delete to clean up
        int rowsAffected = mContentResolver.delete(CMSettings.Secure.CONTENT_URI, Settings.NameValueTable.NAME + " = ?",
                new String[]{ key });
        assertEquals(1, rowsAffected);
    }

    @MediumTest
    public void testPutAndGetGlobalString() {
        final String key = "key";

        // put
        final String expectedValue = "globalTestValue1";
        boolean isPutSuccessful = CMSettings.Global.putString(mContentResolver, key, expectedValue);
        assertTrue(isPutSuccessful);

        // get
        String actualValue = CMSettings.Global.getString(mContentResolver, key);
        assertEquals(expectedValue, actualValue);

        // replace
        final String expectedReplaceValue = "globalTestValue2";
        isPutSuccessful = CMSettings.Global.putString(mContentResolver, key, expectedReplaceValue);
        assertTrue(isPutSuccessful);

        // get
        actualValue = CMSettings.Global.getString(mContentResolver, key);
        assertEquals(expectedReplaceValue, actualValue);

        // delete to clean up
        int rowsAffected = mContentResolver.delete(CMSettings.Global.CONTENT_URI, Settings.NameValueTable.NAME + " = ?",
                new String[]{ key });
        assertEquals(1, rowsAffected);
    }

    // TODO Add tests for other users
}
