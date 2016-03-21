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

package cyanogenmod.weather;

import android.annotation.NonNull;
import android.content.Context;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import cyanogenmod.app.CMContextConstants;

/**
 * Provides access to the weather services in the device.
 */
public class CMWeatherManager {

    private static ICMWeatherManager sWeatherManagerService;
    private static CMWeatherManager sInstance;
    private Context mContext;

    /**
     * Broadcast intent action indicating new weather data is available on the weather content
     * provider.
     */
    public static final String WEATHER_UPDATED_ACTION
            = "cyanogenmod.weather.WEATHER_UPDATED";

    /**
     * Broadcast intent action indicating a change in the state of a weather update request,
     * reported with {@link #EXTRA_WEATHER_UPDATE_RESULT}
     */
    public static final String WEATHER_REQUEST_STATE_CHANGED_ACTION
            = "cyanogenmod.weather.WEATHER_REQUEST_STATE_CHANGED";

    /**
     * The result of the weather update request
     * @see cyanogenmod.weather.RequestInfo
     */
    public static final String EXTRA_WEATHER_UPDATE_RESULT = "request_result";

    private CMWeatherManager(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = (appContext != null) ? appContext : context;
        sWeatherManagerService = getService();

        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.WEATHER_SERVICES) && (sWeatherManagerService == null)) {
            throw new RuntimeException("Unable to bind the CMWeatherManagerService");
        }
    }

    /**
     * Gets or creates an instance of the {@link cyanogenmod.weather.CMWeatherManager}
     * @param context
     * @return {@link CMWeatherManager}
     */
    public static CMWeatherManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CMWeatherManager(context);
        }
        return sInstance;
    }

    /** @hide */
    public static ICMWeatherManager getService() {
        if (sWeatherManagerService != null) {
            return sWeatherManagerService;
        }
        IBinder binder = ServiceManager.getService(CMContextConstants.CM_WEATHER_SERVICE);
        if (binder != null) {
            sWeatherManagerService = ICMWeatherManager.Stub.asInterface(binder);
            return sWeatherManagerService;
        }
        return null;
    }

    /**
     * Forces the weather service to request the latest weather information for the provided
     * {@link android.location.Location} location. You are encouraged to register a
     * broadcast receiver for {@link #WEATHER_REQUEST_STATE_CHANGED_ACTION}
     * to receive notifications of the state of your request. Note that the broadcast will be
     * sent <em>only</em> to the package that requested the update. If you want to be notified
     * when new weather data is available, regardless of who requested the update,
     * you can register a receiver for {@link #WEATHER_UPDATED_ACTION}
     *
     * @param location The location you want to get the latest weather data from.
     */
    public void requestWeatherUpdate(@NonNull Location location) {
        if (sWeatherManagerService == null) {
            return;
        }

        try {
            sWeatherManagerService.forceWeatherUpdate(new RequestInfo(location,
                    mContext.getPackageName()));
        } catch (RemoteException e) {
        }
    }

    /**
     * Forces the weather service to request the latest weather information for the provided
     * city. The string should be localized. You are encouraged to register a
     * broadcast receiver for {@link #WEATHER_REQUEST_STATE_CHANGED_ACTION}
     * to receive notifications of the state of your request. Note that the broadcast will be
     * sent <em>only</em> to the package that requested the update. If you want to be notified
     * when new weather data is available, regardless of who requested the update,
     * you can register a receiver for {@link #WEATHER_UPDATED_ACTION}
     *
     * @param cityName The localized city name you want to get the latest weather data from.
     */
    public void requestWeatherUpdate(@NonNull String cityName) {
        if (sWeatherManagerService == null) {
            return;
        }

        try {
            sWeatherManagerService.forceWeatherUpdate(new RequestInfo(cityName,
                    mContext.getPackageName()));
        } catch (RemoteException e) {
        }
    }
}
