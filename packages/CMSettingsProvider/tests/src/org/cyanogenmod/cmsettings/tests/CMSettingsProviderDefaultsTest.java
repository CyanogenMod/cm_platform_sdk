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
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;

import android.util.TypedValue;
import cyanogenmod.providers.CMSettings;
import org.cyanogenmod.cmsettings.CMDatabaseHelper;
import org.cyanogenmod.cmsettings.R;

/**
 * Created by adnan on 1/25/16.
 */
public class CMSettingsProviderDefaultsTest extends AndroidTestCase {
    private ContentResolver mContentResolver;
    private boolean mHasMigratedSettings;

    // These data structures are set up in a way that is easier for manual input of new defaults
    private static ArrayList<Setting> SYSTEM_SETTINGS_DEFAULTS = new ArrayList<Setting>();
    private static ArrayList<Setting> SECURE_SETTINGS_DEFAULTS = new ArrayList<Setting>();
    private static ArrayList<Setting> GLOBAL_SETTINGS_DEFAULTS = new ArrayList<Setting>();

    //SYSTEM
    static {
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                R.integer.def_qs_quick_pulldown);
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                R.integer.def_notification_brightness_level));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.PEOPLE_LOOKUP_PROVIDER,
                R.integer.def_people_lookup);
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                R.bool.def_notification_multiple_leds));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.SYSTEM_PROFILES_ENABLED,
                R.bool.def_profiles_enabled));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                R.bool.def_notification_pulse_custom_enable));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                R.bool.def_swap_volume_keys_on_rotation));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                R.string.def_notification_pulse_custom_value));
    }

    //SECURE
    static {
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR,
                R.integer.def_force_show_navbar));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.ADVANCED_MODE,
                R.bool.def_advanced_mode));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.QS_USE_MAIN_TILES,
                R.bool.def_sysui_qs_main_tiles));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.STATS_COLLECTION,
                R.bool.def_stats_collection));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.LOCKSCREEN_VISUALIZER_ENABLED,
                R.bool.def_lockscreen_visualizer));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.DEFAULT_THEME_COMPONENTS,
                R.string.def_theme_components));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.DEFAULT_THEME_PACKAGE,
                R.string.def_theme_package));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.QS_TILES,
                R.string.def_qs_tiles));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Secure.PROTECTED_COMPONENT_MANAGERS,
                R.string.def_protected_component_managers));
    }

    //GLOBAL
    static {
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.POWER_NOTIFICATIONS_ENABLED,
                R.bool.def_power_notifications_enabled));
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.POWER_NOTIFICATIONS_VIBRATE,
                R.bool.def_power_notifications_vibrate));
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                CMSettings.Global.POWER_NOTIFICATIONS_RINGTONE,
                R.string.def_power_notifications_ringtone));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = mContext.getContentResolver();
        mHasMigratedSettings = mContext.getSharedPreferences("has_migrated_cm13_settings",
                Context.MODE_PRIVATE) != null;
    }

    @SmallTest
    public void testVerifySystemSettingsDefault() {
        verifyNotMigratedSettings();

        for (Setting setting : SYSTEM_SETTINGS_DEFAULTS) {
            verifyDefaultSettingForTable(setting, CMDatabaseHelper.CMTableNames.SYSTEM);
        }
    }

    @SmallTest
    public void testVerifySecureSettingsDefaults() {
        verifyNotMigratedSettings();

        for (Setting setting : SECURE_SETTINGS_DEFAULTS) {
            verifyDefaultSettingForTable(setting, CMDatabaseHelper.CMTableNames.SYSTEM);
        }
    }

    @SmallTest
    public void testVerifyGlobalSettingsDefaults() {
        verifyNotMigratedSettings();

        for (Setting setting : GLOBAL_SETTINGS_DEFAULTS) {
            verifyDefaultSettingForTable(setting, CMDatabaseHelper.CMTableNames.SYSTEM);
        }
    }

    private void verifyNotMigratedSettings() {
        if (mHasMigratedSettings) {
            throw new AssertionError("CMSettingsProvider contains migrated settings, " +
                    "please only run on clean flash device");
        }
    }

    public void verifyDefaultSettingForTable(Setting setting, String table) {
        TypedValue value = new TypedValue();
        mContext.getResources().getValue(setting.mDefResId, value, true);

        try {
            switch (value.type) {
                case TypedValue.TYPE_INT_DEC:
                case TypedValue.TYPE_INT_BOOLEAN:
                    assertEquals(value.data, getIntForTable(setting, table));
                    break;
                case TypedValue.TYPE_STRING:
                    assertEquals(value.string, getStringForTable(setting, table));
                    break;
            }
        } catch (CMSettings.CMSettingNotFoundException e) {
            throw new AssertionError("Setting " + setting.mKey + " not found!");
        }
    }

    public int getIntForTable(Setting setting, String table)
            throws CMSettings.CMSettingNotFoundException {
        switch (table) {
            case CMDatabaseHelper.CMTableNames.SYSTEM:
                return CMSettings.System.getInt(mContentResolver, setting.mKey);
            case CMDatabaseHelper.CMTableNames.SECURE:
                return CMSettings.Secure.getInt(mContentResolver, setting.mKey);
            case CMDatabaseHelper.CMTableNames.GLOBAL:
                return CMSettings.Global.getInt(mContentResolver, setting.mKey);
            default:
                throw new AssertionError("Invalid or empty table!");
        }
    }

    public String getStringForTable(Setting setting, String table)
            throws CMSettings.CMSettingNotFoundException {
        switch (table) {
            case CMDatabaseHelper.CMTableNames.SYSTEM:
                return CMSettings.System.getString(mContentResolver, setting.mKey);
            case CMDatabaseHelper.CMTableNames.SECURE:
                return CMSettings.Secure.getString(mContentResolver, setting.mKey);
            case CMDatabaseHelper.CMTableNames.GLOBAL:
                return CMSettings.Global.getString(mContentResolver, setting.mKey);
            default:
                throw new AssertionError("Invalid or empty table!");
        }
    }

    private static class Setting {
        public String mKey;
        public int mDefResId;

        public Setting(String key, int defResId) {
            mKey = key;
            mDefResId = defResId;
        }
    }
}
