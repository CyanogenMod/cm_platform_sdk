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

package org.cyanogenmod.tests.versioning.unit.apiv4;

import java.util.HashMap;
import java.util.Map;

public class ApiV4PriorReleaseInterfaces {
    private static Map<String, Map<String, Integer>> mApiMethodsAndValues =
            new HashMap<String, Map<String, Integer>>();

    //Profiles Aidl (IProfileManager)
    static {
        Map<String, Integer> profilesMap = getInternalInterfaceMap("IProfileManager");
        // APRICOT + BOYSENBERRY + CANTALOUPE to 19
        // DRAGONFRUIT BEGIN
        profilesMap.put("isEnabled", 20);
    }

    //PartnerInterface Aidl (IPartnerInterface)
    static {
        Map<String, Integer> partnerMap = getInternalInterfaceMap("IPartnerInterface");
        // APRICOT + BOYSENBERRY + CANTALOUPE to 6
        // DRAGONFRUIT BEGIN
        partnerMap.put("setZenModeWithDuration", 7);
    }

    //CMHardwareManager Aidl (ICMHardwareService)
    static {
        Map<String, Integer> hardwareMap = getInternalInterfaceMap("ICMHardwareService");
        // APRICOT + BOYSENBERRY + CANTALOUPE to 24
        // DRAGONFRUIT BEGIN
        hardwareMap.put("isSunlightEnhancementSelfManaged", 25);
        hardwareMap.put("getUniqueDeviceId", 26);
    }

    //CMStatusBarManager Aidl (ICMStatusBarManager)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE to 5
        // DRAGONFRUIT BEGIN
    }

    //AppSuggestManager Aidl (IAppSuggestManager)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE to 2
        // DRAGONFRUIT BEGIN
    }

    //CMTelephonyManager Aidl (ICMTelephonyManager)
    static {
        // APRICOT + BOYSENBERRY + CANTALOUPE to 9
        // DRAGONFRUIT BEGIN
    }

    //PerformanceManager Aidl (IPerformanceManager)
    static {
        Map<String, Integer> perfMap = getInternalInterfaceMap("IPerformanceManager");
        // DRAGONFRUIT BEGIN
        perfMap.put("cpuBoost", 1);
        perfMap.put("setPowerProfile", 2);
        perfMap.put("getPowerProfile", 3);
        perfMap.put("getNumberOfProfiles", 4);
        perfMap.put("getProfileHasAppProfiles", 5);
    }

    //ExternalViewProviderFactory Aidl (IExternalViewProviderFactory)
    static {
        Map<String, Integer> extProviderMap =
                getInternalInterfaceMap("IExternalViewProviderFactory");
        // DRAGONFRUIT BEGIN
        extProviderMap.put("createExternalView", 1);
    }

    //ExternalViewProvider Aidl (IExternalViewProvider)
    static {
        Map<String, Integer> extViewProviderMap =
                getInternalInterfaceMap("IExternalViewProvider");
        // DRAGONFRUIT BEGIN
        extViewProviderMap.put("onAttach", 1);
        extViewProviderMap.put("onStart", 2);
        extViewProviderMap.put("onResume", 3);
        extViewProviderMap.put("onPause", 4);
        extViewProviderMap.put("onStop", 5);
        extViewProviderMap.put("onDetach", 6);
        extViewProviderMap.put("alterWindow", 7);
    }

    //KeyguardExternalViewCallbacks Aidl (IKeyguardExternalViewCallbacks)
    static {
        Map<String, Integer> kgExtViewCbMap =
                getInternalInterfaceMap("IKeyguardExternalViewCallbacks");
        // DRAGONFRUIT BEGIN
        kgExtViewCbMap.put("requestDismiss", 1);
        kgExtViewCbMap.put("requestDismissAndStartActivity", 2);
        kgExtViewCbMap.put("collapseNotificationPanel", 3);
        kgExtViewCbMap.put("setInteractivity", 4);
    }

    //KeyguardExternalViewProvider Aidl (IKeyguardExternalViewProvider)
    static {
        Map<String, Integer> kgExtViewProviderMap =
                getInternalInterfaceMap("IKeyguardExternalViewProvider");
        // DRAGONFRUIT BEGIN
        kgExtViewProviderMap.put("onAttach", 1);
        kgExtViewProviderMap.put("onDetach", 2);
        kgExtViewProviderMap.put("onKeyguardShowing", 3);
        kgExtViewProviderMap.put("onKeyguardDismissed" , 4);
        kgExtViewProviderMap.put("onBouncerShowing", 5);
        kgExtViewProviderMap.put("onScreenTurnedOn", 6);
        kgExtViewProviderMap.put("onScreenTurnedOff", 7);
        kgExtViewProviderMap.put("registerCallback", 8);
        kgExtViewProviderMap.put("unregisterCallback", 9);
        kgExtViewProviderMap.put("alterWindow", 10);
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
