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

package org.cyanogenmod.tests.versioning.unit;

import android.content.Context;
import android.os.Binder;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;
import org.cyanogenmod.tests.CyanogenModTestApplication;
import org.cyanogenmod.tests.versioning.unit.apiv2.ApiV2PriorReleaseInterfaces;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * These tests validate the enumerated binder transaction call ids per each
 * release api level against the current framework.
 *
 * This is to validate that no interface contracts are broken in terms of binder
 * call method mapping between releases.
 *
 * After too much searching on the internet, I found that no one was bored enough to
 * spend time on this awesomely boring concept. But I am.
 *
 * Also this is a fun endeavour into parameterized unit testing, and by fun, I mean
 * horrible and full of drinking.
 *
 * If you need to blame anyone for this concept, look no further than danesh@cyngn.com
 */
@RunWith(Parameterized.class)
@LargeTest
public class BinderTransactionTest extends AndroidTestCase {
    private static final String STUB_SUFFIX = "$Stub";
    private static final String CYANOGENMOD_NAMESPACE = "cyanogenmod";
    private static final String TRANSACTION_PREFIX = "TRANSACTION_";

    private static final int NOT_FROM_PRIOR_RELEASE = -1;

    private String mField;
    private int mExpectedValue;
    private int mActualValue;
    private static Context sContext;

    private static ArrayList<String> mKnownSdkClasses;
    private static Map<String, Integer> mApiMethodsAndValues = new HashMap<String, Integer>();

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testClassLoaderGivesSDKClasses() {
        /**
         * Verify we can retrieve our sdk classes from this package
         */
        assertNotNull(mKnownSdkClasses);
        assertTrue(mKnownSdkClasses.size() > 0);
    }

    private static void doSetup() {
        mKnownSdkClasses = MagicalDexHelper.getLoadedClasses(
                CyanogenModTestApplication.getStaticApplicationContext(), CYANOGENMOD_NAMESPACE);
        sContext = CyanogenModTestApplication.getStaticApplicationContext();
        mApiMethodsAndValues.putAll(ApiV2PriorReleaseInterfaces.getInterfaces());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        doSetup();
        //Ughhh, lets pretend this never happened
        ArrayList<String> targetFields = new ArrayList<String>();
        ArrayList<Integer> actualValues = new ArrayList<Integer>();

        for (String sClazz : mKnownSdkClasses) {
            if (sClazz.endsWith(STUB_SUFFIX)) {
                try {
                    Class clazz = MagicalDexHelper.loadClassForNameSpace(CyanogenModTestApplication
                            .getStaticApplicationContext(), sClazz);
                    Field[] fields = clazz.getDeclaredFields();

                    for (Field field : fields) {
                        if (field.getName().startsWith(TRANSACTION_PREFIX)) {
                            field.setAccessible(true);
                            targetFields.add(field.getName()
                                    .substring(TRANSACTION_PREFIX.length()));
                            try {
                                actualValues.add(field.getInt(clazz));
                            } catch (IllegalAccessException e) {
                                throw new AssertionError("Unable to access " + field.getName());
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new AssertionError("Unable to load class " + sClazz);
                }
            }
        }
        Object[][] values = new Object[targetFields.size()][3];

        for (int i = 0; i < targetFields.size(); i++) {
            String targetField = targetFields.get(i);
            values[i][0] = targetField;
            values[i][1] = lookupValueForField(targetField);
            values[i][2] = actualValues.get(i);
        }
        return Arrays.asList(values);
    }

    //Look up the target fields value from a prior release
    private static Object lookupValueForField(String fieldName) {
        if (!mApiMethodsAndValues.containsKey(fieldName)) {
            return NOT_FROM_PRIOR_RELEASE;
        }
        return mApiMethodsAndValues.get(fieldName);
    }

    public BinderTransactionTest(String targetField, Integer expectedValue, Integer actualValue) {
        mField = targetField;
        mExpectedValue = expectedValue;
        mActualValue = actualValue;
    }

    @Test
    public void testBinderTransactionValidation() {
        System.out.print("Testing: " + mField);
        if (mExpectedValue == NOT_FROM_PRIOR_RELEASE) {
            //This is a new interface, no need to test against
            return;
        }
        try {
            assertEquals(mExpectedValue, mActualValue);
        } catch (AssertionError e) {
            throw new AssertionError("For the field " + mField + " expected value "
                    + mExpectedValue + " but got " + mActualValue);
        }
    }
}
