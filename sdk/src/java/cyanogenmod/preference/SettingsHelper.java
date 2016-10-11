/*
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
package cyanogenmod.preference;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

import cyanogenmod.providers.CMSettings;

public class SettingsHelper {

    private static final String SETTINGS_GLOBAL = Settings.Global.CONTENT_URI.toString();
    private static final String SETTINGS_SECURE = Settings.Secure.CONTENT_URI.toString();
    private static final String SETTINGS_SYSTEM = Settings.System.CONTENT_URI.toString();

    private static final String CMSETTINGS_GLOBAL = CMSettings.Global.CONTENT_URI.toString();
    private static final String CMSETTINGS_SECURE = CMSettings.Secure.CONTENT_URI.toString();
    private static final String CMSETTINGS_SYSTEM = CMSettings.System.CONTENT_URI.toString();

    public static String getString(Context context, Uri settingsUri) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = context.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            return Settings.Secure.getString(resolver, uri.substring(SETTINGS_SECURE.length()));
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            return Settings.System.getString(resolver, uri.substring(SETTINGS_SYSTEM.length()));
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            return Settings.Global.getString(resolver, uri.substring(SETTINGS_GLOBAL.length()));
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            return CMSettings.Secure.getString(resolver, uri.substring(CMSETTINGS_SECURE.length()));
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            return CMSettings.System.getString(resolver, uri.substring(CMSETTINGS_SYSTEM.length()));
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            return CMSettings.Global.getString(resolver, uri.substring(CMSETTINGS_GLOBAL.length()));
        }
        return null;
    }

    public static int getInt(Context context, Uri settingsUri, int def) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = context.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            return Settings.Secure.getInt(resolver, uri.substring(SETTINGS_SECURE.length()), def);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            return Settings.System.getInt(resolver, uri.substring(SETTINGS_SYSTEM.length()), def);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            return Settings.Global.getInt(resolver, uri.substring(SETTINGS_GLOBAL.length()), def);
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            return CMSettings.Secure.getInt(resolver, uri.substring(CMSETTINGS_SECURE.length()), def);
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            return CMSettings.System.getInt(resolver, uri.substring(CMSETTINGS_SYSTEM.length()), def);
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            return CMSettings.Global.getInt(resolver, uri.substring(CMSETTINGS_GLOBAL.length()), def);
        }
        return def;
    }

    public static boolean getBoolean(Context context, Uri settingsUri, boolean def) {
        int value = getInt(context, settingsUri, def ? 1 : 0);
        return value == 1;
    }

    public static void putString(Context context, Uri settingsUri, String value) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = context.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            Settings.Secure.putString(resolver, uri.substring(SETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            Settings.System.putString(resolver, uri.substring(SETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            Settings.Global.putString(resolver, uri.substring(SETTINGS_GLOBAL.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            CMSettings.Secure.putString(resolver, uri.substring(CMSETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            CMSettings.System.putString(resolver, uri.substring(CMSETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            CMSettings.Global.putString(resolver, uri.substring(CMSETTINGS_GLOBAL.length()), value);
        }
    }

    public static void putInt(Context context, Uri settingsUri, int value) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = context.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            Settings.Secure.putInt(resolver, uri.substring(SETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            Settings.System.putInt(resolver, uri.substring(SETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            Settings.Global.putInt(resolver, uri.substring(SETTINGS_GLOBAL.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            CMSettings.Secure.putInt(resolver, uri.substring(CMSETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            CMSettings.System.putInt(resolver, uri.substring(CMSETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            CMSettings.Global.putInt(resolver, uri.substring(CMSETTINGS_GLOBAL.length()), value);
        }
    }

    public static void putBoolean(Context context, Uri settingsUri, boolean value) {
        putInt(context, settingsUri, value ? 1 : 0);
    }
}
