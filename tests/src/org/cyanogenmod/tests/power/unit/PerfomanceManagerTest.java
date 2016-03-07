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
package org.cyanogenmod.tests.power.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManager;

/**
 * Created by adnan on 3/4/16.
 */
public class PerfomanceManagerTest extends AndroidTestCase {
    private static final String TAG = PerfomanceManagerTest.class.getSimpleName();
    private PerformanceManager mCMPerformanceManager;
    private int mSavedPerfProfile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCMPerformanceManager = PerformanceManager.getInstance(mContext);
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mCMPerformanceManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        IPerformanceManager icmStatusBarManager = mCMPerformanceManager.getService();
        assertNotNull(icmStatusBarManager);
    }

    @SmallTest
    public void testGetNumberOfPerformanceProfiles() {
        assertTrue(mCMPerformanceManager.getNumberOfProfiles() > 0);
    }

    @SmallTest
    public void testGetPowerProfile() {
        int notExpectedProfile = -1;

        mSavedPerfProfile = mCMPerformanceManager.getPowerProfile();

        assertNotSame(notExpectedProfile, mSavedPerfProfile);
    }

    @SmallTest
    public void testSetAndGetPowerProfile() {
        int[] expectedStates = new int[] { PerformanceManager.PROFILE_POWER_SAVE,
                PerformanceManager.PROFILE_BALANCED,
                PerformanceManager.PROFILE_HIGH_PERFORMANCE};

        // set the state
        for (int profile : expectedStates) {
            boolean success = mCMPerformanceManager.setPowerProfile(profile);

            // verify that it was set correctly.
            if (success) {
                assertEquals(profile, mCMPerformanceManager.getPowerProfile());
            } else {
                Log.w(TAG, "Power profile unchanged");
            }
        }
    }

    @SmallTest
    public void testGetPerfProfileHasAppProfiles() {
        //No application has power save by default
        assertEquals(false, mCMPerformanceManager.getProfileHasAppProfiles(
                PerformanceManager.PROFILE_POWER_SAVE));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        //reset
        mCMPerformanceManager.setPowerProfile(mSavedPerfProfile);
    }
}
