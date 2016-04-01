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
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.RequestInfo;

/**
 * This class represents a request submitted by the system to the active weather provider service
 */
public final class ServiceRequest {

    private final RequestInfo mInfo;
    private final IWeatherProviderServiceClient mClient;

    /**
     * If a request is marked as cancelled, it means the client does not want to know anything about
     * this request anymore
     */
    private volatile boolean mCancelled;

    /* package */ ServiceRequest(RequestInfo info, IWeatherProviderServiceClient client) {
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
    public void complete(@NonNull ServiceRequestResult result) {
        if (!mCancelled) {
            try {
                final int requestType = mInfo.getRequestType();
                switch (requestType) {
                    case RequestInfo.TYPE_GEO_LOCATION_REQ:
                    case RequestInfo.TYPE_WEATHER_LOCATION_REQ:
                        if (result.getWeatherInfo() == null) {
                            throw new IllegalStateException("The service request result does not"
                             + " contain a valid WeatherInfo object");
                        }
                        mClient.setServiceRequestState(mInfo, result,
                                CMWeatherManager.WEATHER_REQUEST_COMPLETED);
                        break;
                    case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                        if (result.getLocationLookupList() == null) {
                            //In case the user decided to mark this request as completed with an
                            //empty list. It's not necessarily a failure
                            mClient.setServiceRequestState(mInfo, null,
                                    CMWeatherManager.LOOKUP_REQUEST_NO_MATCH_FOUND);
                        } else {
                            mClient.setServiceRequestState(mInfo, result,
                                    CMWeatherManager.LOOKUP_REQUEST_COMPLETED);
                        }
                        break;
                }
            } catch (RemoteException e) {
            }
        }
    }

    /**
     * This method should be called if the service failed to process the request
     * (no internet connection, time out, etc.)
     */
    public void fail() {
        if (!mCancelled) {
            try {
                final int requestType = mInfo.getRequestType();
                switch (requestType) {
                    case RequestInfo.TYPE_GEO_LOCATION_REQ:
                    case RequestInfo.TYPE_WEATHER_LOCATION_REQ:
                        mClient.setServiceRequestState(mInfo, null,
                                CMWeatherManager.WEATHER_REQUEST_FAILED);
                        break;
                    case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                        mClient.setServiceRequestState(mInfo, null,
                                CMWeatherManager.LOOKUP_REQUEST_FAILED);
                        break;
                }
            } catch (RemoteException e) {
            }
        }
    }

    /**
     * Called by the WeatherProviderService base class to notify we don't want this request anymore.
     * The service implementing the WeatherProviderService will be notified of this action
     * via onRequestCancelled()
     * @hide
     */
    public void cancel() {
        mCancelled = true;
    }
}
