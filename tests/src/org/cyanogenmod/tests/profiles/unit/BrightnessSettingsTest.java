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
import cyanogenmod.profiles.BrightnessSettings;

public class BrightnessSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testConstructWholly() {
        BrightnessSettings brightnessSettings =
                new BrightnessSettings(0, true);
        assertEquals(0, brightnessSettings.getValue());
        assertEquals(true, brightnessSettings.isOverride());
    }

    @SmallTest
    public void testVerifyOverride() {
        BrightnessSettings brightnessSettings =
                new BrightnessSettings(0, true);
        brightnessSettings.setOverride(false);
        assertEquals(false, brightnessSettings.isOverride());
    }

    @SmallTest
    public void testVerifyValue() {
        int expectedValue = 30;
        BrightnessSettings brightnessSettings =
                new BrightnessSettings(0, true);
        brightnessSettings.setValue(expectedValue);
        assertEquals(expectedValue, brightnessSettings.getValue());
    }
}
