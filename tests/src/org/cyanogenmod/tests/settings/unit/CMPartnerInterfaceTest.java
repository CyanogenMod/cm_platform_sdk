package org.cyanogenmod.tests.settings.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.provider.Settings;
import cyanogenmod.app.PartnerInterface;

/**
 * Unit test for PartnerInterface
 */
public class CMPartnerInterfaceTest extends AndroidTestCase {

    private static final String TAG = "CMPartnerInterfaceTest";

    private PartnerInterface mPartnerInterface;
    private boolean mAirplaneModeEnabled;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPartnerInterface = PartnerInterface.getInstance(getContext());
        // Remember the initial state
        mAirplaneModeEnabled = getAirplaneModeEnabled();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // Restore airplane mode
        mPartnerInterface.setAirplaneModeEnabled(mAirplaneModeEnabled);
    }

    @SmallTest
    public void testPartnerInterfaceExists() {
        assertNotNull(mPartnerInterface);
    }

    @SmallTest
    public void testPartnerInterfaceAvailable() {
        assertNotNull(mPartnerInterface.getService());
    }

    @SmallTest
    public void testSetAirplaneModeOn() {
        mPartnerInterface.setAirplaneModeEnabled(true);
        assertTrue(getAirplaneModeEnabled());
    }

    @SmallTest
    public void testSetAirplaneModeOff() {
        mPartnerInterface.setAirplaneModeEnabled(false);
        assertTrue(getAirplaneModeEnabled() == false);
    }

    private boolean getAirplaneModeEnabled() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }
}
