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

package org.cyanogenmod.tests.profiles.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.profiles.RingModeSettings;

public class RingModeSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testConstructWholly() {
        RingModeSettings ringSettings = new RingModeSettings(
                RingModeSettings.RING_MODE_MUTE, true);
        assertEquals(RingModeSettings.RING_MODE_MUTE, ringSettings.getValue());
        assertEquals(true, ringSettings.isOverride());
    }

    @SmallTest
    public void testVerifyOverride() {
        RingModeSettings ringSettings = new RingModeSettings(
                RingModeSettings.RING_MODE_MUTE, true);
        ringSettings.setOverride(false);
        assertEquals(false, ringSettings.isOverride());
    }

    @SmallTest
    public void testVerifyValue() {
        String expectedValue = RingModeSettings.RING_MODE_NORMAL;
        RingModeSettings ringSettings = new RingModeSettings(
                RingModeSettings.RING_MODE_MUTE, true);
        ringSettings.setValue(expectedValue);
        assertEquals(expectedValue, ringSettings.getValue());
    }
}
