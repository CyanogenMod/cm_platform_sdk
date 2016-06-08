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

import android.media.AudioManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.profiles.StreamSettings;

public class StreamSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testConstructManually() {
        StreamSettings streamSettings =
                new StreamSettings(AudioManager.STREAM_RING);
        assertEquals(AudioManager.STREAM_RING, streamSettings.getStreamId());
    }

    @SmallTest
    public void testConstructWholly() {
        StreamSettings streamSettings =
                new StreamSettings(AudioManager.STREAM_RING, 0, true);
        assertEquals(AudioManager.STREAM_RING, streamSettings.getStreamId());
        assertEquals(0, streamSettings.getValue());
        assertEquals(true, streamSettings.isOverride());
    }

    @SmallTest
    public void testVerifyOverride() {
        StreamSettings streamSettings =
                new StreamSettings(AudioManager.STREAM_RING);
        streamSettings.setOverride(true);
        assertEquals(true, streamSettings.isOverride());
    }

    @SmallTest
    public void testVerifyValue() {
        int expectedValue = AudioManager.STREAM_ALARM;
        StreamSettings streamSettings =
                new StreamSettings(AudioManager.STREAM_RING);
        streamSettings.setValue(expectedValue);
        assertEquals(expectedValue, streamSettings.getValue());
    }
}
