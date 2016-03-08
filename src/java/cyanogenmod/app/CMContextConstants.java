/**
 * Copyright (c) 2015, The CyanogenMod Project
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

package cyanogenmod.app;

import android.annotation.SdkConstant;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class CMContextConstants {

    /**
     * @hide
     */
    private CMContextConstants() {
        // Empty constructor
    }

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.CMStatusBarManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.CMStatusBarManager
     */
    public static final String CM_STATUS_BAR_SERVICE = "cmstatusbar";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.ProfileManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.ProfileManager
     *
     * @hide
     */
    public static final String CM_PROFILE_SERVICE = "profile";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.PartnerInterface} interact with system settings.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.PartnerInterface
     *
     * @hide
     */
    public static final String CM_PARTNER_INTERFACE = "cmpartnerinterface";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.CMTelephonyManager} to manage the phone and
     * data connection.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.CMTelephonyManager
     *
     * @hide
     */
    public static final String CM_TELEPHONY_MANAGER_SERVICE = "cmtelephonymanager";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.hardware.CMHardwareManager} to manage the extended
     * hardware features of the device.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.hardware.CMHardwareManager
     *
     * @hide
     */
    public static final String CM_HARDWARE_SERVICE = "cmhardware";

    /**
     * @hide
     */
    public static final String CM_APP_SUGGEST_SERVICE = "cmappsuggest";

    /**
     * Control device power profile and characteristics.
     *
     * @hide
     */
    public static final String CM_PERFORMANCE_SERVICE = "cmperformance";

    /**
     * Controls changing and applying themes
     *
     * @hide
     */
    public static final String CM_THEME_SERVICE = "cmthemes";

    /**
     * Manages composed icons
     *
     * @hide
     */
    public static final String CM_ICON_CACHE_SERVICE = "cmiconcache";

    /**
     * @hide
     */
    public static final String CM_LIVE_LOCK_SCREEN_SERVICE = "cmlivelockscreen";

    /**
     * Features supported by the CMSDK.
     */
    public static class Features {
        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the hardware abstraction
         * framework service utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HARDWARE_ABSTRACTION = "org.cyanogenmod.hardware";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm status bar service
         * utilzed by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String STATUSBAR = "org.cyanogenmod.statusbar";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm profiles service
         * utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PROFILES = "org.cyanogenmod.profiles";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm app suggest service
         * utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String APP_SUGGEST = "org.cyanogenmod.appsuggest";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm telephony service
         * utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String TELEPHONY = "org.cyanogenmod.telephony";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm theme service
         * utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String THEMES = "org.cyanogenmod.theme";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm performance service
         * utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PERFORMANCE = "org.cyanogenmod.performance";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm partner service
         * utilized by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PARTNER = "org.cyanogenmod.partner";

        /*
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the Live lock screen
         * feature.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String LIVE_LOCK_SCREEN = "org.cyanogenmod.livelockscreen";
    }
}
