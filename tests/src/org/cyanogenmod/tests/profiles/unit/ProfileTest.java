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

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.Profile;
import cyanogenmod.profiles.AirplaneModeSettings;
import cyanogenmod.profiles.BrightnessSettings;
import cyanogenmod.profiles.ConnectionSettings;
import cyanogenmod.profiles.LockSettings;
import cyanogenmod.profiles.RingModeSettings;
import cyanogenmod.profiles.StreamSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ProfileTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Only run this if we support profiles service
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.PROFILES));
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

    private static final int EXPECTED_PROFILE_TRIGGER_TYPE = Profile.TriggerType.WIFI;
    private static final String EXPECTED_PROFILE_TRIGGER_ID = "1337";
    private static final int EXPECTED_PROFILE_TRIGGER_STATE = Profile.TriggerState.ON_CONNECT;
    private static final String EXPECTED_PROFILE_TRIGGER_NAME = "ON_CONNECT_WIFI_TRIGGER";
    private Profile.ProfileTrigger createSampleProfileTrigger() {
        return new Profile.ProfileTrigger(EXPECTED_PROFILE_TRIGGER_TYPE,
                EXPECTED_PROFILE_TRIGGER_ID, EXPECTED_PROFILE_TRIGGER_STATE,
                EXPECTED_PROFILE_TRIGGER_NAME);
    }

    @SmallTest
    public void testProfileTriggerId() {
        Profile.ProfileTrigger profileTrigger = createSampleProfileTrigger();
        assertEquals(EXPECTED_PROFILE_TRIGGER_ID, profileTrigger.getId());
    }

    @SmallTest
    public void testProfileTriggerName() {
        Profile.ProfileTrigger profileTrigger = createSampleProfileTrigger();
        assertEquals(EXPECTED_PROFILE_TRIGGER_NAME, profileTrigger.getName());
    }

    @SmallTest
    public void testProfileTriggerState() {
        Profile.ProfileTrigger profileTrigger = createSampleProfileTrigger();
        assertEquals(EXPECTED_PROFILE_TRIGGER_STATE, profileTrigger.getState());
    }

    @SmallTest
    public void testProfileTriggerType() {
        Profile.ProfileTrigger profileTrigger = createSampleProfileTrigger();
        assertEquals(EXPECTED_PROFILE_TRIGGER_STATE, profileTrigger.getType());
    }

    @SmallTest
    public void testProfileConstructor() {
        String expectedName = "PROFILE_NAME";
        Profile profile = new Profile(expectedName);
        assertEquals(expectedName, profile.getName());
    }

    @SmallTest
    public void testProfileAddSecondaryUuid() {
        UUID[] expectedUUIDs = new UUID[2];
        UUID expectedUUID1 = UUID.randomUUID();
        UUID expectedUUID2 = UUID.randomUUID();
        expectedUUIDs[0] = expectedUUID1;
        expectedUUIDs[1] = expectedUUID2;

        Profile profile = new Profile("Single Profile");
        profile.addSecondaryUuid(expectedUUID1);
        profile.addSecondaryUuid(expectedUUID2);

        UUID[] actualUUIDs = profile.getSecondaryUuids();
        for (int i = 0; i < actualUUIDs.length; i++) {
            assertEquals(actualUUIDs[i], expectedUUIDs[i]);
        }

        profile.setSecondaryUuids(Arrays.asList(expectedUUIDs));
        for (int i = 0; i < actualUUIDs.length; i++) {
            assertEquals(actualUUIDs[i], expectedUUIDs[i]);
        }
    }

    @SmallTest
    public void testProfileGetAirplaneMode() {
        Profile profile = new Profile("AirplaneMode Profile");
        AirplaneModeSettings expectedAirplaneModeSettings =
                new AirplaneModeSettings(AirplaneModeSettings.BooleanState.STATE_ENABLED, true);
        profile.setAirplaneMode(expectedAirplaneModeSettings);

        AirplaneModeSettings actualAirplaneModeSettings = profile.getAirplaneMode();
        assertEquals(expectedAirplaneModeSettings.getValue(),
                actualAirplaneModeSettings.getValue());
        assertEquals(expectedAirplaneModeSettings.isOverride(),
                actualAirplaneModeSettings.isOverride());
    }

    @SmallTest
    public void testProfileGetBrightness() {
        Profile profile = new Profile("Brightness Profile");
        BrightnessSettings expectedBrightnessSettings = new BrightnessSettings(0, true);
        profile.setBrightness(expectedBrightnessSettings);

        BrightnessSettings actualBrightnessSettings = profile.getBrightness();
        assertEquals(expectedBrightnessSettings.getValue(), actualBrightnessSettings.getValue());
        assertEquals(expectedBrightnessSettings.isOverride(), actualBrightnessSettings.isOverride());
    }

    @SmallTest
    public void testProfileGetConnectionSettingsWithSubId() {
        int targetSubId = 0;
        Profile profile = new Profile("Connection Sub Id Profile");
        ConnectionSettings expectedConnectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_2G3G4G,
                ConnectionSettings.BooleanState.STATE_ENABLED, true);
        expectedConnectionSettings.setSubId(targetSubId);
        profile.setConnectionSettings(expectedConnectionSettings);

        ConnectionSettings actualConnectionSettings =
                profile.getConnectionSettingWithSubId(targetSubId);
        assertEquals(expectedConnectionSettings.getConnectionId(),
                actualConnectionSettings.getConnectionId());
        assertEquals(expectedConnectionSettings.getValue(), actualConnectionSettings.getValue());
        assertEquals(expectedConnectionSettings.isOverride(),
                actualConnectionSettings.isOverride());
    }

    @SmallTest
    public void testProfileGetConnectionSettings() {
        Profile profile = new Profile("Connection Profile");

        ConnectionSettings expectedConnectionSettings1 = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_2G3G4G,
                ConnectionSettings.BooleanState.STATE_ENABLED, true);
        profile.setConnectionSettings(expectedConnectionSettings1);
        ConnectionSettings expectedConnectionSettings2 = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH,
                ConnectionSettings.BooleanState.STATE_DISALED, false);
        profile.setConnectionSettings(expectedConnectionSettings2);

        List<ConnectionSettings> expectedConnectionSettings = new ArrayList<>();
        // inverted because the backing structure does it this way :/
        expectedConnectionSettings.add(expectedConnectionSettings2);
        expectedConnectionSettings.add(expectedConnectionSettings1);

        List<ConnectionSettings> actualConnectionSettings = new ArrayList<>(
                profile.getConnectionSettings());
        for (int i = 0; i < actualConnectionSettings.size(); i++) {
            ConnectionSettings expectedConnectionSetting = expectedConnectionSettings.get(i);
            ConnectionSettings actualConnectionSetting = actualConnectionSettings.get(i);
            assertEquals(expectedConnectionSetting.getConnectionId(),
                    actualConnectionSetting.getConnectionId());
            assertEquals(expectedConnectionSetting.getValue(), actualConnectionSetting.getValue());
            assertEquals(expectedConnectionSetting.isOverride(),
                    actualConnectionSetting.isOverride());
        }
    }

    @SmallTest
    public void testProfileGetDozeMode() {
        int expectedDozeMode = Profile.DozeMode.ENABLE;
        Profile profile = new Profile("Doze Mode Profile");
        profile.setDozeMode(expectedDozeMode);
        assertEquals(expectedDozeMode, profile.getDozeMode());
    }

    @SmallTest
    public void testProfileGetExpandedDesktopMode() {
        int expectedExpandedDesktopMode = Profile.ExpandedDesktopMode.ENABLE;
        Profile profile = new Profile("Desktop Mode Profile");
        profile.setExpandedDesktopMode(expectedExpandedDesktopMode);
        assertEquals(expectedExpandedDesktopMode, profile.getExpandedDesktopMode());
    }

    @SmallTest
    public void testProfileGetNotificationLightMode() {
        int expectedNotificationLightMode = Profile.NotificationLightMode.ENABLE;
        Profile profile = new Profile("Notification Light Mode Profile");
        profile.setNotificationLightMode(expectedNotificationLightMode);
        assertEquals(expectedNotificationLightMode, profile.getNotificationLightMode());
    }

    @SmallTest
    public void testProfileGetProfileType() {
        int expectedProfileType = Profile.Type.CONDITIONAL;
        Profile profile = new Profile("Profile Type Profile");
        profile.setProfileType(expectedProfileType);
        assertEquals(expectedProfileType, profile.getProfileType());
    }

    @SmallTest
    public void testProfileGetRingMode() {
        Profile profile = new Profile("Ringmode Profile");
        RingModeSettings expectedRingModeSettings =
                new RingModeSettings(RingModeSettings.RING_MODE_MUTE, true);
        profile.setRingMode(expectedRingModeSettings);

        RingModeSettings actualRingModeSettings = profile.getRingMode();
        assertEquals(expectedRingModeSettings.getValue(), actualRingModeSettings.getValue());
        assertEquals(expectedRingModeSettings.isDirty(), actualRingModeSettings.isDirty());
        assertEquals(expectedRingModeSettings.isOverride(), actualRingModeSettings.isOverride());
    }

    @SmallTest
    public void testProfileGetLockScreenMode() {
        Profile profile = new Profile("Lock Profile");
        LockSettings expectedLockSettings = new LockSettings(Profile.LockMode.INSECURE);
        profile.setScreenLockMode(expectedLockSettings);

        LockSettings actualLockSettings = profile.getScreenLockMode();
        assertEquals(expectedLockSettings.getValue(), actualLockSettings.getValue());
        assertEquals(expectedLockSettings.isDirty(), actualLockSettings.isDirty());
    }

    @SmallTest
    public void testProfileGetSettingForConnection() {
        Profile profile = new Profile("Connection Profile");
        ConnectionSettings expectedConnectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_2G3G4G,
                ConnectionSettings.BooleanState.STATE_ENABLED, true);
        profile.setConnectionSettings(expectedConnectionSettings);

        ConnectionSettings actualConnectionSettings =
                profile.getSettingsForConnection(ConnectionSettings.PROFILE_CONNECTION_2G3G4G);
        assertEquals(expectedConnectionSettings.getConnectionId(),
                actualConnectionSettings.getConnectionId());
        assertEquals(expectedConnectionSettings.getValue(), actualConnectionSettings.getValue());
        assertEquals(expectedConnectionSettings.isOverride(),
                actualConnectionSettings.isOverride());
    }

    @SmallTest
    public void testProfileGetSettingForStream() {
        Profile profile = new Profile("Stream Profile");
        StreamSettings expectedStreamSettings =
                new StreamSettings(AudioManager.STREAM_RING, 0, true);
        profile.setStreamSettings(expectedStreamSettings);

        StreamSettings actualStreamSettings = profile.getSettingsForStream(
                expectedStreamSettings.getStreamId());

        assertEquals(expectedStreamSettings.getValue(), actualStreamSettings.getValue());
        assertEquals(expectedStreamSettings.isOverride(), actualStreamSettings.isOverride());
        assertEquals(expectedStreamSettings.isDirty(), actualStreamSettings.isDirty());
    }

    @SmallTest
    public void testProfileGetStreamSettings() {
        Profile profile = new Profile("Stream Profile");
        StreamSettings expectedStreamSettings1 =
                new StreamSettings(AudioManager.STREAM_RING, 0, true);
        profile.setStreamSettings(expectedStreamSettings1);
        StreamSettings expectedStreamSettings2 =
                new StreamSettings(AudioManager.STREAM_RING, 0, true);
        profile.setStreamSettings(expectedStreamSettings2);

        List<StreamSettings> expectedStreamSettings = new ArrayList<>();
        expectedStreamSettings.add(expectedStreamSettings1);
        expectedStreamSettings.add(expectedStreamSettings2);

        List<StreamSettings> actualStreamSettings = new ArrayList<>(
                profile.getStreamSettings());

        for (int i = 0; i < actualStreamSettings.size(); i++) {
            StreamSettings expectedStreamSetting = actualStreamSettings.get(i);
            StreamSettings actualStreamSetting = actualStreamSettings.get(i);
            assertEquals(expectedStreamSetting.getStreamId(),
                    actualStreamSetting.getStreamId());
            assertEquals(expectedStreamSetting.getValue(), actualStreamSetting.getValue());
            assertEquals(expectedStreamSetting.isOverride(),
                    actualStreamSetting.isOverride());
        }
    }

    @SmallTest
    public void testProfileGetTriggerState() {
        Profile profile = new Profile("ProfileTrigger Profile");
        Profile.ProfileTrigger profileTrigger = createSampleProfileTrigger();
        profile.setTrigger(profileTrigger);
        assertEquals(EXPECTED_PROFILE_TRIGGER_STATE,
                profile.getTriggerState(EXPECTED_PROFILE_TRIGGER_TYPE,
                        EXPECTED_PROFILE_TRIGGER_ID));
    }

    @SmallTest
    public void testProfileGetTriggersFromType() {
        Profile profile = new Profile("ProfileTrigger Profile");
        Profile.ProfileTrigger profileTrigger1 = createSampleProfileTrigger();
        Profile.ProfileTrigger profileTrigger2 = createSampleProfileTrigger();
        profile.setTrigger(profileTrigger1);
        profile.setTrigger(profileTrigger2);

        List<Profile.ProfileTrigger> expectedProfileTriggers = new ArrayList<>();
        expectedProfileTriggers.add(profileTrigger1);
        expectedProfileTriggers.add(profileTrigger2);

        List<Profile.ProfileTrigger> actualProfileTriggers = new ArrayList<>(
                profile.getTriggersFromType(EXPECTED_PROFILE_TRIGGER_TYPE));

        for (int i = 0; i < actualProfileTriggers.size(); i++) {
            Profile.ProfileTrigger expectedProfileTrigger = expectedProfileTriggers.get(i);
            Profile.ProfileTrigger actualProfileTrigger = expectedProfileTriggers.get(i);
            assertEquals(expectedProfileTrigger.getId(), actualProfileTrigger.getId());
            assertEquals(expectedProfileTrigger.getName(), actualProfileTrigger.getName());
            assertEquals(expectedProfileTrigger.getState(), actualProfileTrigger.getState());
            assertEquals(expectedProfileTrigger.getType(), actualProfileTrigger.getType());
        }
    }

    @SmallTest
    public void testProfileIsConditionalType() {
        Profile profile = new Profile("Mutable Profile");
        profile.setProfileType(Profile.Type.TOGGLE);
        assertFalse(profile.isConditionalType());
        profile.setProfileType(Profile.Type.CONDITIONAL);
        assertTrue(profile.isConditionalType());
    }

    @SmallTest
    public void testProfileSetName() {
        String expectedProfileName = "MUTABLE Profile";
        Profile profile = new Profile("Mutable Profile");
        profile.setName(expectedProfileName);
        assertEquals(expectedProfileName, profile.getName());
    }
}
