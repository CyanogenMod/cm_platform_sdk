package org.cyanogenmod.tests.settings;

import org.cyanogenmod.tests.TestActivity;
import cyanogenmod.app.PartnerInterface;

/**
 * Tests functionality added in {@link cyanogenmod.app.PartnerInterface}
 */
public class CMPartnerInterfaceTest extends TestActivity {
    PartnerInterface mPartnerInterface;
    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        mPartnerInterface = PartnerInterface.getInstance(this);
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("Test set airplane mode to on") {
                public void run() {
                    mPartnerInterface.setAirplaneModeEnabled(true);
                }
            },
            new Test("Test set airplane mode to off") {
                public void run() {
                    mPartnerInterface.setAirplaneModeEnabled(false);
                }
            },
            new Test("Test set mobile data to on") {
                public void run() {
                    mPartnerInterface.setMobileDataEnabled(true);
                }
            },
            new Test("Test set mobile data to off") {
                public void run() {
                    mPartnerInterface.setMobileDataEnabled(false);
                }
            },
            new Test("Test reboot the device") {
                public void run() {
                    mPartnerInterface.rebootDevice();
                }
            },
            new Test("Test shutdown the device") {
                public void run() {
                    mPartnerInterface.shutdownDevice();
                }
            },
            new Test("Test set zen mode to important interruptions") {
                public void run() {
                    mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                }
            },
            new Test("Test set zen mode to no interruptions") {
                public void run() {
                    mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS);
                }
            },
            new Test("Test turn zen mode off") {
                public void run() {
                    mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_OFF);
                }
            },

            new Test("Test set zen mode to important interruptions with 15 mins duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_15_MINUTES_MS);
                }
            },
            new Test("Test set zen mode to no interruptions with 15 mins duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_15_MINUTES_MS);
                }
            },
            new Test("Test set zen mode to important interruptions with 1 hour 15 mins duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_1_HOUR_MS + PartnerInterface.ZEN_MODE_DURATION_15_MINUTES_MS);
                }
            },
            new Test("Test set zen mode to no interruptions with 1 hour 15 mins duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_1_HOUR_MS + PartnerInterface.ZEN_MODE_DURATION_15_MINUTES_MS);
                }
            },
            new Test("Test set zen mode to important interruptions with 12 hours duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_12_HOURS_MS);
                }
            },
            new Test("Test set zen mode to no interruptions with 12 hours duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_12_HOURS_MS);
                }
            },
            new Test("Test set zen mode to important interruptions with MAX duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_MAX_MS);
                }
            },
            new Test("Test set zen mode to no interruptions with MAX duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, PartnerInterface.ZEN_MODE_DURATION_MAX_MS);
                }
            },
            new Test("Test set zen mode to important interruptions with MAX_LONG duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, Long.MAX_VALUE);
                }
            },
            new Test("Test set zen mode to no interruptions with MAX_LONG duration") {
                public void run() {
                    mPartnerInterface.setZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, Long.MAX_VALUE);
                }
            },

    };
}
