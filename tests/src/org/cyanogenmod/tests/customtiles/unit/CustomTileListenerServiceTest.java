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

import android.content.Intent;
import android.os.IBinder;

import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTileListenerService;

/**
 * Created by adnan on 7/15/15.
 */
public class CustomTileListenerServiceTest extends ServiceTestCase<CustomTileListenerService> {
    private CMStatusBarManager mCmStatusBarManager;

    public CustomTileListenerServiceTest() {
        super(CustomTileListenerService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCmStatusBarManager = CMStatusBarManager.getInstance(mContext);
    }

    @SmallTest
    public void testStartable() {
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), CustomTileListenerService.class);
        startService(startIntent);
        assertNotNull(getService());
    }

    @MediumTest
    public void testBindable() {
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), CustomTileListenerService.class);
        IBinder service = bindService(startIntent);
        assertNotNull(service);
    }
}
