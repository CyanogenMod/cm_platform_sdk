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

package org.cyanogenmod.internal.logging;

import com.android.internal.logging.MetricsLogger;

/**
 * Serves as a central location for logging constants that is android release agnostic.
 */
public class CMMetricsLogger extends MetricsLogger {
    private static final int BASE = -Integer.MAX_VALUE;
    //Since we never want to collide, lets start at the back and move inward
    public static final int DONT_LOG = BASE + 1;

    public static final int ANONYMOUS_STATS = BASE + 2;
    public static final int APP_GROUP_CONFIG = BASE + 3;
    public static final int APP_GROUP_LIST = BASE + 4;
    public static final int BATTERY_LIGHT_SETTINGS = BASE + 5;
    public static final int BUTTON_SETTINGS = BASE + 6;
    public static final int CHOOSE_LOCK_PATTERN_SIZE = BASE + 7;
    public static final int DISPLAY_ROTATION = BASE + 8;
    public static final int LIVE_DISPLAY = BASE + 9;
    public static final int NOTIFICATION_LIGHT_SETTINGS = BASE + 10;
    public static final int NOTIFICATION_MANAGER_SETTINGS = BASE + 11;
    public static final int POWER_MENU_ACTIONS = BASE + 12;
    public static final int PREVIEW_DATA = BASE + 13;
    public static final int PRIVACY_GUARD_PREFS = BASE + 14;
    public static final int PRIVACY_SETTINGS = BASE + 15;
    public static final int PROFILE_GROUP_CONFIG = BASE + 16;
    public static final int PROFILES_SETTINGS = BASE + 17;
    public static final int SETUP_ACTIONS_FRAGMENT = BASE + 18;
    public static final int SETUP_TRIGGERS_FRAGMENT = BASE + 19;
    public static final int STYLUS_GESTURES = BASE + 20;
    public static final int TILE_ADB_OVER_NETWORK = BASE + 21;
    public static final int TILE_AMBIENT_DISPLAY = BASE + 22;
    public static final int TILE_COMPASS = BASE + 23;
    public static final int TILE_CUSTOM_QS = BASE + 24;
    public static final int TILE_CUSTOM_QS_DETAIL = BASE + 25;
    public static final int TILE_EDIT = BASE + 26;
    public static final int TILE_LIVE_DISPLAY = BASE + 27;
    public static final int TILE_LOCKSCREEN_TOGGLE = BASE + 28;
    public static final int TILE_NFC = BASE + 29;
    public static final int TILE_PERF_PROFILE = BASE + 30;
    public static final int TILE_PERF_PROFILE_DETAIL = BASE + 31;
    public static final int TILE_PROFILES = BASE + 32;
    public static final int TILE_PROFILES_DETAIL = BASE + 33;
    public static final int TILE_SCREEN_TIME_OUT = BASE + 34;
    public static final int TILE_SCREEN_TIME_OUT_DETAIL = BASE + 35;
    public static final int TILE_SYNC = BASE + 36;
    public static final int TILE_USB_TETHER = BASE + 37;
    public static final int TILE_VOLUME = BASE + 38;
    public static final int TILE_HEADS_UP = BASE + 39;
    public static final int TILE_BATTERY_SAVER = BASE + 40;
    public static final int TILE_CAFFEINE = BASE + 41;
    public static final int WEATHER_SETTINGS = BASE + 42;
    public static final int TILE_THEMES = BASE + 43;
}
