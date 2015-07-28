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

package org.cyanogenmod.tests.profiles;

import android.media.AudioManager;
import android.os.Bundle;

import cyanogenmod.app.Profile;
import cyanogenmod.app.Profile.Type;

import cyanogenmod.app.ProfileManager;
import cyanogenmod.profiles.AirplaneModeSettings;
import cyanogenmod.profiles.BrightnessSettings;
import cyanogenmod.profiles.ConnectionSettings;
import cyanogenmod.profiles.LockSettings;
import cyanogenmod.profiles.RingModeSettings;
import cyanogenmod.profiles.StreamSettings;
import org.cyanogenmod.tests.TestActivity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by adnan on 6/26/15.
 */
public class ProfileTest extends TestActivity {
    private ProfileManager mProfileManager;
    private ArrayList<UUID> mProfileUuidList;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mProfileManager = ProfileManager.getInstance(this);
        mProfileUuidList = new ArrayList<UUID>();
    }

    @Override
    protected Test[] tests() {
        return mTests;
    }

    @Override
    protected String tag() {
        return null;
    }

    private Test[] mTests = new Test[] {
            new Test("test create random Profile") {
                public void run() {
                    Profile profile = new Profile("Test Profile");
                    profile.setProfileType(Type.TOGGLE);
                    profile.setExpandedDesktopMode(Profile.ExpandedDesktopMode.ENABLE);
                    profile.setDozeMode(Profile.DozeMode.DEFAULT);
                    profile.setScreenLockMode(new LockSettings(Profile.LockMode.DISABLE));
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                }
            },
            new Test("test add static Profile") {
                public void run() {
                    Profile profile = new Profile("Test Profile-Active",
                            0, UUID.fromString("65cd0d0c-1c42-11e5-9a21-1697f925ec7b"));
                    profile.setProfileType(Type.TOGGLE);
                    profile.setExpandedDesktopMode(Profile.ExpandedDesktopMode.ENABLE);
                    profile.setDozeMode(Profile.DozeMode.DEFAULT);
                    profile.setScreenLockMode(new LockSettings(Profile.LockMode.DISABLE));
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("test remove static Profile") {
                public void run() {
                    mProfileManager.removeProfile(
                            mProfileManager.getProfile("65cd0d0c-1c42-11e5-9a21-1697f925ec7b"));
                }
            },
            new Test("test create Profile and Set Active") {
                public void run() {
                    Profile profile = new Profile("Test Profile-Active");
                    profile.setProfileType(Type.TOGGLE);
                    profile.setExpandedDesktopMode(Profile.ExpandedDesktopMode.ENABLE);
                    profile.setDozeMode(Profile.DozeMode.DEFAULT);
                    profile.setScreenLockMode(new LockSettings(Profile.LockMode.DISABLE));
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("test create Profile, override airplane settings, and set active") {
                @Override
                protected void run() {
                    Profile profile = new Profile("Test Profile-Override-AIR-Active");
                    profile.setProfileType(Type.TOGGLE);
                    AirplaneModeSettings airplaneModeSettings =
                            new AirplaneModeSettings(
                                    AirplaneModeSettings.BooleanState.STATE_ENABLED, true);
                    profile.setAirplaneMode(airplaneModeSettings);
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("test create Profile, override ring stream settings, and set active") {
                @Override
                protected void run() {
                    Profile profile = new Profile("Test Profile-Override-RNG-Active");
                    profile.setProfileType(Type.TOGGLE);
                    StreamSettings streamSettings =
                            new StreamSettings(AudioManager.STREAM_RING, 0, true);
                    profile.setStreamSettings(streamSettings);
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("test create Profile, override BT connection settings, and set active") {
                @Override
                protected void run() {
                    Profile profile = new Profile("Test Profile-Override-CNNCT-Active");
                    profile.setProfileType(Type.TOGGLE);
                    ConnectionSettings connectionSettings =
                            new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH,
                                    ConnectionSettings.BooleanState.STATE_ENABLED, true);
                    profile.setConnectionSettings(connectionSettings);
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("test create Profile, override brightness settings, and set active") {
                @Override
                protected void run() {
                    Profile profile = new Profile("Test Profile-Override-BRGHT-Active");
                    profile.setProfileType(Type.TOGGLE);
                    BrightnessSettings brightnessSettings =
                            new BrightnessSettings(0, true);
                    profile.setBrightness(brightnessSettings);
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("test create Profile, override ringmode settings, and set active") {
                @Override
                protected void run() {
                    Profile profile = new Profile("Test Profile-Override-RNGMD-Active");
                    profile.setProfileType(Type.TOGGLE);
                    RingModeSettings ringSettings = new RingModeSettings(
                            RingModeSettings.RING_MODE_MUTE, true);
                    profile.setRingMode(ringSettings);
                    mProfileUuidList.add(profile.getUuid());
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
            new Test("Reset All") {
                @Override
                protected void run() {
                    // make sure we remove our own
                    for (UUID uuid: mProfileUuidList) {
                        Profile profile = mProfileManager.getProfile(uuid);
                        if (profile != null) {
                            mProfileManager.removeProfile(profile);
                        }
                    }
                    mProfileManager.resetAll();
                }
            }
    };
}
