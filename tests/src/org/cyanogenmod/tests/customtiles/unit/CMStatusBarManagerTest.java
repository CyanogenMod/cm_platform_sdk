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

package org.cyanogenmod.tests.customtiles.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.ICMStatusBarManager;

/**
 * Created by adnan on 7/14/15.
 */
public class CMStatusBarManagerTest extends AndroidTestCase {
    private CMStatusBarManager mCMStatusBarManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Only run this if we support cm status bar service
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.STATUSBAR));
        mCMStatusBarManager = CMStatusBarManager.getInstance(mContext);
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mCMStatusBarManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        ICMStatusBarManager icmHardwareManagerService = mCMStatusBarManager.getService();
        assertNotNull(icmHardwareManagerService);
    }
}
