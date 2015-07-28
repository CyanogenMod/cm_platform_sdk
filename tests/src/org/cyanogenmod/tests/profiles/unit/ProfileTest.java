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

import android.media.AudioManager;
import android.os.Parcel;
import android.test.AndroidTestCase;

import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.app.Profile;
import cyanogenmod.profiles.AirplaneModeSettings;
import cyanogenmod.profiles.BrightnessSettings;
import cyanogenmod.profiles.ConnectionSettings;
import cyanogenmod.profiles.LockSettings;
import cyanogenmod.profiles.RingModeSettings;
import cyanogenmod.profiles.StreamSettings;

/**
 * Created by adnan on 7/14/15.
 */
public class ProfileTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @MediumTest
    public void testProfileConnectionSettingsUnravelFromParcel() {
        Profile profile = new Profile("Connection Profile");
        ConnectionSettings expectedConnectionSettings =
                new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_GPS,
                        ConnectionSettings.BooleanState.STATE_DISALED, true);
        profile.setConnectionSettings(expectedConnectionSettings);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        ConnectionSettings actualConnectionSettings = fromParcel.getSettingsForConnection(
                expectedConnectionSettings.getConnectionId());

        assertEquals(expectedConnectionSettings.getConnectionId(),
                actualConnectionSettings.getConnectionId());
        assertEquals(expectedConnectionSettings.getValue(),
                actualConnectionSettings.getValue());
        assertEquals(expectedConnectionSettings.isDirty(),
                actualConnectionSettings.isDirty());
        assertEquals(expectedConnectionSettings.isOverride(),
                actualConnectionSettings.isOverride());
    }

    @MediumTest
    public void testProfileAirplaneModeSettingsUnravelFromParcel() {
        Profile profile = new Profile("AirplaneMode Profile");
        AirplaneModeSettings expectedAirplaneModeSettings =
                new AirplaneModeSettings(AirplaneModeSettings.BooleanState.STATE_ENABLED, true);
        profile.setAirplaneMode(expectedAirplaneModeSettings);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        AirplaneModeSettings actualAirplaneModeSettings = fromParcel.getAirplaneMode();

        assertEquals(expectedAirplaneModeSettings.getValue(),
                actualAirplaneModeSettings.getValue());
        assertEquals(expectedAirplaneModeSettings.isDirty(),
                actualAirplaneModeSettings.isDirty());
        assertEquals(expectedAirplaneModeSettings.isOverride(),
                expectedAirplaneModeSettings.isOverride());
    }

    @MediumTest
    public void testProfileBrightnessSettingsUnravelFromParcel() {
        Profile profile = new Profile("Brightness Profile");
        BrightnessSettings expectedBrightnessSettings = new BrightnessSettings(0, true);
        profile.setBrightness(expectedBrightnessSettings);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        BrightnessSettings actualBrightnessSettings = fromParcel.getBrightness();

        assertEquals(expectedBrightnessSettings.getValue(), actualBrightnessSettings.getValue());
        assertEquals(expectedBrightnessSettings.isOverride(),
                actualBrightnessSettings.isOverride());
        assertEquals(expectedBrightnessSettings.isDirty(), actualBrightnessSettings.isDirty());
    }

    @MediumTest
    public void testProfileRingmodeSettingsUnravelFromParcel() {
        Profile profile = new Profile("Ringmode Profile");
        RingModeSettings expectedRingModeSettings =
                new RingModeSettings(RingModeSettings.RING_MODE_MUTE, true);
        profile.setRingMode(expectedRingModeSettings);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        RingModeSettings actualRingModeSettings = fromParcel.getRingMode();

        assertNotNull(fromParcel);
        assertEquals(expectedRingModeSettings.getValue(), actualRingModeSettings.getValue());
        assertEquals(expectedRingModeSettings.isDirty(), actualRingModeSettings.isDirty());
        assertEquals(expectedRingModeSettings.isOverride(), actualRingModeSettings.isOverride());
    }

    @MediumTest
    public void testProfileStreamSettingsUnravelFromParcel() {
        Profile profile = new Profile("Stream Profile");
        StreamSettings expectedStreamSettings =
                new StreamSettings(AudioManager.STREAM_RING, 0, true);
        profile.setStreamSettings(expectedStreamSettings);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        StreamSettings actualStreamSettings = fromParcel.getSettingsForStream(
                expectedStreamSettings.getStreamId());

        assertNotNull(fromParcel);
        assertEquals(expectedStreamSettings.getValue(), actualStreamSettings.getValue());
        assertEquals(expectedStreamSettings.isOverride(), actualStreamSettings.isOverride());
        assertEquals(expectedStreamSettings.isDirty(), actualStreamSettings.isDirty());
    }

    @MediumTest
    public void testProfileLockSettingsUnravelFromParcel() {
        Profile profile = new Profile("Lock Profile");
        LockSettings expectedLockSettings = new LockSettings(Profile.LockMode.INSECURE);
        profile.setScreenLockMode(expectedLockSettings);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        LockSettings actualLockSettings = fromParcel.getScreenLockMode();

        assertNotNull(fromParcel);
        assertEquals(expectedLockSettings.getValue(), actualLockSettings.getValue());
        assertEquals(expectedLockSettings.isDirty(), actualLockSettings.isDirty());
    }

    @MediumTest
    public void testProfileUnravelFromParcel() {
        Profile profile = new Profile("Single Profile");
        profile.setProfileType(Profile.Type.TOGGLE);
        profile.setDozeMode(Profile.DozeMode.ENABLE);
        profile.setStatusBarIndicator(true);

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        profile.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        Profile fromParcel = Profile.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(profile.getName(), fromParcel.getName());
        assertEquals(profile.getProfileType(), fromParcel.getProfileType());
        assertEquals(profile.getDozeMode(), fromParcel.getDozeMode());
        assertEquals(profile.getStatusBarIndicator(), fromParcel.getStatusBarIndicator());
    }
}
