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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.providers.ThemesContract;

public class ThemesContractTests extends AndroidTestCase {
    private static final String TAG = ThemesContract.class.getSimpleName();

    public static final String[] COMPONENTS = {
            ThemesContract.ThemesColumns.MODIFIES_LAUNCHER,
            ThemesContract.ThemesColumns.MODIFIES_LOCKSCREEN,
            ThemesContract.ThemesColumns.MODIFIES_ICONS,
            ThemesContract.ThemesColumns.MODIFIES_STATUS_BAR,
            ThemesContract.ThemesColumns.MODIFIES_BOOT_ANIM,
            ThemesContract.ThemesColumns.MODIFIES_FONTS,
            ThemesContract.ThemesColumns.MODIFIES_NOTIFICATIONS,
            ThemesContract.ThemesColumns.MODIFIES_RINGTONES,
            ThemesContract.ThemesColumns.MODIFIES_ALARMS,
            ThemesContract.ThemesColumns.MODIFIES_OVERLAYS,
            ThemesContract.ThemesColumns.MODIFIES_NAVIGATION_BAR,
            ThemesContract.ThemesColumns.MODIFIES_LIVE_LOCK_SCREEN
    };

    public static final String[] URIS = {
            ThemesContract.ThemesColumns.HOMESCREEN_URI,
            ThemesContract.ThemesColumns.LOCKSCREEN_URI,
            ThemesContract.ThemesColumns.ICON_URI,
            ThemesContract.ThemesColumns.STATUSBAR_URI,
            ThemesContract.ThemesColumns.BOOT_ANIM_URI,
            ThemesContract.ThemesColumns.FONT_URI,
            null,
            null,
            null,
            ThemesContract.ThemesColumns.OVERLAYS_URI,
            null,
            null,
    };

    @SmallTest
    public void testComponentToImageColNameTransformation() {
        for (int i = 0; i < ThemesContract.MixnMatchColumns.ROWS.length; i++) {
            if (URIS[i] != null) {
                assertEquals(URIS[i],
                        ThemesContract.MixnMatchColumns.componentToImageColName(
                                ThemesContract.MixnMatchColumns.ROWS[i]));
            } else {
                try {
                    ThemesContract.MixnMatchColumns.componentToImageColName(
                            ThemesContract.MixnMatchColumns.ROWS[i]);
                    throw new AssertionError("Key " + ThemesContract.MixnMatchColumns.ROWS[i]
                            + " should throw IllegalArgumentException");
                } catch (IllegalArgumentException e) {
                    // Expected
                }
            }
        }
    }

    @SmallTest
    public void testComponentToMixNMatchKeyTransformation() {
        for (int i = 0; i < COMPONENTS.length; i++) {
            assertEquals(ThemesContract.MixnMatchColumns.componentToMixNMatchKey(
                    COMPONENTS[i]) , ThemesContract.MixnMatchColumns.ROWS[i]);
        }
    }

    @SmallTest
    public void testmixNMatchKeyToComponentTransformation() {
        for (int i = 0; i < ThemesContract.MixnMatchColumns.ROWS.length; i++) {
            assertEquals(ThemesContract.MixnMatchColumns.mixNMatchKeyToComponent(
                    ThemesContract.MixnMatchColumns.ROWS[i]) , COMPONENTS[i]);
        }
    }
}
