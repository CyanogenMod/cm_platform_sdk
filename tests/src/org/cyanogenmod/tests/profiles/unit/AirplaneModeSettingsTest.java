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
import cyanogenmod.profiles.AirplaneModeSettings;

public class AirplaneModeSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testConstructWholly() {
        AirplaneModeSettings airplaneModeSettings =
                new AirplaneModeSettings(
                        AirplaneModeSettings.BooleanState.STATE_ENABLED, true);
        assertEquals(AirplaneModeSettings.BooleanState.STATE_ENABLED, airplaneModeSettings.getValue());
        assertEquals(true, airplaneModeSettings.isOverride());
    }

    @SmallTest
    public void testVerifyOverride() {
        AirplaneModeSettings airplaneModeSettings =
                new AirplaneModeSettings(
                        AirplaneModeSettings.BooleanState.STATE_ENABLED, true);
        airplaneModeSettings.setOverride(false);
        assertEquals(false, airplaneModeSettings.isOverride());
    }

    @SmallTest
    public void testVerifyValue() {
        int expectedValue = AirplaneModeSettings.BooleanState.STATE_DISALED;
        AirplaneModeSettings airplaneModeSettings =
                new AirplaneModeSettings(
                        AirplaneModeSettings.BooleanState.STATE_ENABLED, true);
        airplaneModeSettings.setValue(expectedValue);
        assertEquals(expectedValue, airplaneModeSettings.getValue());
    }
}
