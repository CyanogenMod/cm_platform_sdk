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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import cyanogenmod.platform.R;


/**
 * Helpers for checking if a device supports various features.
 *
 * @hide
 */
public class Constraints {

    public static boolean checkConstraints(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return true;
        }

        TypedArray a = context.getResources().obtainAttributes(attrs,
                R.styleable.cm_SelfRemovingPreference);

        try {

            // Check if a specific package is installed
            String rPackage = a.getString(R.styleable.cm_SelfRemovingPreference_requiresPackage);
            if (rPackage != null && !isPackageInstalled(context, rPackage, false)) {
                return false;
            }

            // Check if a system feature is available
            String rFeature = a.getString(R.styleable.cm_SelfRemovingPreference_requiresFeature);
            if (rFeature != null && !hasSystemFeature(context, rFeature)) {
                return false;
            }

            // Check a boolean system property
            String rProperty = a.getString(R.styleable.cm_SelfRemovingPreference_requiresProperty);
            if (rProperty != null) {
                String value = SystemProperties.get(rProperty);
                if (value == null || !Boolean.parseBoolean(value)) {
                    return false;
                }
            }

            // Check a config resource. This can be a bool or a string. A null string
            // fails the constraint.
           TypedValue tv = a.peekValue(R.styleable.cm_SelfRemovingPreference_requiresConfig);
            if (tv != null) {
                if (tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        if (context.getResources().getString(tv.resourceId) == null) {
                            return false;
                        }
                    }
                } else if (tv.type == TypedValue.TYPE_INT_BOOLEAN) {
                    if (tv.resourceId != 0) {
                        if (!context.getResources().getBoolean(tv.resourceId)) {
                            return false;
                        }
                    }
                }
            }
        } finally {
            a.recycle();
        }

        return true;
    }

    /**
     * Returns whether the device supports a particular feature
     */
    public static boolean hasSystemFeature(Context context, String feature) {
        return context.getPackageManager().hasSystemFeature(feature);
    }
    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    /**
     * Checks if a package is installed. Set the ignoreState argument to true if you don't
     * care if the package is enabled/disabled.
     */
    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Does the device support Doze?
     * @param context
     * @return
     */
    public static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }
}
