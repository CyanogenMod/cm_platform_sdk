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
    private static Map<String, Map<String, Integer>> mApiMethodsAndValues =
            new HashMap<String, Map<String, Integer>>();

    //Profiles Aidl (IProfileManager)
    static {
        Map<String, Integer> profilesMap = getInternalInterfaceMap("IProfileManager");
        // APRICOT + BOYSENBERRY + CANTALOUPE
        profilesMap.put("setActiveProfile", 1);
        profilesMap.put("etActiveProfileByName", 2);
        profilesMap.put("getActiveProfile", 3);
        profilesMap.put("addProfile", 4);
        profilesMap.put("removeProfile", 5);
        profilesMap.put("updateProfile", 6);
        profilesMap.put("getProfile", 7);
        profilesMap.put("getProfileByName", 8);
        profilesMap.put("getProfiles", 9);
        profilesMap.put("profileExists", 10);
        profilesMap.put("profileExistsByName", 11);
        profilesMap.put("notificationGroupExistsByName", 12);
        profilesMap.put("getNotificationGroups", 13);
        profilesMap.put("addNotificationGroup", 14);
        profilesMap.put("removeNotificationGroup", 15);
        profilesMap.put("updateNotificationGroup", 16);
        profilesMap.put("getNotificationGroupForPackage", 17);
        profilesMap.put("getNotificationGroup", 18);
        profilesMap.put("resetAll", 19);
    }

    //PartnerInterface Aidl (IPartnerInterface)
    static {
        Map<String, Integer> partnerMap = getInternalInterfaceMap("IPartnerInterface");
        // APRICOT + BOYSENBERRY + CANTALOUPE
        partnerMap.put("setAirplaneModeEnabled", 1);
        partnerMap.put("setMobileDataEnabled", 2);
        partnerMap.put("setZenMode", 3);
        partnerMap.put("shutdown", 4);
        partnerMap.put("reboot", 5);
        partnerMap.put("getCurrentHotwordPackageName", 6);
    }

    //CMHardwareManager Aidl (ICMHardwareService)
    static {
        Map<String, Integer> hardwareMap = getInternalInterfaceMap("ICMHardwareService");
        // APRICOT + BOYSENBERRY + CANTALOUPE
        hardwareMap.put("getSupportedFeatures", 1);
        hardwareMap.put("get", 2);
        hardwareMap.put("set", 3);
        hardwareMap.put("getDisplayColorCalibration", 4);
        hardwareMap.put("setDisplayColorCalibration", 5);
        hardwareMap.put("getNumGammaControls", 6);
        hardwareMap.put("getDisplayGammaCalibration", 7);
        hardwareMap.put("setDisplayGammaCalibration", 8);
        hardwareMap.put("getVibratorIntensity", 9);
        hardwareMap.put("setVibratorIntensity", 10);
        hardwareMap.put("getLtoSource", 11);
        hardwareMap.put("getLtoDestination", 12);
        hardwareMap.put("getLtoDownloadInterval", 13);
        hardwareMap.put("getSerialNumber", 14);
        hardwareMap.put("requireAdaptiveBacklightForSunlightEnhancement", 15);
        hardwareMap.put("getDisplayModes", 16);
        hardwareMap.put("getCurrentDisplayMode", 17);
        hardwareMap.put("getDefaultDisplayMode", 18);
        hardwareMap.put("setDisplayMode", 19);
        hardwareMap.put("writePersistentBytes", 20);
        hardwareMap.put("readPersistentBytes", 21);
        hardwareMap.put("getThermalState", 22);
        hardwareMap.put("registerThermalListener", 23);
        hardwareMap.put("unRegisterThermalListener", 24);
    }

    //CMStatusBarManager Aidl (ICMStatusBarManager)
    static {
        Map<String, Integer> statusBarMap = getInternalInterfaceMap("ICMStatusBarManager");
        // APRICOT + BOYSENBERRY + CANTALOUPE
        statusBarMap.put("createCustomTileWithTag", 1);
        statusBarMap.put("removeCustomTileWithTag", 2);
        statusBarMap.put("registerListener", 3);
        statusBarMap.put("unregisterListener", 4);
        statusBarMap.put("removeCustomTileFromListener", 5);
    }

    //AppSuggestManager Aidl (IAppSuggestManager)
    static {
        Map<String, Integer> suggestMap = getInternalInterfaceMap("IAppSuggestManager");
        // APRICOT + BOYSENBERRY + CANTALOUPE
        suggestMap.put("handles", 1);
        suggestMap.put("getSuggestions", 2);
    }

    //CMTelephonyManager Aidl (ICMTelephonyManager)
    static {
        Map<String, Integer> telephonyMap = getInternalInterfaceMap("ICMTelephonyManager");
        // APRICOT + BOYSENBERRY + CANTALOUPE
        telephonyMap.put("getSubInformation", 1);
        telephonyMap.put("isSubActive", 2);
        telephonyMap.put("isDataConnectionSelectedOnSub", 3);
        telephonyMap.put("isDataConnectionEnabled", 4);
        telephonyMap.put("setSubState", 5);
        telephonyMap.put("setDataConnectionSelectedOnSub", 6);
        telephonyMap.put("setDataConnectionState", 7);
        telephonyMap.put("setDefaultPhoneSub", 8);
        telephonyMap.put("setDefaultSmsSub", 9);
    }

    protected static Map<String, Integer> getInternalInterfaceMap(String targetInterface) {
        Map<String, Integer> internalMap = mApiMethodsAndValues.get(targetInterface);
        if (internalMap == null) {
            internalMap = new HashMap<String, Integer>();
            mApiMethodsAndValues.put(targetInterface, internalMap);
            return internalMap;
        }
        return internalMap;
    }

    public static Map<String, Map<String, Integer>> getInterfaces() {
        return mApiMethodsAndValues;
    }
}
