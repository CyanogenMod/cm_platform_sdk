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

package org.cyanogenmod.tests.hardware.unit;

import android.os.Build;
import android.os.SystemProperties;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import cyanogenmod.hardware.CMHardwareManager;

public class UniqueDeviceIDTest extends AndroidTestCase {
    private static final String TAG = UniqueDeviceIDTest.class.getSimpleName();
    private static final int MINIMUM_LENGTH = 3;
    private CMHardwareManager mCMHardwareManager;

    //TODO: Use the TYPE declaration from CMHardwareManager public interface in future
    private static final int TYPE_MMC0_CID = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCMHardwareManager = CMHardwareManager.getInstance(mContext);
    }

    @SmallTest
    public void testGetSerialNumber() {
        final int feature = CMHardwareManager.FEATURE_SERIAL_NUMBER;
        if (mCMHardwareManager.isSupported(feature)) {
            String notExpectedSerialNo = SystemProperties.get("ro.serialno");
            String actualSerialNo = mCMHardwareManager.getSerialNumber();
            assertNotNull(actualSerialNo);
            assertNotSame(notExpectedSerialNo, actualSerialNo);
        }
    }

    @SmallTest
    public void testGetUniqueDeviceId() {
        final int feature = CMHardwareManager.FEATURE_UNIQUE_DEVICE_ID;
        assertFeatureEnabledOnRetail(feature);
        if (mCMHardwareManager.isSupported(feature)) {
            String uniqueDeviceId = mCMHardwareManager.getUniqueDeviceId();
            //FIXME: This is based off the default implementation in cyngn/hw, make more robust
            assertNotNull(uniqueDeviceId);
            assertTrue(uniqueDeviceId.length() >= MINIMUM_LENGTH);
            assertTrue(isValidHexNumberAndType(uniqueDeviceId.substring(0, 3)));
        }
    }

    private void assertFeatureEnabledOnRetail(int feature) {
        if (TextUtils.equals(Build.TYPE, "user")) {
            assertTrue(mCMHardwareManager.isSupported(feature));
        }
    }

    private static boolean isValidHexNumberAndType(String target) {
        try {
            long value = Long.parseLong(target, 16);
            return isValidType((int) value);
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isValidType(int value) {
        switch (value) {
            case TYPE_MMC0_CID:
                return true;
            default:
                return false;
        }
    }
}
