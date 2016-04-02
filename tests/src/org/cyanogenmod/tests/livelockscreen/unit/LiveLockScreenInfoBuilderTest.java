/**
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.tests.livelockscreen.unit;

import android.content.ComponentName;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.app.LiveLockScreenInfo;

public class LiveLockScreenInfoBuilderTest extends AndroidTestCase {

    @SmallTest
    public void testConstructor() {
        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder().build();
        assertNotNull(llsInfo);
    }

    @SmallTest
    public void testBuildWithComponent() {
        ComponentName cn = new ComponentName("package", "class");

        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder()
                .setComponent(cn)
                .build();

        assertNotNull(llsInfo);
        assertEquals(cn, llsInfo.component);
    }

    @SmallTest
    public void testBuildWithNullComponentThrowsException() {
        ComponentName cn = null;

        LiveLockScreenInfo.Builder builder = new LiveLockScreenInfo.Builder();
        try {
            builder.setComponent(cn);
            throw new AssertionError("Failed to set null component");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testBuildWithPriority() {
        int priority = LiveLockScreenInfo.PRIORITY_HIGH;

        LiveLockScreenInfo llsInfo = new LiveLockScreenInfo.Builder()
                .setPriority(priority)
                .build();

        assertNotNull(llsInfo);
        assertEquals(priority, llsInfo.priority);
    }

    @SmallTest
    public void testBuildWithGreaterThanMaxPriority() {
        int priority = LiveLockScreenInfo.PRIORITY_MAX + 1;

        LiveLockScreenInfo.Builder builder = new LiveLockScreenInfo.Builder();
        try {
            builder.setPriority(priority);
            throw new AssertionError("Failed to set priority greater than max");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testBuildWithLessThanMinPriority() {
        int priority = LiveLockScreenInfo.PRIORITY_MIN - 1;

        LiveLockScreenInfo.Builder builder = new LiveLockScreenInfo.Builder();
        try {
            builder.setPriority(priority);
            throw new AssertionError("Failed to set priority greater than max");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }
}
