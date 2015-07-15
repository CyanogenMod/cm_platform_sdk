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
import cyanogenmod.app.ProfileManager;
import cyanogenmod.app.IProfileManager;

/**
 * Created by adnan on 7/15/15.
 */
public class ProfileManagerTest extends AndroidTestCase {
    private ProfileManager mProfileManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProfileManager = ProfileManager.getInstance(mContext);
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
}
