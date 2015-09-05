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
    };
}
