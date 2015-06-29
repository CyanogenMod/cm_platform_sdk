package org.cyanogenmod.tests.profiles;

import android.os.Bundle;

import cyanogenmod.app.Profile;
import cyanogenmod.app.Profile.Type;

import cyanogenmod.app.ProfileManager;
import org.cyanogenmod.tests.TestActivity;

import java.util.UUID;

/**
 * Created by adnan on 6/26/15.
 */
public class ProfileTest extends TestActivity {
    private ProfileManager mProfileManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mProfileManager = ProfileManager.getInstance(this);
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
                    profile.setScreenLockMode(Profile.LockMode.DISABLE);
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
                    profile.setScreenLockMode(Profile.LockMode.DISABLE);
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
                    profile.setScreenLockMode(Profile.LockMode.DISABLE);
                    mProfileManager.addProfile(profile);
                    mProfileManager.setActiveProfile(profile.getUuid());
                }
            },
    };
}
