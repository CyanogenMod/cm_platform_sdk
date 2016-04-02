/*
 * Copyright (C) 2015 The CyanogenMod Project
 * This code has been modified.  Portions copyright (C) 2010, T-Mobile USA, Inc.
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

package cyanogenmod.content;

import android.Manifest;

/**
 * CyanogenMod specific intent definition class.
 */
public class Intent {

    /**
     * Activity Action: Start action associated with long press on the recents key.
     * <p>Input: {@link #EXTRA_LONG_PRESS_RELEASE} is set to true if the long press
     * is released
     * <p>Output: Nothing
     * @hide
     */
    public static final String ACTION_RECENTS_LONG_PRESS =
            "cyanogenmod.intent.action.RECENTS_LONG_PRESS";

    /**
     * This field is part of the intent {@link #ACTION_RECENTS_LONG_PRESS}.
     * The type of the extra is a boolean that indicates if the long press
     * is released.
     * @hide
     */
    public static final String EXTRA_RECENTS_LONG_PRESS_RELEASE =
            "cyanogenmod.intent.extra.RECENTS_LONG_PRESS_RELEASE";

    /**
     * Intent filter to update protected app component's settings
     */
    public static final String ACTION_PROTECTED = "cyanogenmod.intent.action.PACKAGE_PROTECTED";

    /**
     * Intent filter to notify change in state of protected application.
     */
    public static final String ACTION_PROTECTED_CHANGED =
            "cyanogenmod.intent.action.PROTECTED_COMPONENT_UPDATE";

    /**
     * This field is part of the intent {@link #ACTION_PROTECTED_CHANGED}.
     * Intent extra field for the state of protected application
     */
    public static final String EXTRA_PROTECTED_STATE =
            "cyanogenmod.intent.extra.PACKAGE_PROTECTED_STATE";

    /**
     * This field is part of the intent {@link #ACTION_PROTECTED_CHANGED}.
     * Intent extra field to indicate protected component value
     */
    public static final String EXTRA_PROTECTED_COMPONENTS =
            "cyanogenmod.intent.extra.PACKAGE_PROTECTED_COMPONENTS";

    /**
     * Broadcast action: notify the system that the user has performed a gesture on the screen
     * to launch the camera. Broadcast should be protected to receivers holding the
     * {@link Manifest.permission#STATUS_BAR_SERVICE} permission.
     * @hide
     */
    public static final String ACTION_SCREEN_CAMERA_GESTURE =
            "cyanogenmod.intent.action.SCREEN_CAMERA_GESTURE";

    /**
     * Broadcast action: perform any initialization required for CMHW services.
     * Runs when the service receives the signal the device has booted, but
     * should happen before {@link android.content.Intent#ACTION_BOOT_COMPLETED}.
     *
     * Requires {@link cyanogenmod.platform.Manifest.permission#HARDWARE_ABSTRACTION_ACCESS}.
     * @hide
     */
    public static final String ACTION_INITIALIZE_CM_HARDWARE =
            "cyanogenmod.intent.action.INITIALIZE_CM_HARDWARE";

    /**
     * Broadcast Action: Indicate that an unrecoverable error happened during app launch.
     * Could indicate that curently applied theme is malicious.
     * @hide
     */
    public static final String ACTION_APP_FAILURE = "cyanogenmod.intent.action.APP_FAILURE";

    /**
     * Used to indicate that a theme package has been installed or un-installed.
     */
    public static final String CATEGORY_THEME_PACKAGE_INSTALLED_STATE_CHANGE =
            "cyanogenmod.intent.category.THEME_PACKAGE_INSTALL_STATE_CHANGE";

    /**
     * Action sent from the provider when a theme has been fully installed.  Fully installed
     * means that the apk was installed by PackageManager and the theme resources were
     * processed and cached by {@link org.cyanogenmod.platform.internal.ThemeManagerService}
     * Requires the {@link  cyanogenmod.platform.Manifest.permission#READ_THEMES} permission to
     * receive this broadcast.
     */
    public static final String ACTION_THEME_INSTALLED =
            "cyanogenmod.intent.action.THEME_INSTALLED";

    /**
     * Action sent from the provider when a theme has been updated.
     * Requires the {@link cyanogenmod.platform.Manifest.permission#READ_THEMES} permission to
     * receive this broadcast.
     */
    public static final String ACTION_THEME_UPDATED =
            "cyanogenmod.intent.action.THEME_UPDATED";

    /**
     * Action sent from the provider when a theme has been removed.
     * Requires the {@link  cyanogenmod.platform.Manifest.permission#READ_THEMES} permission to
     * receive this broadcast.
     */
    public static final String ACTION_THEME_REMOVED =
            "cyanogenmod.intent.action.THEME_REMOVED";

    /**
     * Uri scheme used to broadcast the theme's package name when broadcasting
     * {@link Intent#ACTION_THEME_INSTALLED} or
     * {@link Intent#ACTION_THEME_REMOVED}
     */
    public static final String URI_SCHEME_PACKAGE = "package";

    /**
     * Implicit action to open live lock screen settings.
     * @hide
     */
    public static final String ACTION_OPEN_LIVE_LOCKSCREEN_SETTINGS =
            "cyanogenmod.intent.action.OPEN_LIVE_LOCKSCREEN_SETTINGS";

}
