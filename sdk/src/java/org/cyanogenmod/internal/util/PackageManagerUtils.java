/**
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
package org.cyanogenmod.internal.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public final class PackageManagerUtils {
    private static final int FLAG_SUSPENDED = 1 << 30;

    private PackageManagerUtils() {
        // This class is not supposed to be instantiated
    }

    /**
     * Checks whether a given package exists
     *
     * @param context
     * @param packageName
     * @return true if the package exists
     */
    public static boolean isAppInstalled(final Context context, final String packageName) {
        return getApplicationInfo(context, packageName, 0) != null;
    }

    /**
     * Check whether a package with specific flags is enabled
     *
     * @param context
     * @param packageName
     * @param flags
     * @return true if the package is enabled
     */
    public static boolean isAppEnabled(final Context context,
                                       final String packageName, final int flags) {
        final ApplicationInfo info = getApplicationInfo(context, packageName, flags);
        return info != null && info.enabled;
    }

    /**
     * Check whether a package is enabled
     *
     * @param context
     * @param packageName
     * @return true if the package is enabled
     */
    public static boolean isAppEnabled(final Context context, final String packageName) {
        return isAppEnabled(context, packageName, 0);
    }

    /**
     * Check if a package can possibly be on the SDCard
     * This is just a workaround and doesn't guarantee that the app is on SD card
     *
     * @param context
     * @param packageName
     * @return true if the package is on the SDCard
     */
    public static boolean isAppOnSdcard(final Context context, final String packageName) {
        return isAppEnabled(context, packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
    }

    /**
     * Check if a package is suspended
     *
     * @param context
     * @param packageName
     * @return true if the package is suspended
     */
    public static boolean isAppSuspended(final Context context, final String packageName) {
        return isAppSuspended(getApplicationInfo(context, packageName, 0));
    }

    /**
     * Check if a package is suspended
     *
     * @param info
     * @return true if the package is suspended
     */
    public static boolean isAppSuspended(final ApplicationInfo info) {
        return info != null && (info.flags & FLAG_SUSPENDED) != 0;
    }

    /**
     * Get the ApplicationInfo of a package
     *
     * @param context
     * @param packageName
     * @param flags
     * @return null if the package cannot be found or the ApplicationInfo is null
     */
    public static ApplicationInfo getApplicationInfo(final Context context,
                                                     final String packageName, final int flags) {
        final PackageManager packageManager = context.getPackageManager();
        ApplicationInfo info;
        try {
            info = packageManager.getApplicationInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            info = null;
        }
        return info;
    }
}
