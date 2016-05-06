/*
 * Copyright (C) 2016, The CyanogenMod Project
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
package org.cyanogenmod.platform.internal;

import android.util.Log;

class NativeHelper {

    private static final String TAG = "CMSDK-JNI";

    private static Boolean sNativeLibraryLoaded;

    /**
     * Loads the JNI backend if not already loaded.
     *
     * @return true if JNI platform library is loaded
     */
    synchronized static boolean isNativeLibraryAvailable() {
        if (sNativeLibraryLoaded == null) {
            try {
                System.loadLibrary("cmsdk_platform_jni");
                sNativeLibraryLoaded = true;

            } catch (Throwable t) {
                sNativeLibraryLoaded = false;
                Log.w(TAG, "CMSDK native library unavailable");
            }
        }
        return sNativeLibraryLoaded;
    }
}
