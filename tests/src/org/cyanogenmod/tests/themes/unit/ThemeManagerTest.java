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
import android.content.pm.PackageManager;
import android.content.res.ThemeConfig;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import android.util.Log;

import cyanogenmod.themes.IThemeService;
import cyanogenmod.themes.ThemeChangeRequest;
import cyanogenmod.themes.ThemeManager;
import cyanogenmod.themes.ThemeManager.ThemeChangeListener;

import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.ThemesContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ThemeManagerTest extends AndroidTestCase {
    private static final String TAG = ThemeManagerTest.class.getSimpleName();
    private static final int COUNTDOWN = 1;

    private ThemeManager mThemeManager;

    private static final List<String> ALL_THEME_COMPONENTS = new ArrayList<>();

    static {
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_ALARMS);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_FONTS);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_ICONS);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_LAUNCHER);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_LOCKSCREEN);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_NAVIGATION_BAR);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_OVERLAYS);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_RINGTONES);
        ALL_THEME_COMPONENTS.add(ThemesContract.ThemesColumns.MODIFIES_STATUS_BAR);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mThemeManager = ThemeManager.getInstance(getContext());
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
        final String defaultThemePkg = getDefaultThemePackageName(mContext);

        // Get the default theme components
        final List<String> components = getSupportedComponentsForTheme(mContext, defaultThemePkg);

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

    private ThemeChangeListener dummyThemeChangeListener = new ThemeChangeListener() {
        @Override
        public void onProgress(int progress) {

        }

        @Override
        public void onFinish(boolean isSuccess) {

        }
    };

    @SmallTest
    public void testRegisterAndUnregisterThemeChangeListener() {
        // Exploit the illegalArgumentException thrown by registerThemeChangeListener to
        // verify registration.
        mThemeManager.registerThemeChangeListener(dummyThemeChangeListener);

        try {
            mThemeManager.registerThemeChangeListener(dummyThemeChangeListener);
            throw new AssertionError("Failed to register theme change listener!");
        } catch (IllegalArgumentException e) {
            // EXPECTED!
        }

        // Inversely, exploit that the illegal argument exception isn't thrown
        // if unregistering and reregistering
        mThemeManager.unregisterThemeChangeListener(dummyThemeChangeListener);

        try {
            mThemeManager.registerThemeChangeListener(dummyThemeChangeListener);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("Failed to unregister theme change listener!");
        }

        // Cleanup!
        mThemeManager.unregisterThemeChangeListener(dummyThemeChangeListener);
    }

    private ThemeManager.ThemeProcessingListener dummyThemeProcessingListener =
            new ThemeManager.ThemeProcessingListener() {
        @Override
        public void onFinishedProcessing(String pkgName) {

        }
    };

    @SmallTest
    public void testRegisterAndUnregisterThemeProcessingListener() {
        // Exploit the illegalArgumentException thrown by registerThemeChangeListener to
        // verify registration.
        mThemeManager.registerProcessingListener(dummyThemeProcessingListener);

        try {
            mThemeManager.registerProcessingListener(dummyThemeProcessingListener);
            throw new AssertionError("Failed to register theme processing listener!");
        } catch (IllegalArgumentException e) {
            // EXPECTED!
        }

        // Inversely, exploit that the illegal argument exception isn't thrown
        // if unregistering and reregistering
        mThemeManager.unregisterProcessingListener(dummyThemeProcessingListener);

        try {
            mThemeManager.registerProcessingListener(dummyThemeProcessingListener);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("Failed to unregister theme change listener!");
        }

        // Cleanup!
        mThemeManager.unregisterProcessingListener(dummyThemeProcessingListener);
    }

    boolean actualRequestThemeChangeAsMapResponse = false;
    @SmallTest
    public void testRequestThemeChangeAsMapAndCallback() {
        Map<String, String> expectedAppOverlayMap = new HashMap<>();
        final CountDownLatch signal = new CountDownLatch(COUNTDOWN);

        // Get the default theme package
        final String defaultThemePkg = getDefaultThemePackageName(mContext);

        // Get the default theme components
        final List<String> components = getSupportedComponentsForTheme(mContext, defaultThemePkg);

        for (String component : components) {
            expectedAppOverlayMap.put(component, defaultThemePkg);
        }

        mThemeManager.registerThemeChangeListener(new ThemeChangeListener() {
            @Override
            public void onProgress(int progress) {

            }

            @Override
            public void onFinish(boolean isSuccess) {
                actualRequestThemeChangeAsMapResponse = isSuccess;
                signal.countDown();
            }
        });

        mThemeManager.requestThemeChange(expectedAppOverlayMap);

        // Lock
        try {
            signal.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

        assertTrue(actualRequestThemeChangeAsMapResponse);
    }

    boolean actualRequestThemeChangeAsStringListResponse = false;
    @SmallTest
    public void testRequestThemeChangeAsStringListAndCallback() {
        final CountDownLatch signal = new CountDownLatch(COUNTDOWN);

        // Get the default theme package
        final String defaultThemePkg = getDefaultThemePackageName(mContext);

        // Get the default theme components
        final List<String> components = getSupportedComponentsForTheme(mContext, defaultThemePkg);

        mThemeManager.registerThemeChangeListener(new ThemeChangeListener() {
            @Override
            public void onProgress(int progress) {

            }

            @Override
            public void onFinish(boolean isSuccess) {
                actualRequestThemeChangeAsStringListResponse = isSuccess;
                signal.countDown();
            }
        });

        mThemeManager.requestThemeChange(defaultThemePkg, components);

        // Lock
        try {
            signal.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

        assertTrue(actualRequestThemeChangeAsStringListResponse);
    }

    boolean actualRequestThemeChangeAsRequestResponse = false;
    @SmallTest
    public void testRequestThemeChangeAsRequestAndCallback() {
        final CountDownLatch signal = new CountDownLatch(COUNTDOWN);

        // Get the default theme package
        final String defaultThemePkg = getDefaultThemePackageName(mContext);

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setAlarm(defaultThemePkg)
                .setNavBar(defaultThemePkg)
                .setBootanimation(defaultThemePkg)
                .setLockWallpaper(defaultThemePkg)
                .setLiveLockScreen(defaultThemePkg)
                .build();

        mThemeManager.registerThemeChangeListener(new ThemeChangeListener() {
            @Override
            public void onProgress(int progress) {

            }

            @Override
            public void onFinish(boolean isSuccess) {
                actualRequestThemeChangeAsRequestResponse = isSuccess;
                signal.countDown();
            }
        });

        mThemeManager.requestThemeChange(request, true);

        // Lock
        try {
            signal.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

        assertTrue(actualRequestThemeChangeAsRequestResponse);
    }

    private boolean verifyThemeAppliedFromPackageForRow(String packageName, String expectedPackage,
            boolean systemTheme) {
        boolean verified = TextUtils.isEmpty(expectedPackage) ||
                TextUtils.equals(packageName, expectedPackage);

        if (systemTheme && !verified) {
            verified = TextUtils.equals(expectedPackage, ThemeConfig.SYSTEM_DEFAULT);
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

    private static String getDefaultThemePackageName(Context context) {
        final String defaultThemePkg = CMSettings.Secure.getString(context.getContentResolver(),
                CMSettings.Secure.DEFAULT_THEME_PACKAGE);
        if (!TextUtils.isEmpty(defaultThemePkg)) {
            PackageManager pm = context.getPackageManager();
            try {
                if (pm.getPackageInfo(defaultThemePkg, 0) != null) {
                    return defaultThemePkg;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // doesn't exist so system will be default
                Log.w(TAG, "Default theme " + defaultThemePkg + " not found", e);
            }
        }

        return ThemeConfig.SYSTEM_DEFAULT;
    }

    private static List<String> getSupportedComponentsForTheme(Context context,
            String themePkgName) {
        List<String> supportedComponents = new ArrayList<>();

        String selection = ThemesContract.ThemesColumns.PKG_NAME + "= ?";
        String[] selectionArgs = new String[]{ themePkgName };
        Cursor c = context.getContentResolver().query(ThemesContract.ThemesColumns.CONTENT_URI,
                null, selection, selectionArgs, null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    for (String component : ALL_THEME_COMPONENTS) {
                        int index = c.getColumnIndex(component);
                        if (c.getInt(index) == 1) {
                            supportedComponents.add(component);
                        }
                    }
                }
            } finally {
                c.close();
            }
        }
        return supportedComponents;
    }
}
