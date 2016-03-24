/**
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.tests.themes.unit;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import android.util.Log;

import cyanogenmod.themes.IThemeService;
import cyanogenmod.themes.ThemeManager;
import cyanogenmod.themes.ThemeManager.ThemeChangeListener;

import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.ThemesContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by adnan on 3/23/16.
 */
public class ThemeManagerTest extends AndroidTestCase {
    private static final String TAG = ThemeManagerTest.class.getSimpleName();
    private static final int COUNTDOWN = 1;

    private ThemeManager mThemeManager;
    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mThemeManager = ThemeManager.getInstance(getContext());
        mContentResolver = mContext.getContentResolver();
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mThemeManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        IThemeService icmStatusBarManager = mThemeManager.getService();
        assertNotNull(icmStatusBarManager);
    }

    @SmallTest
    public void testApplyDefaultTheme() {
        final HashMap<String, String> componentKeyMap = new HashMap<>();
        final CountDownLatch signal = new CountDownLatch(COUNTDOWN);

        // Get the default theme package
        final String defaultThemePkg = CMSettings.Secure.getString(mContentResolver,
                CMSettings.Secure.DEFAULT_THEME_PACKAGE);

        // Get the deefault theme components
        final ArrayList<String> components = new ArrayList<>(
                Arrays.asList(CMSettings.Secure.getString(mContentResolver,
                        CMSettings.Secure.DEFAULT_THEME_COMPONENTS).split("\\|")));

        // Populate componentkey map since we're going to lock the thread
        for (String component : components) {
            String key = ThemesContract.MixnMatchColumns.componentToMixNMatchKey(component);
            componentKeyMap.put(key, getPackageNameForKey(mContext, key));
        }

        // Register defaultThemeChangeListener
        mThemeManager.registerThemeChangeListener(new ThemeChangeListener() {
            public void onProgress(int progress) {

            }

            public void onFinish(boolean isSuccess) {
                boolean assertionFailure = false;
                if (isSuccess) {
                    for (String component : components) {
                        String key = ThemesContract.MixnMatchColumns.
                                componentToMixNMatchKey(component);
                        Log.d(TAG, "Verifying row " + key);
                        if (!verifyThemeAppliedFromPackageForRow(defaultThemePkg,
                                componentKeyMap.get(key), true)) {
                            Log.d(TAG, "Expected package " + defaultThemePkg
                                    + " but got package " + componentKeyMap.get(key));
                            assertionFailure = true;
                        }
                    }
                }
                mThemeManager.unregisterThemeChangeListener(this);
                signal.countDown();
                if (assertionFailure) throw new AssertionError("Unable to apply default theme");
            }
        });

        // Apply the default theme
        mThemeManager.applyDefaultTheme();

        // Lock
        try {
            signal.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private boolean verifyThemeAppliedFromPackageForRow(String packageName, String expectedPackage,
            boolean systemTheme) {
        boolean verified = TextUtils.isEmpty(expectedPackage) ||
                TextUtils.equals(packageName, expectedPackage);

        if (systemTheme && !verified) {
            verified = TextUtils.equals(expectedPackage, "system");
        }

        return verified;
    }

    private String getPackageNameForKey(Context context, String key) {
        final ContentResolver cr = context.getContentResolver();
        String[] projection = {ThemesContract.MixnMatchColumns.COL_VALUE};
        String selection = ThemesContract.MixnMatchColumns.COL_KEY + "=?";
        String[] selectionArgs = {key};
        Cursor c = cr.query(ThemesContract.MixnMatchColumns.CONTENT_URI, projection, selection,
                selectionArgs, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return c.getString(0);
                }
            } finally {
                c.close();
            }
        }
        return null;
    }

}
