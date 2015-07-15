package org.cyanogenmod.tests.profiles.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.app.IProfileManager;

/**
 * Created by adnan on 7/15/15.
 */
public class ProfileManagerTest extends AndroidTestCase {
    private ProfileManager mProfileManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProfileManager = ProfileManager.getInstance(mContext);
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mProfileManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        IProfileManager iProfileManager = mProfileManager.getService();
        assertNotNull(iProfileManager);
    }
}
