package org.cyanogenmod.tests.settings;

import org.cyanogenmod.tests.TestActivity;
import cyanogenmod.app.SettingsManager;

/**
 * Tests functionality added in {@link cyanogenmod.app.SettingsManager}
 */
public class CMSettingsManagerTest extends TestActivity {
    SettingsManager mSettingsManager;
    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        mSettingsManager = SettingsManager.getInstance(this);
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("Test set airplane mode to on") {
                public void run() {
                    mSettingsManager.setAirplaneModeEnabled(true);
                }
            },
            new Test("Test set airplane mode to off") {
                public void run() {
                    mSettingsManager.setAirplaneModeEnabled(false);
                }
            },
            new Test("Test set mobile data to on") {
                public void run() {
                    mSettingsManager.setMobileDataEnabled(true);
                }
            },
            new Test("Test set mobile data to off") {
                public void run() {
                    mSettingsManager.setMobileDataEnabled(false);
                }
            },
            new Test("Test reboot the device") {
                public void run() {
                    mSettingsManager.rebootDevice();
                }
            },
            new Test("Test shutdown the device") {
                public void run() {
                    mSettingsManager.shutdownDevice();
                }
            },
            new Test("Test set zen mode to important interruptions") {
                public void run() {
                    mSettingsManager.setZenMode(SettingsManager.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                }
            },
            new Test("Test set zen mode to no interruptions") {
                public void run() {
                    mSettingsManager.setZenMode(SettingsManager.ZEN_MODE_NO_INTERRUPTIONS);
                }
            },
            new Test("Test turn zen mode off") {
                public void run() {
                    mSettingsManager.setZenMode(SettingsManager.ZEN_MODE_OFF);
                }
            },
    };
}
