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

package cyanogenmod.os;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.SparseArray;

/**
 * Information about the current CyanogenMod build, extracted from system properties.
 */
public class Build {
    /** Value used for when a build property is unknown. */
    public static final String UNKNOWN = "unknown";

    /** A build ID utilized to distinguish cyanogenmod versions */
    public static final String CYANOGENMOD_VERSION = "ro.cm.version";

    /** A build ID string meant for displaying to the user */
    public static final String CYANOGENMOD_DISPLAY_VERSION = "ro.cm.display.version";

    private static final SparseArray<String> sdkMap;
    static
    {
        sdkMap = new SparseArray<String>();
        sdkMap.put(CM_VERSION_CODES.APRICOT, "Apricot");
        sdkMap.put(CM_VERSION_CODES.BOYSENBERRY, "Boysenberry");
        sdkMap.put(CM_VERSION_CODES.CANTALOUPE, "Cantaloupe");
        sdkMap.put(CM_VERSION_CODES.DRAGON_FRUIT, "Dragon Fruit");
        sdkMap.put(CM_VERSION_CODES.ELDERBERRY, "Elderberry");
        sdkMap.put(CM_VERSION_CODES.FIG, "Fig");
    }

    /** Various version strings. */
    public static class CM_VERSION {
        /**
         * The user-visible SDK version of the framework; its possible
         * values are defined in {@link Build.CM_VERSION_CODES}.
         *
         * Will return 0 if the device does not support the CM SDK.
         */
        public static final int SDK_INT = SystemProperties.getInt(
                "ro.cm.build.version.plat.sdk", 0);
    }

    /**
     * Enumeration of the currently known SDK version codes.  These are the
     * values that can be found in {@link CM_VERSION#SDK_INT}.  Version numbers
     * increment monotonically with each official platform release.
     *
     * To programmatically validate that a given API is available for use on the device,
     * you can quickly check if the SDK_INT from the OS is provided and is greater or equal
     * to the API level that your application is targeting.
     *
     * <p>Example for validating that Profiles API is available
     * <pre class="prettyprint">
     * private void removeActiveProfile() {
     *     Make sure we're running on BoysenBerry or higher to use Profiles API
     *     if (Build.CM_VERSION.SDK_INT >= Build.CM_VERSION_CODES.BOYSENBERRY) {
     *         ProfileManager profileManager = ProfileManager.getInstance(this);
     *         Profile activeProfile = profileManager.getActiveProfile();
     *         if (activeProfile != null) {
     *             profileManager.removeProfile(activeProfile);
     *         }
     *     }
     * }
     * </pre>
     */
    public static class CM_VERSION_CODES {
        /**
         * June 2015: The first version of the platform sdk for CyanogenMod
         */
        public static final int APRICOT = 1;

        /**
         * September 2015: The second version of the platform sdk for CyanogenMod
         *
         * <p>Applications targeting this or a later release will get these
         * new features:</p>
         * <ul>
         * <li>Profiles API via {@link cyanogenmod.app.ProfileManager}
         * <li>New Expanded Styles for Custom Tiles via
         * {@link cyanogenmod.app.CustomTile.RemoteExpandedStyle}
         * <li>Hardware Abstraction Framework Access via
         * {@link cyanogenmod.hardware.CMHardwareManager} (Not for use by 3rd parties)
         * <li>MSIM API via {@link cyanogenmod.app.CMTelephonyManager}
         * <li>Interface for partners via {@link cyanogenmod.app.PartnerInterface}
         * <li>Introductory Settings Provider {@link cyanogenmod.providers.CMSettings}
         * <li>AlarmClock API via {@link cyanogenmod.alarmclock.CyanogenModAlarmClock}
         * </ul>
         */
        public static final int BOYSENBERRY = 2;

        /**
         * November - December 2015: The third iteration of the platform sdk for CyanogenMod
         * Transition api level that is mostly 1:1 to {@link #BOYSENBERRY}
         */
        public static final int CANTALOUPE = 3;

        /**
         * January 2016: The 4th iteration of the platform sdk for CyanogenMod
         *
         * <p>Applications targeting this or a later version will get access to these
         * new features:</p>
         * <ul>
         * <li>External views api, and specifically Keyguard interfaces for making
         * live lockscreens via {@link cyanogenmod.externalviews.KeyguardExternalView}</li>
         * <li>Inclusion of the PerformanceManager interfaces, allowing an application to specify
         * the type of mode to have the device be placed in via
         * {@link cyanogenmod.power.PerformanceManager}</li>
         * <li>Numerous new "System" settings exposed via the
         * {@link cyanogenmod.providers.CMSettings.System} interface</li>
         * </ul>
         */
        public static final int DRAGON_FRUIT = 4;

        /**
         * April 2016: The 5th iteration of the platform sdk for CyanogenMod
         *
         * <p>Applications targeting this or a later version will get access to these
         * new features!</p>
         * <li>Weather request api to fetch weather data from providers on the device
         * {@link cyanogenmod.weather.CMWeatherManager}</li>
         * <li>Weather provider api to provide weather data to any listener on the device
         * {@link cyanogenmod.weatherservice.WeatherProviderService}</li>
         * <li>Extended capabilities of the {@link cyanogenmod.externalviews.KeyguardExternalView}
         * interfaces to provide immersive and interactive experiences on the lockscreen.</li>
         * <li>Themes interfaces have found a new home in the cmsdk, thus we allow access
         * to 3rd parties requesting theme changes on the platform via
         * {@link cyanogenmod.themes.ThemeManager} and
         * {@link cyanogenmod.themes.ThemeChangeRequest}</li>
         * <li>Full access to the {@link cyanogenmod.providers.ThemesContract} and provider</li>
         * <li>Parceling helper class {@link cyanogenmod.os.Concierge} to help with parcel
         * headers and protocol revisions</li>
         * </ul>
         */
        public static final int ELDERBERRY = 5;

        /**
         * Future Release - 3rd quarter 2016
         */
        public static final int FIG = 6;
    }

    /**
     * Retrieve the name for the SDK int
     * @param sdkInt
     * @return name of the SDK int, {@link #UNKNOWN) if not known
     */
    public static String getNameForSDKInt(int sdkInt) {
        final String name = sdkMap.get(sdkInt);
        if (TextUtils.isEmpty(name)) {
            return UNKNOWN;
        }
        return name;
    }
}
