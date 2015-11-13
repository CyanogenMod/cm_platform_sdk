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

    public static final String CMSETTINGS_AUTHORITY = "cmsettings";
    public static final String SETTINGS_AUTHORITY = "settings";
    public static final String CONTENT_URI = "content://";

    private static final String SYSTEM = "/system";
    private static final String SECURE = "/secure";
    private static final String GLOBAL = "/global";

    private static ArrayList<Setting> cmSystemSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> cmSecureSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> cmGlobalSettingList = new ArrayList<Setting>();

    private static ArrayList<Setting> androidSystemSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> androidSecureSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> androidGlobalSettingList = new ArrayList<Setting>();

    public static void main(String[] args) throws IOException {
        SettingImageCommands cmSettingImage = new SettingImageCommands(CMSETTINGS_AUTHORITY);
        cmSettingImage.addQuery(SYSTEM, cmSystemSettingList);
        cmSettingImage.addQuery(SECURE, cmSecureSettingList);
        cmSettingImage.addQuery(GLOBAL, cmGlobalSettingList);

        SettingImageCommands androidSettingImage = new SettingImageCommands(SETTINGS_AUTHORITY);
        androidSettingImage.addQuery(SYSTEM, androidSystemSettingList);
        androidSettingImage.addQuery(SECURE, androidSecureSettingList);
        androidSettingImage.addQuery(GLOBAL, androidGlobalSettingList);

        // CyanogenMod Queries
        cmSettingImage.execute();
        // Android Queries
        androidSettingImage.execute();

        if (DEBUG) {
            iterateAndPrint(CMSETTINGS_AUTHORITY, SYSTEM, cmSystemSettingList);
            iterateAndPrint(CMSETTINGS_AUTHORITY, SECURE, cmSecureSettingList);
            iterateAndPrint(CMSETTINGS_AUTHORITY, GLOBAL, cmGlobalSettingList);
            iterateAndPrint(SETTINGS_AUTHORITY, SYSTEM, androidSystemSettingList);
            iterateAndPrint(SETTINGS_AUTHORITY, SECURE, androidSecureSettingList);
            iterateAndPrint(SETTINGS_AUTHORITY, GLOBAL, androidGlobalSettingList);
        }

        // Write all the android system settings to cmsettings system table and requery
        SettingImageCommands cmSettingImageRequery = new SettingImageCommands(CMSETTINGS_AUTHORITY);
        for (Setting setting: androidSystemSettingList) {
            cmSettingImageRequery.addInsert(SYSTEM, setting);
        }
        cmSettingImageRequery.addQuery(SYSTEM, cmSystemSettingList);
        cmSettingImageRequery.execute();

        // Display
        iterateAndPrint(CMSETTINGS_AUTHORITY, SYSTEM, cmSystemSettingList);
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