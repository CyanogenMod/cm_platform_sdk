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

import android.location.Location;
import android.os.IBinder;
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherLocation;
import cyanogenmod.weatherservice.IWeatherProviderService;
import cyanogenmod.weatherservice.IWeatherProviderServiceClient;
import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.ServiceRequestResult;
import org.cyanogenmod.tests.common.MockIBinderStubForInterface;
import org.cyanogenmod.tests.common.ThreadServiceTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class WeatherProviderServiceTest extends ThreadServiceTestCase<MockWeatherProviderService> {

    public WeatherProviderServiceTest() {
        super(MockWeatherProviderService.class);
    }

    private static final String CITY_NAME = "Seattle";
    private static final int TIMEOUT = 5000;

    public void testCityNameLookupRequest() throws Exception {
        IBinder binder = bindService((ServiceRunnable) null);
        assertNotNull(binder);

        final IWeatherProviderService provider = IWeatherProviderService.Stub.asInterface(binder);
        assertNotNull(provider);

        provider.processCityNameLookupRequest(
                buildMockdRequestInfo(RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ));
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<ServiceRequest> params
                        = ArgumentCaptor.forClass(ServiceRequest.class);

                Mockito.verify(getService().getCallTracker(), Mockito.timeout(TIMEOUT).times(1))
                        .onRequestSubmitted(params.capture());

                ServiceRequest request = params.getValue();
                assertEquals(request.getRequestInfo().getRequestType(),
                        RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ);

                assertEquals(request.getRequestInfo().getCityName(), CITY_NAME);
            }
        });
    }

    public void testWeatherUpdateRequestByWeatherLocation() throws Exception {
        IBinder binder = bindService((ServiceRunnable) null);
        assertNotNull(binder);

        final IWeatherProviderService provider = IWeatherProviderService.Stub.asInterface(binder);
        assertNotNull(provider);

        provider.processWeatherUpdateRequest(
                buildMockdRequestInfo(RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ));
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<ServiceRequest> params
                        = ArgumentCaptor.forClass(ServiceRequest.class);

                Mockito.verify(getService().getCallTracker(), Mockito.timeout(TIMEOUT).times(1))
                        .onRequestSubmitted(params.capture());

                ServiceRequest request = params.getValue();
                assertEquals(request.getRequestInfo().getRequestType(),
                        RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ);

                WeatherLocation weatherLocation = request.getRequestInfo().getWeatherLocation();
                assertNotNull(weatherLocation);
                assertEquals(weatherLocation.getCity(), CITY_NAME);
            }
        });
    }

    public void testWeatherUpdateRequestByGeoLocation() throws Exception {
        IBinder binder = bindService((ServiceRunnable) null);
        assertNotNull(binder);

        final IWeatherProviderService provider = IWeatherProviderService.Stub.asInterface(binder);
        assertNotNull(provider);

        provider.processWeatherUpdateRequest(
                buildMockdRequestInfo(RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ));
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<ServiceRequest> params
                        = ArgumentCaptor.forClass(ServiceRequest.class);

                Mockito.verify(getService().getCallTracker(), Mockito.timeout(TIMEOUT).times(1))
                        .onRequestSubmitted(params.capture());

                ServiceRequest request = params.getValue();
                assertEquals(request.getRequestInfo().getRequestType(),
                        RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ);

                Location location = request.getRequestInfo().getLocation();
                assertNotNull(location);
            }
        });
    }

    public void testServiceRequestResult() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        IBinder binder = bindService((ServiceRunnable) null);
        assertNotNull(binder);

        final IWeatherProviderService provider = IWeatherProviderService.Stub.asInterface(binder);
        assertNotNull(provider);

        IWeatherProviderServiceClient client =
                MockIBinderStubForInterface.getMockInterface(
                        IWeatherProviderServiceClient.Stub.class);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                RequestInfo requestInfo = (RequestInfo) invocation.getArguments()[0];
                int result = (int) invocation.getArguments()[2];

                assertNotNull(requestInfo);
                assertEquals(result, CMWeatherManager.RequestStatus.FAILED);

                latch.countDown();
                return null;
            }
        }).when(client)
                .setServiceRequestState(Mockito.any(RequestInfo.class),
                        Mockito.any(ServiceRequestResult.class),
                                Mockito.eq(CMWeatherManager.RequestStatus.FAILED));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                RequestInfo requestInfo = (RequestInfo) invocation.getArguments()[0];
                int result = (int) invocation.getArguments()[2];

                assertNotNull(requestInfo);
                assertEquals(result, CMWeatherManager.RequestStatus.SUBMITTED_TOO_SOON);

                latch.countDown();
                return null;
            }
        }).when(client)
                .setServiceRequestState(Mockito.any(RequestInfo.class),
                        Mockito.any(ServiceRequestResult.class),
                        Mockito.eq(CMWeatherManager.RequestStatus.SUBMITTED_TOO_SOON));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                RequestInfo requestInfo = (RequestInfo) invocation.getArguments()[0];
                ServiceRequestResult requestResult
                        = (ServiceRequestResult) invocation.getArguments()[1];
                int result = (int) invocation.getArguments()[2];

                assertNotNull(requestInfo);
                assertNotNull(requestResult);
                assertNotNull(requestResult.getLocationLookupList());
                assertEquals(result, CMWeatherManager.RequestStatus.COMPLETED);

                latch.countDown();
                return null;
            }
        }).when(client)
                .setServiceRequestState(Mockito.any(RequestInfo.class),
                        Mockito.any(ServiceRequestResult.class),
                        Mockito.eq(CMWeatherManager.RequestStatus.COMPLETED));

        provider.setServiceClient(client);

        final RequestInfo requestInfo
                = buildMockdRequestInfo(RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ);

        Mockito.reset(getService().getCallTracker());
        provider.processCityNameLookupRequest(requestInfo);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<ServiceRequest> params
                        = ArgumentCaptor.forClass(ServiceRequest.class);

                Mockito.verify(getService().getCallTracker(), Mockito.timeout(TIMEOUT).times(1))
                        .onRequestSubmitted(params.capture());

                ServiceRequest request = params.getValue();
                request.fail();
            }
        });

        Mockito.reset(getService().getCallTracker());
        provider.processCityNameLookupRequest(requestInfo);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<ServiceRequest> params
                        = ArgumentCaptor.forClass(ServiceRequest.class);

                Mockito.verify(getService().getCallTracker(), Mockito.timeout(TIMEOUT).times(1))
                        .onRequestSubmitted(params.capture());

                ServiceRequest request = params.getValue();
                request.reject(CMWeatherManager.RequestStatus.SUBMITTED_TOO_SOON);
            }
        });

        Mockito.reset(getService().getCallTracker());
        provider.processCityNameLookupRequest(requestInfo);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<ServiceRequest> params
                        = ArgumentCaptor.forClass(ServiceRequest.class);

                Mockito.verify(getService().getCallTracker(), Mockito.timeout(TIMEOUT).times(1))
                        .onRequestSubmitted(params.capture());

                ServiceRequest request = params.getValue();

                List<WeatherLocation> locations = new ArrayList<>();
                locations.add(new WeatherLocation.Builder(CITY_NAME).build());
                ServiceRequestResult result = new ServiceRequestResult.Builder(locations).build();
                request.complete(result);
            }
        });

        try  {
            latch.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

    }

    private RequestInfo buildMockdRequestInfo(int requestType) {
        try {
            Constructor<RequestInfo> c = RequestInfo.class.getDeclaredConstructor();
            c.setAccessible(true);
            RequestInfo info = c.newInstance();
            Field type;
            type = info.getClass().getDeclaredField("mRequestType");
            type.setAccessible(true);
            type.set(info, requestType);
            switch (requestType) {
                case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                    Field cityName;
                    cityName = info.getClass().getDeclaredField("mCityName");
                    cityName.setAccessible(true);
                    cityName.set(info, CITY_NAME);
                    break;
                case RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ:
                    Field weatherLocation;
                    weatherLocation = info.getClass().getDeclaredField("mWeatherLocation");
                    weatherLocation.setAccessible(true);
                    weatherLocation.set(info, new WeatherLocation.Builder(CITY_NAME).build());
                    break;
                case RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ:
                    Field location;
                    location = info.getClass().getDeclaredField("mLocation");
                    location.setAccessible(true);
                    location.set(info, new Location("dummy_location_provider"));
                    break;
                default:
                    throw new AssertionError("Unknown request type");
            }
            return info;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
