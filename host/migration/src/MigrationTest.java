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
    private static final boolean DEBUG = false;

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
        QueryWithTimeout cmSystemQuery =
                new QueryWithTimeout(CMSETTINGS_AUTHORITY, SYSTEM, cmSystemSettingList);
        QueryWithTimeout cmSecureQuery =
                new QueryWithTimeout(CMSETTINGS_AUTHORITY, SECURE, cmSecureSettingList);
        QueryWithTimeout cmGlobalQuery =
                new QueryWithTimeout(CMSETTINGS_AUTHORITY, GLOBAL, cmGlobalSettingList);
        QueryWithTimeout androidSystemQuery =
                new QueryWithTimeout(SETTINGS_AUTHORITY, SYSTEM, androidSystemSettingList);
        QueryWithTimeout androidSecureQuery =
                new QueryWithTimeout(SETTINGS_AUTHORITY, SECURE, androidSecureSettingList);
        QueryWithTimeout androidGlobalQuery =
                new QueryWithTimeout(SETTINGS_AUTHORITY, GLOBAL, androidGlobalSettingList);

        // CyanogenMod Queries
        cmSystemQuery.run();
        cmSecureQuery.run();
        cmGlobalQuery.run();

        // Android Queries
        androidSystemQuery.run();
        androidSecureQuery.run();
        androidGlobalQuery.run();

        if (DEBUG) {
            iterateAndPrint(CMSETTINGS_AUTHORITY, SYSTEM, cmSystemSettingList);
            iterateAndPrint(CMSETTINGS_AUTHORITY, SECURE, cmSecureSettingList);
            iterateAndPrint(CMSETTINGS_AUTHORITY, GLOBAL, cmGlobalSettingList);
            iterateAndPrint(SETTINGS_AUTHORITY, SYSTEM, androidSystemSettingList);
            iterateAndPrint(SETTINGS_AUTHORITY, SECURE, androidSecureSettingList);
            iterateAndPrint(SETTINGS_AUTHORITY, GLOBAL, androidGlobalSettingList);
        }

        for (Setting setting: androidSystemSettingList) {
            new InsertWithTimeout(CMSETTINGS_AUTHORITY, SYSTEM, setting).run();
        }

        QueryWithTimeout cmSystemReQuery =
                new QueryWithTimeout(CMSETTINGS_AUTHORITY, SYSTEM, cmSystemSettingList);
        cmSystemReQuery.run();
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