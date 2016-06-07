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

package org.cyanogenmod.cmsettings.tests;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;

import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import cyanogenmod.providers.CMSettings;
import org.cyanogenmod.cmsettings.CMDatabaseHelper;
import org.cyanogenmod.cmsettings.CMSettingsProvider;
import org.cyanogenmod.cmsettings.R;

/**
 * Created by adnan on 1/25/16.
 */
public class CMSettingsProviderDefaultsTest extends AndroidTestCase {
    private ContentResolver mContentResolver;
    private boolean mHasMigratedSettings;
    private Resources mRemoteResources;

    // These data structures are set up in a way that is easier for manual input of new defaults
    private static ArrayList<Setting> SYSTEM_SETTINGS_DEFAULTS = new ArrayList<Setting>();
    private static ArrayList<Setting> SECURE_SETTINGS_DEFAULTS = new ArrayList<Setting>();
    private static ArrayList<Setting> GLOBAL_SETTINGS_DEFAULTS = new ArrayList<Setting>();

    //SYSTEM
    static {
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                "R.integer.def_qs_quick_pulldown"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                "R.integer.def_notification_brightness_level"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.ENABLE_PEOPLE_LOOKUP,
                "R.integer.def_people_lookup"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                "R.bool.def_notification_multiple_leds"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.SYSTEM_PROFILES_ENABLED,
                "R.bool.def_profiles_enabled"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                "R.bool.def_notification_pulse_custom_enable"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                "R.bool.def_swap_volume_keys_on_rotation"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                "R.string.def_notification_pulse_custom_value"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.STATUS_BAR_BATTERY_STYLE,
                "R.integer.def_battery_style"));
    }

    //SECURE
    static {
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.ADVANCED_MODE,
                "R.bool.def_advanced_mode"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.QS_USE_MAIN_TILES,
                "R.bool.def_sysui_qs_main_tiles"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.STATS_COLLECTION,
                "R.bool.def_stats_collection"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.LOCKSCREEN_VISUALIZER_ENABLED,
                "R.bool.def_lockscreen_visualizer"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.DEFAULT_THEME_COMPONENTS,
                "R.string.def_theme_components"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.DEFAULT_THEME_PACKAGE,
                "R.string.def_theme_package"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.PROTECTED_COMPONENT_MANAGERS,
                "R.string.def_protected_component_managers"));
    }

    //GLOBAL
    static {
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.POWER_NOTIFICATIONS_ENABLED,
                "R.bool.def_power_notifications_enabled"));
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.POWER_NOTIFICATIONS_VIBRATE,
                "R.bool.def_power_notifications_vibrate"));
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.POWER_NOTIFICATIONS_RINGTONE,
                "R.string.def_power_notifications_ringtone"));
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.WEATHER_TEMPERATURE_UNIT,
                "R.integer.def_temperature_unit"));
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.DEV_FORCE_SHOW_NAVBAR,
                "R.integer.def_force_show_navbar"));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getContext().getContentResolver();
        mHasMigratedSettings = getContext().getSharedPreferences(CMSettingsProvider.TAG,
                Context.MODE_PRIVATE).getBoolean(CMSettingsProvider.PREF_HAS_MIGRATED_CM_SETTINGS,
                false);
        mRemoteResources = getRemoteResources("org.cyanogenmod.cmsettings");
    }

    @SmallTest
    public void testVerifySystemSettingsDefault() {
        if (verifyNotMigratedSettings()) {
            for (Setting setting : SYSTEM_SETTINGS_DEFAULTS) {
                verifyDefaultSettingForTable(setting, CMDatabaseHelper.CMTableNames.TABLE_SYSTEM);
            }
        }
    }

    @SmallTest
    public void testVerifySecureSettingsDefaults() {
        if (verifyNotMigratedSettings()) {
            for (Setting setting : SECURE_SETTINGS_DEFAULTS) {
                verifyDefaultSettingForTable(setting, CMDatabaseHelper.CMTableNames.TABLE_SECURE);
            }
        }
    }

    @SmallTest
    public void testVerifyGlobalSettingsDefaults() {
        if (verifyNotMigratedSettings()) {
            for (Setting setting : GLOBAL_SETTINGS_DEFAULTS) {
                verifyDefaultSettingForTable(setting, CMDatabaseHelper.CMTableNames.TABLE_GLOBAL);
            }
        }
    }

    private boolean verifyNotMigratedSettings() {
        return !mHasMigratedSettings;
    }

    private void verifyDefaultSettingForTable(Setting setting, String table) {
        TypedValue value = new TypedValue();
        try {
            int identifier = mRemoteResources.getIdentifier(
                    setting.mDefResName, setting.mType, "org.cyanogenmod.cmsettings");
            mRemoteResources.getValue(identifier, value, true);
        } catch (Resources.NotFoundException e) {
            // Resource not found, can't verify because it probably wasn't loaded in
            throw new AssertionError("Unable to find resource for " + setting.mKey);
        }

        try {
            switch (value.type) {
                case TypedValue.TYPE_INT_DEC:
                    int actualValue = getIntForTable(setting, table);
                    try {
                        assertEquals(value.data, actualValue);
                    } catch (AssertionError e) {
                        throw new AssertionError("Compared value of " + setting.mKey + " expected "
                                + value.data + " got " + actualValue);
                    }
                    break;
                case TypedValue.TYPE_INT_BOOLEAN:
                    int actualBooleanValue = getIntForTable(setting, table);
                    try {
                        //This is gross
                        //Boolean can be "true" as long as it isn't 0
                        if (value.data != 0) {
                            value.data = 1;
                        }
                        assertEquals(value.data, actualBooleanValue);
                    } catch (AssertionError e) {
                        throw new AssertionError("Compared value of " + setting.mKey + " expected "
                                + value.data + " got " + actualBooleanValue);
                    }
                    break;
                case TypedValue.TYPE_STRING:
                    if (!TextUtils.isEmpty(value.string)) {
                        //This should really be done as a parameterized test
                        String actualStringValue = getStringForTable(setting, table);
                        try {
                            assertEquals(value.string, actualStringValue);
                        } catch (AssertionError e) {
                            throw new AssertionError("Compared value of " + setting.mKey
                                    + " expected " + value.string + " got " + actualStringValue);
                        }
                    }
                    break;
                case TypedValue.TYPE_NULL:
                    break;
            }
        } catch (CMSettings.CMSettingNotFoundException e) {
            e.printStackTrace();
            throw new AssertionError("Setting " + setting.mKey + " not found!");
        }
    }

    private int getIntForTable(Setting setting, String table)
            throws CMSettings.CMSettingNotFoundException {
        switch (table) {
            case CMDatabaseHelper.CMTableNames.TABLE_SYSTEM:
                return CMSettings.System.getIntForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_OWNER);
            case CMDatabaseHelper.CMTableNames.TABLE_SECURE:
                return CMSettings.Secure.getIntForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_OWNER);
            case CMDatabaseHelper.CMTableNames.TABLE_GLOBAL:
                return CMSettings.Global.getIntForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_OWNER);
            default:
                throw new AssertionError("Invalid or empty table!");
        }
    }

    private String getStringForTable(Setting setting, String table)
            throws CMSettings.CMSettingNotFoundException {
        switch (table) {
            case CMDatabaseHelper.CMTableNames.TABLE_SYSTEM:
                return CMSettings.System.getStringForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_OWNER);
            case CMDatabaseHelper.CMTableNames.TABLE_SECURE:
                return CMSettings.Secure.getStringForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_OWNER);
            case CMDatabaseHelper.CMTableNames.TABLE_GLOBAL:
                return CMSettings.Global.getStringForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_OWNER);
            default:
                throw new AssertionError("Invalid or empty table!");
        }
    }

    private static class Setting {
        public String mKey;
        public String mDefResName;
        public String mType;

        public Setting(String key, String defResId) {
            mKey = key;
            String[] parts = defResId.split("\\.");
            mType = parts[1];
            mDefResName = parts[2];
        }
    }

    private Resources getRemoteResources(String packageName)
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mContext.getPackageManager();
        return packageManager.getResourcesForApplication(packageName);
    }
}
