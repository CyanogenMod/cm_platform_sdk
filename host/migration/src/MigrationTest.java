/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import java.io.IOException;
import java.util.ArrayList;

/**
 * A verbose settings migration test
 */
class MigrationTest {
    public static final boolean DEBUG = true;

    private static ArrayList<Setting> cmSystemSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> cmSecureSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> cmGlobalSettingList = new ArrayList<Setting>();

    private static ArrayList<Setting> androidSystemSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> androidSecureSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> androidGlobalSettingList = new ArrayList<Setting>();

    public static void main(String[] args) throws IOException {
        SettingImageCommands cmSettingImage =
                new SettingImageCommands(SettingsConstants.CMSETTINGS_AUTHORITY);
        cmSettingImage.addQuery(SettingsConstants.SYSTEM, cmSystemSettingList);
        cmSettingImage.addQuery(SettingsConstants.SECURE, cmSecureSettingList);
        cmSettingImage.addQuery(SettingsConstants.GLOBAL, cmGlobalSettingList);

        SettingImageCommands androidSettingImage =
                new SettingImageCommands(SettingsConstants.SETTINGS_AUTHORITY);
        androidSettingImage.addQuery(SettingsConstants.SYSTEM, androidSystemSettingList);
        androidSettingImage.addQuery(SettingsConstants.SECURE, androidSecureSettingList);
        androidSettingImage.addQuery(SettingsConstants.GLOBAL, androidGlobalSettingList);

        // CyanogenMod Queries
        cmSettingImage.execute();
        // Android Queries
        androidSettingImage.execute();

        // Write all the android system settings to cmsettings system table and requery
        SettingImageCommands cmSettingImageRequery =
                new SettingImageCommands(SettingsConstants.CMSETTINGS_AUTHORITY);
        for (Setting setting: androidSystemSettingList) {
            if (CMSettings.System.isLegacySetting(setting.getKey())) {
                cmSettingImageRequery.addInsert(SettingsConstants.SYSTEM, setting);
            }
        }
        cmSettingImageRequery.addQuery(SettingsConstants.SYSTEM, cmSystemSettingList);
        cmSettingImageRequery.execute();

        // Display
        iterateAndPrint(SettingsConstants.CMSETTINGS_AUTHORITY,
                SettingsConstants.SYSTEM, cmSystemSettingList);
    }

    private static void iterateAndPrint(String targetAuthority, String targetUri,
            ArrayList<Setting> settings) {
        System.out.println("\n\n\nShowing settings for authority "
                + targetAuthority + " for target uri " + targetUri);
        for (Setting setting : settings) {
            System.out.println("Setting key:" + setting.getKey()
                    + " value:" + setting.getValue() + " with type " + setting.getValueType());
        }
    }
}