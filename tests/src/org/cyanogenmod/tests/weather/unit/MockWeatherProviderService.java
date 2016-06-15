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

package org.cyanogenmod.tests.weather.unit;

import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.WeatherProviderService;
import org.mockito.Mockito;

public class MockWeatherProviderService extends WeatherProviderService {

    private MockWeatherProviderService mCallTracker;

    public MockWeatherProviderService() {
        mCallTracker = Mockito.mock(MockWeatherProviderService.class);
    }

    public MockWeatherProviderService getCallTracker() {
        return mCallTracker;
    }

    @Override
    protected void onConnected() {
        mCallTracker.onConnected();
    }

    @Override
    protected void onDisconnected() {
        mCallTracker.onDisconnected();
    }

    @Override
    protected void onRequestSubmitted(ServiceRequest request) {
        mCallTracker.onRequestSubmitted(request);
    }

    @Override
    protected void onRequestCancelled(ServiceRequest request) {
        mCallTracker.onRequestCancelled(request);
    }
}
