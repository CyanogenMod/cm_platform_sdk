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
package org.cyanogenmod.internal.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public final class PackageUtils {
    private static final int FLAG_SUSPENDED = 1<<30;

    private static final boolean ATLEAST_N = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    private PackageUtils() {
        // This class is not supposed to be instantiated
    }

    /**
     * Checks whether a given package exists
     *
     * @return true if package exists
     */
    public static boolean isAppPresent(final Context context, final String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether a given package exists and is enabled
     *
     * @return true if package is enabled
     */
    public static boolean isAppEnabled(final Context context, final String packageName) {
        return isAppEnabled(context, packageName, 0);
    }

    /**
     * Checks whether a given package with the given flags exists and is enabled
     *
     * @return true if package is enabled
     */
    public static boolean isAppEnabled(final Context context,
                                       final String packageName, final int flags) {
        final PackageManager packageManager = context.getPackageManager();
        try {
            final ApplicationInfo info = packageManager.getApplicationInfo(packageName, flags);
            return info != null && info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the app can possibly be on the SDCard. This is just a workaround and doesn't
     * guarantee that the app is on SD card.
     *
     * @return true if app is on SDCard
     */
    public static boolean isAppOnSdcard(final Context context, final String packageName) {
        return isAppEnabled(context, packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
    }

    /**
     * Checks if the app is suspended
     *
     * @return true if the app is suspended
     */
    public static boolean isAppSuspended(Context context, String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            return info != null && isAppSuspended(info);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the app is suspended
     *
     * @return true if the app is suspended
     */
    public static boolean isAppSuspended(final ApplicationInfo info) {
        // The value of FLAG_SUSPENDED was reused by a hidden constant
        // ApplicationInfo.FLAG_PRIVILEGED prior to N, so only check for suspended flag on N
        // or later.
        return ATLEAST_N && (info != null) && ((info.flags & FLAG_SUSPENDED) != 0);
    }
}
