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

package org.cyanogenmod.tests.hardware.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.ICMHardwareService;

/**
 * Created by adnan on 9/1/15.
 */
public class CMHardwareManagerTest extends AndroidTestCase {
    private CMHardwareManager mCMHardwareManager;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCMHardwareManager = CMHardwareManager.getInstance(mContext);
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mCMHardwareManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        ICMHardwareService icmStatusBarManager = mCMHardwareManager.getService();
        assertNotNull(icmStatusBarManager);
    }
}
