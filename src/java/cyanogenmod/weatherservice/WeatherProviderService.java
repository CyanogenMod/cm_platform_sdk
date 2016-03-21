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

import android.annotation.SdkConstant;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import cyanogenmod.weather.RequestInfo;

/**
 * This is the base class for implementing a weather provider service. A weather provider service
 * can handle weather update request and update the weather content provider data by processing
 * {@link cyanogenmod.weatherservice.WeatherUpdateRequest}
 */
public abstract class WeatherProviderService extends Service {

    private Handler mHandler;
    private IWeatherProviderServiceClient mClient;

    /**
     * The {@link android.content.Intent} action that must be declared as handled by a service in
     * its manifest for the system to recognize it as a weather provider service
     */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE
            = "cyanogenmod.weatherservice.WeatherProviderService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    protected final void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mHandler = new ServiceHandler(base.getMainLooper());
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IWeatherProviderService.Stub mBinder = new IWeatherProviderService.Stub() {

        @Override
        public void processRequest(final RequestInfo info) {
            mHandler.obtainMessage(ServiceHandler.MSG_ON_NEW_REQUEST, info).sendToTarget();
        }

        @Override
        public void setServiceClient(IWeatherProviderServiceClient client) {
            mHandler.obtainMessage(ServiceHandler.MSG_SET_CLIENT, client).sendToTarget();
        }
    };

    private class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }
        public static final int MSG_SET_CLIENT = 1;
        public static final int MSG_ON_NEW_REQUEST = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_CLIENT: {
                    mClient = (IWeatherProviderServiceClient) msg.obj;
                    if (mClient != null) {
                        onConnected();
                    } else {
                        onDisconnected();
                    }
                    return;
                }
                case MSG_ON_NEW_REQUEST: {
                    RequestInfo info = (RequestInfo) msg.obj;
                    if (info != null) {
                        onWeatherUpdateRequested(new WeatherUpdateRequest(info, mClient));
                    }
                    return;
                }
            }
        }
    }

    /**
     * The system has connected to this service.
     */
    protected void onConnected() {
        /* Do nothing */
    }

    /**
     * The system has disconnected from this service.
     */
    protected void onDisconnected() {
        /* Do nothing */
    }

    /**
     * A new weather update request has been submitted to this service
     * @param request The update request
     * @see {@link cyanogenmod.weatherservice.WeatherUpdateRequest}
     */
    public abstract void onWeatherUpdateRequested(WeatherUpdateRequest request);
}