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


public final class PackageUtils {
    private static final String TAG = "PackageUtils";

    private PackageUtils() {
        // This class is not supposed to be instantiated
    }

    /**
     * Checks whether a given package exists
     *
     * @return true if existing
     */
    public static boolean isAppPresent(Context context, String packageName) {
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
     * @return true if enabled
     */
    public static boolean isAppEnabled(Context context, String packageName) {
        return isAppEnabled(context, packageName, 0);
    }

    /**
     * Checks whether a given package exists with given flags and is enabled
     *
     * @return true if enabled
     */
    public static boolean isAppEnabled(Context context, String packageName, int flags) {
        final PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, flags);
            return info != null && info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
