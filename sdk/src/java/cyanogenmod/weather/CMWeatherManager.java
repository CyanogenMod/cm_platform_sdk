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
import android.content.ComponentName;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.WeatherContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to the weather services in the device.
 */
public class CMWeatherManager {

    private static ICMWeatherManager sWeatherManagerService;
    private static CMWeatherManager sInstance;
    private Context mContext;
    private Map<RequestInfo,WeatherUpdateRequestListener> mWeatherUpdateRequestListeners
            = Collections.synchronizedMap(new HashMap<RequestInfo,WeatherUpdateRequestListener>());
    private Map<RequestInfo,LookupCityRequestListener> mLookupNameRequestListeners
            = Collections.synchronizedMap(new HashMap<RequestInfo,LookupCityRequestListener>());
    private Handler mHandler;
    private Set<WeatherServiceProviderChangeListener> mProviderChangedListeners = new ArraySet<>();

    private static final String TAG = CMWeatherManager.class.getSimpleName();


    /**
     * The different request statuses
     */
    public static final class RequestStatus {

        private RequestStatus() {}

        /**
         * Request successfully completed
         */
        public static final int COMPLETED = 1;
        /**
         * An error occurred while trying to honor the request
         */
        public static final int FAILED = -1;
        /**
         * The request can't be processed at this time
         */
        public static final int SUBMITTED_TOO_SOON = -2;
        /**
         * Another request is already in progress
         */
        public static final int ALREADY_IN_PROGRESS = -3;
        /**
         * No match found for the query
         */
        public static final int NO_MATCH_FOUND = -4;
    }

    private CMWeatherManager(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = (appContext != null) ? appContext : context;
        sWeatherManagerService = getService();

        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.WEATHER_SERVICES) && (sWeatherManagerService == null)) {
            Log.wtf(TAG, "Unable to bind the CMWeatherManagerService");
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
     * @return An integer that identifies the request submitted to the weather service
     * Note that this method might return -1 if an error occurred while trying to submit
     * the request.
     */
    public int requestWeatherUpdate(@NonNull Location location,
            @NonNull WeatherUpdateRequestListener listener) {
        if (sWeatherManagerService == null) {
            return -1;
        }

        try {
            int tempUnit = CMSettings.Global.getInt(mContext.getContentResolver(),
                    CMSettings.Global.WEATHER_TEMPERATURE_UNIT,
                        WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT);

            RequestInfo info = new RequestInfo
                    .Builder(mRequestInfoListener)
                    .setLocation(location)
                    .setTemperatureUnit(tempUnit)
                    .build();
            if (listener != null) mWeatherUpdateRequestListeners.put(info, listener);
            sWeatherManagerService.updateWeather(info);
            return info.hashCode();
        } catch (RemoteException e) {
            return -1;
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
     * @return An integer that identifies the request submitted to the weather service.
     * Note that this method might return -1 if an error occurred while trying to submit
     * the request.
     */
    public int requestWeatherUpdate(@NonNull WeatherLocation weatherLocation,
            @NonNull WeatherUpdateRequestListener listener) {
        if (sWeatherManagerService == null) {
            return -1;
        }

        try {
            int tempUnit = CMSettings.Global.getInt(mContext.getContentResolver(),
                    CMSettings.Global.WEATHER_TEMPERATURE_UNIT,
                        WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT);

            RequestInfo info = new RequestInfo
                    .Builder(mRequestInfoListener)
                    .setWeatherLocation(weatherLocation)
                    .setTemperatureUnit(tempUnit)
                    .build();
            if (listener != null) mWeatherUpdateRequestListeners.put(info, listener);
            sWeatherManagerService.updateWeather(info);
            return info.hashCode();
        } catch (RemoteException e) {
            return -1;
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
     * @return An integer that identifies the request submitted to the weather service.
     * Note that this method might return -1 if an error occurred while trying to submit
     * the request.
     */
    public int lookupCity(@NonNull String city, @NonNull LookupCityRequestListener listener) {
        if (sWeatherManagerService == null) {
            return -1;
        }
        try {
            RequestInfo info = new RequestInfo
                    .Builder(mRequestInfoListener)
                    .setCityName(city)
                    .build();
            if (listener != null) mLookupNameRequestListeners.put(info, listener);
            sWeatherManagerService.lookupCity(info);
            return info.hashCode();
        } catch (RemoteException e) {
            return -1;
        }
    }

    /**
     * Cancels a request that was previously submitted to the weather service.
     * @param requestId The ID that you received when the request was submitted
     */
    public void cancelRequest(int requestId) {
        if (sWeatherManagerService == null) {
            return;
        }

        try {
            sWeatherManagerService.cancelRequest(requestId);
        }catch (RemoteException e){
        }
    }

    /**
     * Registers a {@link WeatherServiceProviderChangeListener} to be notified when a new weather
     * service provider becomes active.
     * @param listener {@link WeatherServiceProviderChangeListener} to register
     */
    public void registerWeatherServiceProviderChangeListener(
            @NonNull WeatherServiceProviderChangeListener listener) {
        if (sWeatherManagerService == null) return;

        synchronized (mProviderChangedListeners) {
            if (mProviderChangedListeners.contains(listener)) {
                throw new IllegalArgumentException("Listener already registered");
            }
            if (mProviderChangedListeners.size() == 0) {
                try {
                    sWeatherManagerService.registerWeatherServiceProviderChangeListener(
                            mProviderChangeListener);
                } catch (RemoteException e){
                }
            }
            mProviderChangedListeners.add(listener);
        }
    }

    /**
     * Unregisters a listener
     * @param listener A previously registered {@link WeatherServiceProviderChangeListener}
     */
    public void unregisterWeatherServiceProviderChangeListener(
            @NonNull WeatherServiceProviderChangeListener listener) {
        if (sWeatherManagerService == null) return;

        synchronized (mProviderChangedListeners) {
            if (!mProviderChangedListeners.contains(listener)) {
                throw new IllegalArgumentException("Listener was never registered");
            }
            mProviderChangedListeners.remove(listener);
            if (mProviderChangedListeners.size() == 0) {
                try {
                    sWeatherManagerService.unregisterWeatherServiceProviderChangeListener(
                            mProviderChangeListener);
                } catch(RemoteException e){
                }
            }
        }
    }

    /**
     * Gets the service's label as declared by the active weather service provider in its manifest
     * @return the service's label
     */
    public String getActiveWeatherServiceProviderLabel() {
        if (sWeatherManagerService == null) return null;

        try {
            return sWeatherManagerService.getActiveWeatherServiceProviderLabel();
        } catch(RemoteException e){
        }
        return null;
    }

    private final IWeatherServiceProviderChangeListener mProviderChangeListener =
            new IWeatherServiceProviderChangeListener.Stub() {
        @Override
        public void onWeatherServiceProviderChanged(final String providerName) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mProviderChangedListeners) {
                        List<WeatherServiceProviderChangeListener> deadListeners
                                = new ArrayList<>();
                        for (WeatherServiceProviderChangeListener listener
                                : mProviderChangedListeners) {
                            try {
                                listener.onWeatherServiceProviderChanged(providerName);
                            } catch (Throwable e) {
                                deadListeners.add(listener);
                            }
                        }
                        if (deadListeners.size() > 0) {
                            for (WeatherServiceProviderChangeListener listener : deadListeners) {
                                mProviderChangedListeners.remove(listener);
                            }
                        }
                    }
                }
            });
        }
    };

    private final IRequestInfoListener mRequestInfoListener = new IRequestInfoListener.Stub() {

        @Override
        public void onWeatherRequestCompleted(final RequestInfo requestInfo, final int status,
                final WeatherInfo weatherInfo) {
            final WeatherUpdateRequestListener listener
                    = mWeatherUpdateRequestListeners.remove(requestInfo);
            if (listener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onWeatherRequestCompleted(status, weatherInfo);
                    }
                });
            }
        }

        @Override
        public void onLookupCityRequestCompleted(RequestInfo requestInfo, final int status,
            final List<WeatherLocation> weatherLocations) {

            final LookupCityRequestListener listener
                    = mLookupNameRequestListeners.remove(requestInfo);
            if (listener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onLookupCityRequestCompleted(status, weatherLocations);
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
         * @param status See {@link RequestStatus}
         *
         * @param weatherInfo A fully populated {@link WeatherInfo} if state is
         *                    {@link RequestStatus#COMPLETED}, null otherwise
         */
        void onWeatherRequestCompleted(int status, WeatherInfo weatherInfo);
    }

    /**
     * Interface used to receive notifications upon completion of a request to lookup a city name
     */
    public interface LookupCityRequestListener {
        /**
         * This method will be called when the weather service provider has finished processing the
         * request.
         *
         * @param status See {@link RequestStatus}
         *
         * @param locations A list of {@link WeatherLocation} if the status is
         * {@link RequestStatus#COMPLETED}, null otherwise
         */
        void onLookupCityRequestCompleted(int status, List<WeatherLocation> locations);
    }

    /**
     * Interface used to be notified when the user changes the weather service provider
     */
    public interface WeatherServiceProviderChangeListener {
        /**
         * This method will be called when a new weather service provider becomes active in the
         * system. The parameter can be null when
         * <p>The user removed the active weather service provider from the system </p>
         * <p>The active weather provider was disabled.</p>
         *
         * @param providerLabel The label as declared on the weather service provider manifest
         */
        void onWeatherServiceProviderChanged(String providerLabel);
    }
}
