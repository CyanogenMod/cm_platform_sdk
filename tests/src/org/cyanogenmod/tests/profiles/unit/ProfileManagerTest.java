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

package org.cyanogenmod.tests.profiles.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.app.IProfileManager;
import cyanogenmod.providers.CMSettings;

/**
 * Created by adnan on 7/15/15.
 */
public class ProfileManagerTest extends AndroidTestCase {
    private ProfileManager mProfileManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProfileManager = ProfileManager.getInstance(mContext);
        // Only run this if we support profiles service
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.PROFILES));
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mProfileManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        IProfileManager iProfileManager = mProfileManager.getService();
        assertNotNull(iProfileManager);
    }

    @SmallTest
    public void testManagerProfileIsEnabled() {
        // first enable profiles
        final String enabledValue = "1";
        assertTrue(CMSettings.System.putString(getContext().getContentResolver(),
                CMSettings.System.SYSTEM_PROFILES_ENABLED, enabledValue));

        // check that we successfully enabled them via system setting
        assertEquals(enabledValue, CMSettings.System.getString(getContext().getContentResolver(),
                CMSettings.System.SYSTEM_PROFILES_ENABLED));

        // check that profile manger returns true
        assertTrue(mProfileManager.isProfilesEnabled());

        // now disable the setting
        final String disabledValue = "0";
        assertTrue(CMSettings.System.putString(getContext().getContentResolver(),
                CMSettings.System.SYSTEM_PROFILES_ENABLED, disabledValue));

        // check that we successfully disable them via system setting
        assertEquals(disabledValue, CMSettings.System.getString(getContext().getContentResolver(),
                CMSettings.System.SYSTEM_PROFILES_ENABLED));

        assertFalse(mProfileManager.isProfilesEnabled());
    }
}
