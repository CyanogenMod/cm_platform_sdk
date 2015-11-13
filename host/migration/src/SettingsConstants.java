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

import java.util.ArrayList;

/**
 * Created by adnan on 11/16/15.
 */
public class SettingsConstants {
    public static final String CMSETTINGS_AUTHORITY = "cmsettings";
    public static final String SETTINGS_AUTHORITY = "settings";
    public static final String CONTENT_URI = "content://";
    public static final String SYSTEM = "/system";
    public static final String SECURE = "/secure";
    public static final String GLOBAL = "/global";

    public static class Ignorables {
        public static ArrayList<String> SECURE_SETTINGS = new ArrayList<String>();

        static {
            SECURE_SETTINGS.add(CMSettings.Secure.ADB_PORT);
        }
    }
}
