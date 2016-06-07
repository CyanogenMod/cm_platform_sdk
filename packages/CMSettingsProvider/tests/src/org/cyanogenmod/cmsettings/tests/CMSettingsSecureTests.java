/**
 * Copyright (c) 2016, The CyanogenMod Project
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

package org.cyanogenmod.cmsettings.tests;

import android.content.ContentResolver;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.providers.CMSettings;

public class CMSettingsSecureTests extends AndroidTestCase {
    private ContentResolver mContentResolver;

    private static final String UNREALISTIC_SETTING = "_______UNREAL_______";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = mContext.getContentResolver();
    }

    @SmallTest
    public void testFloat() {
        final float expectedFloatValue = 1.0f;
        CMSettings.Secure.putFloat(mContentResolver,
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER, expectedFloatValue);

        try {
            float actualValue = CMSettings.Secure.getFloat(mContentResolver,
                    CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER);
            assertEquals(expectedFloatValue, actualValue);
        } catch (CMSettings.CMSettingNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @SmallTest
    public void testFloatWithDefault() {
        final float expectedDefaultFloatValue = 1.5f;
        float actualValue = CMSettings.Secure.getFloat(mContentResolver,
                    UNREALISTIC_SETTING, expectedDefaultFloatValue);
        assertEquals(expectedDefaultFloatValue, actualValue);
    }

    @SmallTest
    public void testInt() {
        final int expectedIntValue = 2;
        CMSettings.Secure.putInt(mContentResolver,
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER, expectedIntValue);

        try {
            int actualValue = CMSettings.Secure.getInt(mContentResolver,
                    CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER);
            assertEquals(expectedIntValue, actualValue);
        } catch (CMSettings.CMSettingNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @SmallTest
    public void testIntWithDefault() {
        final int expectedDefaultIntValue = 11;
        int actualValue = CMSettings.Secure.getInt(mContentResolver,
                UNREALISTIC_SETTING, expectedDefaultIntValue);
        assertEquals(expectedDefaultIntValue, actualValue);
    }

    @SmallTest
    public void testLong() {
        final long expectedLongValue = 3l;
        CMSettings.Secure.putLong(mContentResolver,
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER, expectedLongValue);

        try {
            long actualValue = CMSettings.Secure.getLong(mContentResolver,
                    CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER);
            assertEquals(expectedLongValue, actualValue);
        } catch (CMSettings.CMSettingNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @SmallTest
    public void testLongWithDefault() {
        final long expectedDefaultLongValue = 17l;
        long actualValue = CMSettings.Secure.getLong(mContentResolver,
                UNREALISTIC_SETTING, expectedDefaultLongValue);
        assertEquals(expectedDefaultLongValue, actualValue);
    }

    @SmallTest
    public void testString() {
        final String expectedStringValue = "4";
        CMSettings.Secure.putString(mContentResolver,
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER, expectedStringValue);

        String actualValue = CMSettings.Secure.getString(mContentResolver,
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER);
        assertEquals(expectedStringValue, actualValue);
    }

    @SmallTest
    public void testGetUri() {
        final Uri expectedUri = Uri.withAppendedPath(CMSettings.Secure.CONTENT_URI,
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER);

        final Uri actualUri = CMSettings.Secure.getUriFor(
                CMSettings.Secure.__MAGICAL_TEST_PASSING_ENABLER);

        assertEquals(expectedUri, actualUri);
    }
}
