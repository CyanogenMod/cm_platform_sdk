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
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import cyanogenmod.app.CMContextConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides access to the weather services in the device.
 */
public class CMWeatherManager {

    private static ICMWeatherManager sWeatherManagerService;
    private static CMWeatherManager sInstance;
    private Context mContext;
    private Map<Integer,WeatherUpdateRequestListener> mWeatherUpdateRequestListeners
            = Collections.synchronizedMap(new HashMap<Integer,WeatherUpdateRequestListener>());

    private Map<Integer,LookupCityRequestListener> mLookupNameRequestListeners
            = Collections.synchronizedMap(new HashMap<Integer,LookupCityRequestListener>());
    private Handler mHandler;
    private static final String TAG = CMWeatherManager.class.getSimpleName();

    /**
     * Broadcast intent action indicating new weather data is available on the weather content
     * provider.
     */
    public static final String WEATHER_UPDATED_ACTION = "cyanogenmod.weather.WEATHER_UPDATED";

    /**
     * Weather update request state: Successfully completed
     */
    public static final int WEATHER_REQUEST_COMPLETED = 1;

    /**
     * Weather update request state: The weather does not change very often. You need to wait
     * a bit longer before requesting an update again
     */
    public static final int WEATHER_REQUEST_SUBMITTED_TOO_SOON = -1;

    /**
     * Weather update request state: An error occurred while trying to update the weather. You
     * should wait before trying again, or your request will be rejected with
     * {@link #WEATHER_REQUEST_SUBMITTED_TOO_SOON}
     */
    public static final int WEATHER_REQUEST_FAILED = -2;

    /**
     * Weather update request state: Only one update request can be processed at a given time.
     */
    public static final int WEATHER_REQUEST_ALREADY_IN_PROGRESS = -3;

    /** @hide */
    public static final int LOOKUP_REQUEST_COMPLETED  = 100;

    private CMWeatherManager(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = (appContext != null) ? appContext : context;
        sWeatherManagerService = getService();

        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.WEATHER_SERVICES) && (sWeatherManagerService == null)) {
            throw new RuntimeException("Unable to bind the CMWeatherManagerService");
        }
        mHandler = new Handler(appContext.getMainLooper());
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
     * Forces the weather service to request the latest available weather information for
     * the supplied {@link android.location.Location} location.
     *
     * @param location The location you want to get the latest weather data from.
     * @param listener {@link WeatherUpdateRequestListener} To be notified once the active weather
     *                                                     service provider has finished
     *                                                     processing your request
     */
    public void requestWeatherUpdate(@NonNull Location location,
            @NonNull WeatherUpdateRequestListener listener) {
        if (sWeatherManagerService == null) {
            return;
        }

        try {
            RequestInfo info = new RequestInfo
                    .Builder(mWeatherManagerCallbacks)
                    .setLocation(location)
                    .create();
            if (listener != null) mWeatherUpdateRequestListeners.put(info.getKey(), listener);
            sWeatherManagerService.forceWeatherUpdate(info);
        } catch (RemoteException e) {
        }
    }

    /**
     * Forces the weather service to request the latest weather information for the provided
     * WeatherLocation. This is the preferred method for requesting a weather update.
     *
     * @param weatherLocation A {@link cyanogenmod.weather.WeatherLocation} that was previously
     *                        obtained by calling
     *                        {@link #lookupCity(String, LookupCityRequestListener)}
     * @param listener {@link WeatherUpdateRequestListener} To be notified once the active weather
     *                                                     service provider has finished
     *                                                     processing your request
     */
    public void requestWeatherUpdate(@NonNull WeatherLocation weatherLocation,
            @NonNull WeatherUpdateRequestListener listener) {
        if (sWeatherManagerService == null) {
            return;
        }

        try {
            RequestInfo info = new RequestInfo
                    .Builder(mWeatherManagerCallbacks)
                    .setWeatherLocation(weatherLocation)
                    .create();
            if (listener != null) mWeatherUpdateRequestListeners.put(info.getKey(), listener);
            sWeatherManagerService.forceWeatherUpdate(info);
        } catch (RemoteException e) {
        }
    }

    /**
     * Request the active weather provider service to lookup the supplied city name.
     *
     * @param city The city name
     * @param listener {@link LookupCityRequestListener} To be notified once the request has been
     *                                                  completed. Upon success, a list of
     *                                                  {@link cyanogenmod.weather.WeatherLocation}
     *                                                  will be provided
     */
    public void lookupCity(@NonNull String city, @NonNull LookupCityRequestListener listener) {
        if (sWeatherManagerService == null) {
            return;
        }
        try {
            RequestInfo info = new RequestInfo
                    .Builder(mWeatherManagerCallbacks)
                    .setCityName(city)
                    .create();
            if (listener != null) mLookupNameRequestListeners.put(info.getKey(), listener);
            sWeatherManagerService.lookupCity(info);
        } catch (RemoteException e) {
        }
    }

    private IRequestInfoListener.Stub mWeatherManagerCallbacks
            = new IRequestInfoListener.Stub() {

        @Override
        public void onWeatherRequestCompleted(final RequestInfo requestInfo, final int state,
                final WeatherInfo weatherInfo) {
            final WeatherUpdateRequestListener listener
                    = mWeatherUpdateRequestListeners.remove(requestInfo.getKey());
            if (listener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onWeatherRequestCompleted(state, weatherInfo);
                    }
                });
            }
        }

        @Override
        public void onLookupCityRequestCompleted(RequestInfo requestInfo,
            final List<WeatherLocation> weatherLocations) {

            final LookupCityRequestListener listener
                    = mLookupNameRequestListeners.remove(requestInfo.getKey());
            if (listener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onLookupCityRequestCompleted(new ArrayList<>(weatherLocations));
                    }
                });
            }
        }
    };

    /**
     * Interface used to receive notifications upon completion of a weather update request
     */
    public interface WeatherUpdateRequestListener {
        /**
         * This method will be called when the weather service provider has finished processing the
         * request
         *
         * @param state Any of the following values
         *              {@link #WEATHER_REQUEST_COMPLETED}
         *              {@link #WEATHER_REQUEST_ALREADY_IN_PROGRESS}
         *              {@link #WEATHER_REQUEST_SUBMITTED_TOO_SOON}
         *              {@link #WEATHER_REQUEST_FAILED}
         *
         * @param weatherInfo A fully populated {@link WeatherInfo} if state is
         *                    {@link #WEATHER_REQUEST_COMPLETED}, null otherwise
         */
        void onWeatherRequestCompleted(int state, WeatherInfo weatherInfo);
    }

    /**
     * Interface used to receive notifications upon completion of a request to lookup a city name
     */
    public interface LookupCityRequestListener {
        /**
         * This method will be called when the weather service provider has finished processing the
         * request
         *
         * @param locations
         */
        void onLookupCityRequestCompleted(ArrayList<WeatherLocation> locations);
    }
}
