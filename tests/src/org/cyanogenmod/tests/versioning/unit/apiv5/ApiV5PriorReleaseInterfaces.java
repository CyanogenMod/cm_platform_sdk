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

package org.cyanogenmod.tests.versioning.unit.apiv5;

import java.util.HashMap;
import java.util.Map;

public class ApiV5PriorReleaseInterfaces {
    private static Map<String, Map<String, Integer>> mApiMethodsAndValues =
            new HashMap<String, Map<String, Integer>>();

    //ExternalViewProviderFactory Aidl (IExternalViewProviderFactory)
    static {
        Map<String, Integer> extProviderMap =
                getInternalInterfaceMap("IExternalViewProviderFactory");
        // DRAGONFRUIT TO 1
        // ELDERBERRY BEGIN
    }

    //ExternalViewProvider Aidl (IExternalViewProvider)
    static {
        Map<String, Integer> extViewProviderMap =
                getInternalInterfaceMap("IExternalViewProvider");
        // DRAGONFRUIT TO 7
        // ELDERBERRY BEGIN
    }

    //KeyguardExternalViewCallbacks Aidl (IKeyguardExternalViewCallbacks)
    static {
        Map<String, Integer> kgExtViewCbMap =
                getInternalInterfaceMap("IKeyguardExternalViewCallbacks");
        // DRAGONFRUIT TO 4
        // ELDERBERRY BEGIN
        kgExtViewCbMap.put("onAttachedToWindow", 5);
        kgExtViewCbMap.put("onDetachedFromWindow", 6);
        kgExtViewCbMap.put("slideLockscreenIn", 7);
    }

    //KeyguardExternalViewProvider Aidl (IKeyguardExternalViewProvider)
    static {
        Map<String, Integer> kgExtViewProviderMap =
                getInternalInterfaceMap("IKeyguardExternalViewProvider");
        // DRAGONFRUIT TO 10
        // ELDERBERRY BEGIN
        kgExtViewProviderMap.put("onLockscreenSlideOffsetChanged", 11);
    }

    //LiveLockscreenManager Aidl (ILiveLockScreenManager)
    static {
        Map<String, Integer> llScreenManagerMap =
                getInternalInterfaceMap("ILiveLockScreenManager");
        //ELDERBERRY BEGIN
        llScreenManagerMap.put("enqueueLiveLockScreen", 1);
        llScreenManagerMap.put("cancelLiveLockScreen", 2);
        llScreenManagerMap.put("getCurrentLiveLockScreen", 3);
        llScreenManagerMap.put("getDefaultLiveLockScreen", 4);
        llScreenManagerMap.put("setDefaultLiveLockScreen", 5);
        llScreenManagerMap.put("setLiveLockScreenEnabled", 6);
        llScreenManagerMap.put("getLiveLockScreenEnabled ", 7);
        llScreenManagerMap.put("registerChangeListener", 8);
        llScreenManagerMap.put("unregisterChangeListener", 9);
    }

    //BaseLiveLockManagerService Aidl (ILiveLockScreenManagerProvider)
    static {
        Map<String, Integer> llManagerProvider =
                getInternalInterfaceMap("ILiveLockScreenManagerProvider");
        //ELDERBERRY BEGIN
        llManagerProvider.put("enqueueLiveLockScreen", 1);
        llManagerProvider.put("cancelLiveLockScreen", 2);
        llManagerProvider.put("getCurrentLiveLockScreen", 3);
        llManagerProvider.put("updateDefaultLiveLockScreen", 4);
        llManagerProvider.put("getLiveLockScreenEnabled ", 5);
        llManagerProvider.put("registerChangeListener", 6);
        llManagerProvider.put("unregisterChangeListener", 7);
    }

    //LiveLockScreenChangeListener Aidl (ILiveLockScreenChangeListener)
    static{
        Map<String, Integer> llChangeListener =
                getInternalInterfaceMap("ILiveLockScreenChangeListener");
        //ELDERBERRY BEGIN
        llChangeListener.put("onLiveLockScreenChanged", 1);
    }

    //CMAudioManager Aidl (ICMAudioService)
    static {
        Map<String, Integer> cmAudioService =
                getInternalInterfaceMap("ICMAudioService");
        //ELDERBERRY BEGIN
        cmAudioService.put("listAudioSessions", 1);
    }

    //ThemeChangeListener Aidl (IThemeChangeListener)
    static {
        Map<String, Integer> themeChangeListener =
                getInternalInterfaceMap("IThemeChangeListener");
        //ELDERBERRY BEGIN
        themeChangeListener.put("onProgress", 1);
        themeChangeListener.put("onFinish", 2);
    }

    //ThemeProcessingListener Aidl (IThemeProcessingListener)
    static {
        Map<String, Integer> themeChangeListener =
                getInternalInterfaceMap("IThemeProcessingListener");
        //ELDERBERRY BEGIN
        themeChangeListener.put("onFinishedProcessing", 1);
    }

    //ThemeManager Aidl (IThemeService)
    static {
        Map<String, Integer> themes =
                getInternalInterfaceMap("IThemeService");
        //ELDERBERRY BEGIN
        themes.put("requestThemeChangeUpdates", 1);
        themes.put("removeUpdates ", 2);
        themes.put("requestThemeChange", 3);
        themes.put("applyDefaultTheme", 4);
        themes.put("isThemeApplying", 5);
        themes.put("getProgress", 6);
        themes.put("processThemeResources", 7);
        themes.put("isThemeBeingProcessed", 8);
        themes.put("registerThemeProcessingListener", 9);
        themes.put("unregisterThemeProcessingListener", 10);
        themes.put("rebuildResourceCache", 11);
        themes.put("getLastThemeChangeTime", 12);
        themes.put("getLastThemeChangeRequestType", 13);
    }

    //CMWeatherManager Aidl (ICMWeatherManager)
    static {
        Map<String, Integer> icmWeatherManager =
                getInternalInterfaceMap("ICMWeatherManager");
        //ELDERBERRY BEGIN
        icmWeatherManager.put("updateWeather", 1);
        icmWeatherManager.put("lookupCity ", 2);
        icmWeatherManager.put("registerWeatherServiceProviderChangeListener", 3);
        icmWeatherManager.put("unregisterWeatherServiceProviderChangeListener", 4);
        icmWeatherManager.put("getActiveWeatherServiceProviderLabel", 5);
        icmWeatherManager.put("cancelRequest", 6);
    }

    //RequestInfoListener Aidl (IRequestInfoListener)
    static {
        Map<String, Integer> requestInfoListener =
                getInternalInterfaceMap("IRequestInfoListener");
        //ELDERBERRY BEGIN
        requestInfoListener.put("onWeatherRequestCompleted", 1);
        requestInfoListener.put("onLookupCityRequestCompleted ", 2);
    }

    //WeatherServiceProviderChangeListener Aidl (IWeatherServiceProviderChangeListener)
    static {
        Map<String, Integer> weatherServiceProviderChangeListener =
                getInternalInterfaceMap("IWeatherServiceProviderChangeListener");
        //ELDERBERRY BEGIN
        weatherServiceProviderChangeListener.put("onWeatherServiceProviderChanged", 1);
    }

    //WeatherProviderService Aidl (IWeatherProviderService)
    static {
        Map<String, Integer> weatherProviderService =
                getInternalInterfaceMap("IWeatherProviderService");
        //ELDERBERRY BEGIN
        weatherProviderService.put("processWeatherUpdateRequest", 1);
        weatherProviderService.put("processCityNameLookupRequest ", 2);
        weatherProviderService.put("setServiceClient", 3);
        weatherProviderService.put("cancelOngoingRequests", 4);
        weatherProviderService.put("cancelRequest", 5);
    }

    //WeatherProviderServiceClient Aidl (IWeatherProviderServiceClient)
    static {
        Map<String, Integer> weatherProviderServiceClient =
                getInternalInterfaceMap("IWeatherProviderServiceClient");
        //ELDERBERRY BEGIN
        weatherProviderServiceClient.put("setServiceRequestState", 1);
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
