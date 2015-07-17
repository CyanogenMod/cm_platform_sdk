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

    private static final SparseArray<String> sdkMap;
    static
    {
        sdkMap = new SparseArray<String>();
        sdkMap.put(CM_VERSION_CODES.APRICOT, "Apricot");
        sdkMap.put(CM_VERSION_CODES.BOYSENBERRY, "Boysenberry");
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
         * July 2015 - ?: The second version of the platform sdk for CyanogenMod
         */
        public static final int BOYSENBERRY = 2;
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
