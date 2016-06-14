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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.app.IProfileManager;
import cyanogenmod.providers.CMSettings;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class ProfileManagerTest extends AndroidTestCase {
    private static final String TAG = ProfileManagerTest.class.getSimpleName();
    private static final int COUNTDOWN = 1;
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

    private void ensureProfilesEnabled() {
        final String enabledValue = "1";
        assertTrue(CMSettings.System.putString(getContext().getContentResolver(),
                CMSettings.System.SYSTEM_PROFILES_ENABLED, enabledValue));
    }

    @SmallTest
    public void testGetActiveProfile() {
        ensureProfilesEnabled();
        final CountDownLatch signal = new CountDownLatch(COUNTDOWN);
        final Profile expectedActiveProfile = new Profile("TEST ACTIVE PROFILE");
        mProfileManager.addProfile(expectedActiveProfile);

        BroadcastReceiver intentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                signal.countDown();
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ProfileManager.INTENT_ACTION_PROFILE_SELECTED);
        intentFilter.addAction(ProfileManager.INTENT_ACTION_PROFILE_UPDATED);

        mContext.registerReceiver(intentReceiver, intentFilter);

        mProfileManager.setActiveProfile(expectedActiveProfile.getName());

        // Lock
        try {
            signal.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

        assertEquals(expectedActiveProfile.getName(),
                mProfileManager.getActiveProfile().getName());
        mProfileManager.resetAll();

        mContext.unregisterReceiver(intentReceiver);
    }

    @SmallTest
    public void testGetProfileNames() {
        ensureProfilesEnabled();
        String[] expectedProfileNames = new String[5];
        // These defaults are pulled from the default xml in the cm platform resource package
        Profile expectedProfile1 = mProfileManager.getProfile(
                UUID.fromString("6a181391-12ef-4f43-a701-32b11ed69449"));
        Profile expectedProfile2 = mProfileManager.getProfile(
                UUID.fromString("0230226d-0d05-494a-a9bd-d222a1117655"));
        Profile expectedProfile3 = mProfileManager.getProfile(
                UUID.fromString("e4e77d03-82ce-4417-9257-7d6c9ffb8fd1"));

        // Add extras
        Profile expectedProfile4 = new Profile("PROFILE 1");
        Profile expectedProfile5 = new Profile("PROFILE 2");

        expectedProfileNames[0] = expectedProfile1.getName();
        expectedProfileNames[1] = expectedProfile2.getName();
        expectedProfileNames[2] = expectedProfile3.getName();
        expectedProfileNames[3] = expectedProfile4.getName();
        expectedProfileNames[4] = expectedProfile5.getName();

        mProfileManager.addProfile(expectedProfile1);
        mProfileManager.addProfile(expectedProfile2);

        String[] actualProfileNames = mProfileManager.getProfileNames();
        for (int i = 0; i < actualProfileNames.length; i++) {
            assertEquals(expectedProfileNames[i], actualProfileNames[i]);
        }
        mProfileManager.resetAll();
    }

    @SmallTest
    public void testGetProfiles() {
        ensureProfilesEnabled();
        Profile[] expectedProfiles = new Profile[5];
        // These defaults are pulled from the default xml in the cm platform resource package
        Profile expectedProfile1 = mProfileManager.getProfile(
                UUID.fromString("6a181391-12ef-4f43-a701-32b11ed69449"));
        Profile expectedProfile2 = mProfileManager.getProfile(
                UUID.fromString("0230226d-0d05-494a-a9bd-d222a1117655"));
        Profile expectedProfile3 = mProfileManager.getProfile(
                UUID.fromString("e4e77d03-82ce-4417-9257-7d6c9ffb8fd1"));

        // Add extras
        Profile expectedProfile4 = new Profile("PROFILE 1");
        Profile expectedProfile5 = new Profile("PROFILE 2");

        expectedProfiles[0] = expectedProfile1;
        expectedProfiles[1] = expectedProfile2;
        expectedProfiles[2] = expectedProfile3;
        expectedProfiles[3] = expectedProfile4;
        expectedProfiles[4] = expectedProfile5;

        // The actual results come back in alphabetical order, :/
        Arrays.sort(expectedProfiles);

        mProfileManager.addProfile(expectedProfile4);
        mProfileManager.addProfile(expectedProfile5);

        Profile[] actualProfiles = mProfileManager.getProfiles();
        for (int i = 0; i < actualProfiles.length; i++) {
            assertEquals(expectedProfiles[i].getName(), actualProfiles[i].getName());
        }
        mProfileManager.resetAll();
    }

    @SmallTest
    public void testProfileExists() {
        ensureProfilesEnabled();
        assertTrue(mProfileManager.profileExists(
                UUID.fromString("6a181391-12ef-4f43-a701-32b11ed69449")));
        assertTrue(mProfileManager.profileExists(
                UUID.fromString("0230226d-0d05-494a-a9bd-d222a1117655")));
        assertTrue(mProfileManager.profileExists(
                UUID.fromString("e4e77d03-82ce-4417-9257-7d6c9ffb8fd1")));
        String expectedProfileName = "PROFILE 1";
        Profile expectedProfile = new Profile(expectedProfileName);
        mProfileManager.addProfile(expectedProfile);
        assertTrue(mProfileManager.profileExists(expectedProfileName));
        mProfileManager.resetAll();
    }

    @SmallTest
    public void testUpdateProfile() {
        ensureProfilesEnabled();
        String originalProfileName = "PROFILE 1";
        String expectedProfileName = "PROFILE 2";
        Profile expectedProfile = new Profile(originalProfileName);
        mProfileManager.addProfile(expectedProfile);
        expectedProfile.setName(expectedProfileName);
        mProfileManager.updateProfile(expectedProfile);
        assertNotSame(originalProfileName, expectedProfile.getName());
        assertEquals(expectedProfileName, expectedProfile.getName());
        mProfileManager.resetAll();
    }
}
