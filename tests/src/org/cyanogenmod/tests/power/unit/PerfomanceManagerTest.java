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
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManager;

/**
 * Code coverage for public facing {@link PerformanceManager} interfaces.
 * The test below will save and restore the current performance profile to
 * not impact successive tests.
 */
public class PerfomanceManagerTest extends AndroidTestCase {
    private static final String TAG = PerfomanceManagerTest.class.getSimpleName();
    private static final int IMPOSSIBLE_POWER_PROFILE = -1;
    private PerformanceManager mCMPerformanceManager;
    private int mSavedPerfProfile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCMPerformanceManager = PerformanceManager.getInstance(mContext);
        // Save the perf profile for later restore.
        mSavedPerfProfile = mCMPerformanceManager.getPowerProfile();
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
        // Assert that we can even set perf profiles
        assertTrue(mCMPerformanceManager.getNumberOfProfiles() > 0);
    }

    @SmallTest
    public void testGetPowerProfile() {
        assertNotSame(IMPOSSIBLE_POWER_PROFILE, mSavedPerfProfile);
    }

    @SmallTest
    public void testSetAndGetPowerProfile() {
        int[] expectedStates = new int[] { PerformanceManager.PROFILE_POWER_SAVE,
                PerformanceManager.PROFILE_BALANCED,
                PerformanceManager.PROFILE_HIGH_PERFORMANCE};

        // Set the state
        for (int profile : expectedStates) {
            // If the target perf profile is the same as the current one,
            // setPowerProfile will noop, ignore that scenario
            if (mCMPerformanceManager.getPowerProfile() != profile) {
                mCMPerformanceManager.setPowerProfile(profile);
                // Verify that it was set correctly.
                assertEquals(profile, mCMPerformanceManager.getPowerProfile());
            }
        }
    }

    @SmallTest
    public void testGetPerfProfileHasAppProfiles() {
        // No application has power save by default
        assertEquals(false, mCMPerformanceManager.getProfileHasAppProfiles(
                PerformanceManager.PROFILE_POWER_SAVE));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // Reset
        mCMPerformanceManager.setPowerProfile(mSavedPerfProfile);
    }
}
