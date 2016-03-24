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
import cyanogenmod.themes.IThemeService;
import cyanogenmod.themes.ThemeManager;

import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.ThemesContract;

/**
 * Created by adnan on 3/23/16.
 */
public class ThemeManagerTest extends AndroidTestCase {
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
        // Get the default theme package
        final String defaultThemePkg = CMSettings.Secure.getString(mContentResolver,
                CMSettings.Secure.DEFAULT_THEME_PACKAGE);

        // Apply the default theme
        mThemeManager.applyDefaultTheme();

        // Iterate over all the rows in the ThemesContract's MixnMatch table
        // to verify that the package corresponds to what is expected from the default
        // theme being applied.
        for (String key : ThemesContract.MixnMatchColumns.ROWS) {
            assertTrue(verifyThemeAppliedFromPackageForRow(defaultThemePkg, key));
        }
    }

    /**
     * Verify the key from the MixNMatch table via the ThemesProvider
     * @param packageName
     * @return Whether or not the theme is applied correctly per the ThemesProvider
     */
    private boolean verifyThemeAppliedFromPackageForRow(String packageName, String key) {
        return TextUtils.equals(packageName, getPackageNameForKey(mContext, key));
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
