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

package cyanogenmod.weatherservice;

import android.annotation.NonNull;
import android.os.RemoteException;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;

/**
 * This class represents a weather update request from the weather service perspective
 */
public final class WeatherUpdateRequest {

    private final RequestInfo mInfo;
    private final IWeatherProviderServiceClient mClient;

    /* package */ WeatherUpdateRequest(RequestInfo info, IWeatherProviderServiceClient client) {
        mInfo = info;
        mClient = client;
    }

    /**
     * Obtains the request information
     * @return {@link cyanogenmod.weather.RequestInfo}
     */
    public RequestInfo getRequestInfo() {
        return mInfo;
    }

    /**
     * This method should be called once the request has been completed
     */
    public void complete(@NonNull WeatherInfo weatherInfo) {
        try {
            mClient.setWeatherRequestState(mInfo, weatherInfo,
                    RequestInfo.WEATHER_REQUEST_COMPLETED);
        }catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method should be called if the service failed to process the request
     * (no internet connection, time out, etc.)
     */
    public void fail() {
        try {
            mClient.setWeatherRequestState(mInfo, null, RequestInfo.WEATHER_REQUEST_FAILED);
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }
}
