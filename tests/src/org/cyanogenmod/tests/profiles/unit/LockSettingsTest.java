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
import cyanogenmod.app.Profile;
import cyanogenmod.profiles.LockSettings;

public class LockSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testConstructWholly() {
        LockSettings lockSettings = new LockSettings(Profile.LockMode.INSECURE);
        assertEquals(Profile.LockMode.INSECURE, lockSettings.getValue());
    }

    @SmallTest
    public void testVerifyValue() {
        int expectedValue = Profile.LockMode.DEFAULT;
        LockSettings lockSettings = new LockSettings(Profile.LockMode.INSECURE);
        lockSettings.setValue(expectedValue);
        assertEquals(expectedValue, lockSettings.getValue());
    }
}
