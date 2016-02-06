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

package org.cyanogenmod.tests.versioning.unit.apiv2;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adnan on 2/4/16.
 */
public class ApiV2PriorReleaseInterfaces {
    private static Map<String, Integer> mApiMethodsAndValues = new HashMap<String, Integer>();

    //Profiles Aidl (IProfileManager)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE
        mApiMethodsAndValues.put("setActiveProfile", 1);
        mApiMethodsAndValues.put("etActiveProfileByName", 2);
        mApiMethodsAndValues.put("getActiveProfile", 3);
        mApiMethodsAndValues.put("addProfile", 4);
        mApiMethodsAndValues.put("removeProfile", 5);
        mApiMethodsAndValues.put("updateProfile", 6);
        mApiMethodsAndValues.put("getProfile", 7);
        mApiMethodsAndValues.put("getProfileByName", 8);
        mApiMethodsAndValues.put("getProfiles", 9);
        mApiMethodsAndValues.put("profileExists", 10);
        mApiMethodsAndValues.put("profileExistsByName", 11);
        mApiMethodsAndValues.put("notificationGroupExistsByName", 12);
        mApiMethodsAndValues.put("getNotificationGroups", 13);
        mApiMethodsAndValues.put("addNotificationGroup", 14);
        mApiMethodsAndValues.put("removeNotificationGroup", 15);
        mApiMethodsAndValues.put("updateNotificationGroup", 16);
        mApiMethodsAndValues.put("getNotificationGroupForPackage", 17);
        mApiMethodsAndValues.put("getNotificationGroup", 18);
        mApiMethodsAndValues.put("resetAll", 19);

        //FUTURE RELEASE
    }

    //PartnerInterface Aidl (IPartnerInterface)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE
        mApiMethodsAndValues.put("setAirplaneModeEnabled_0", 1);
        mApiMethodsAndValues.put("setMobileDataEnabled_1", 2);
        mApiMethodsAndValues.put("setZenMode", 3);
        mApiMethodsAndValues.put("shutdown", 4);
        mApiMethodsAndValues.put("reboot", 5);
        mApiMethodsAndValues.put("getCurrentHotwordPackageName", 6);

        //FUTURE RELEASE
    }

    //CMHardwareManager Aidl (ICMHardwareService)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE
        mApiMethodsAndValues.put("getSupportedFeatures_0", 1);
        mApiMethodsAndValues.put("get_1", 2);
        mApiMethodsAndValues.put("set", 3);
        mApiMethodsAndValues.put("getDisplayColorCalibration", 4);
        mApiMethodsAndValues.put("setDisplayColorCalibration", 5);
        mApiMethodsAndValues.put("getNumGammaControls", 6);
        mApiMethodsAndValues.put("getDisplayGammaCalibration", 7);
        mApiMethodsAndValues.put("setDisplayGammaCalibration", 8);
        mApiMethodsAndValues.put("getVibratorIntensity", 9);
        mApiMethodsAndValues.put("setVibratorIntensity", 10);
        mApiMethodsAndValues.put("getLtoSource", 11);
        mApiMethodsAndValues.put("getLtoDestination", 12);
        mApiMethodsAndValues.put("getLtoDownloadInterval", 13);
        mApiMethodsAndValues.put("getSerialNumber", 14);
        mApiMethodsAndValues.put("requireAdaptiveBacklightForSunlightEnhancement", 15);
        mApiMethodsAndValues.put("getDisplayModes", 16);
        mApiMethodsAndValues.put("getCurrentDisplayMode", 17);
        mApiMethodsAndValues.put("getDefaultDisplayMode", 18);
        mApiMethodsAndValues.put("setDisplayMode", 19);
        mApiMethodsAndValues.put("writePersistentBytes", 20);
        mApiMethodsAndValues.put("readPersistentBytes", 21);
        mApiMethodsAndValues.put("getThermalState", 22);
        mApiMethodsAndValues.put("registerThermalListener", 23);
        mApiMethodsAndValues.put("unRegisterThermalListener", 24);

        //FUTURE RELEASE
    }

    //CMStatusBarManager Aidl (ICMStatusBarManager)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE
        mApiMethodsAndValues.put("createCustomTileWithTag", 1);
        mApiMethodsAndValues.put("removeCustomTileWithTag", 2);
        mApiMethodsAndValues.put("registerListener", 3);
        mApiMethodsAndValues.put("unregisterListener", 4);
        mApiMethodsAndValues.put("removeCustomTileFromListener", 5);

        //FUTURE RELEASE
    }

    //AppSuggestManager Aidl (IAppSuggestManager)
    static {
        mApiMethodsAndValues.put("handles_0", 1);
        mApiMethodsAndValues.put("getSuggestions_1", 2);
    }

    public static Map<String, Integer> getInterfaces() {
        return mApiMethodsAndValues;
    }
}
