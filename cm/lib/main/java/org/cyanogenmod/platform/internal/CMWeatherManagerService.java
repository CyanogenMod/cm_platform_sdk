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

package org.cyanogenmod.platform.internal;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.platform.Manifest;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.WeatherContract.WeatherColumns;
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.ICMWeatherManager;
import cyanogenmod.weather.IRequestInfoListener;
import cyanogenmod.weather.IWeatherServiceProviderChangeListener;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weatherservice.IWeatherProviderService;
import cyanogenmod.weatherservice.IWeatherProviderServiceClient;
import cyanogenmod.weatherservice.ServiceRequestResult;

import java.util.ArrayList;
import java.util.List;

public class CMWeatherManagerService extends SystemService{

    private static final String TAG = CMWeatherManagerService.class.getSimpleName();
    /**
     * How long clients will have to wait until a new weather update request can be honored
     * TODO Allow weather service providers to specify this threshold
     */
    private static final long REQUEST_THRESHOLD_MILLIS = 1000L * 60L * 10L;

    private IWeatherProviderService mWeatherProviderService;
    private boolean mIsWeatherProviderServiceBound;
    private long mLastWeatherUpdateRequestTimestamp = -REQUEST_THRESHOLD_MILLIS;
    private boolean mIsProcessingRequest = false;
    private Object mMutex = new Object();
    private Context mContext;
    private final RemoteCallbackList<IWeatherServiceProviderChangeListener> mProviderChangeListeners
            = new RemoteCallbackList<>();
    private volatile boolean mReconnectedDuePkgModified = false;

    private final IWeatherProviderServiceClient mServiceClient
            = new IWeatherProviderServiceClient.Stub() {
        @Override
        public void setServiceRequestState(RequestInfo requestInfo,
                ServiceRequestResult result, int state) {
            synchronized (mMutex) {
                final long identity = Binder.clearCallingIdentity();
                try {
                    boolean providerUpdated = false;
                    WeatherInfo weatherInfo = null;

                    if (state == CMWeatherManager.WEATHER_REQUEST_COMPLETED) {
                        weatherInfo = (result != null) ? result.getWeatherInfo() : null;
                        if (weatherInfo == null) {
                            //This should never happen! Weather Provider Services should set
                            //WEATHER_REQUEST_COMPLETED only if the update request was successful
                            //This is just defensive code to prevent undesired behavior and avoid
                            //corrupting the content provider
                            state = CMWeatherManager.WEATHER_REQUEST_FAILED;
                        } else {
                            if (!requestInfo.isQueryOnlyWeatherRequest()) {
                                providerUpdated = updateWeatherInfoLocked(weatherInfo);
                            }
                        }
                    }
                    final IRequestInfoListener listener = requestInfo.getRequestListener();
                    final int requestType = requestInfo.getRequestType();
                    if (listener != null && listener.asBinder().pingBinder()) {
                        if (requestType == RequestInfo.TYPE_WEATHER_LOCATION_REQ
                                || requestType == RequestInfo.TYPE_GEO_LOCATION_REQ) {
                            try {
                                listener.onWeatherRequestCompleted(requestInfo, state, weatherInfo);
                            } catch (RemoteException e) {
                            }
                        } else if (requestType == RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ) {
                            try {
                                listener.onLookupCityRequestCompleted(requestInfo,
                                        result.getLocationLookupList());
                            } catch (RemoteException e) {
                            }
                        }
                    }

                    if (providerUpdated) {
                        //Send broadcast to registered receivers
                        Intent intent = new Intent(CMWeatherManager.WEATHER_UPDATED_ACTION);
                        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
                mIsProcessingRequest = false;
            }
        }
    };


    private void enforcePermission() {
        mContext.enforceCallingOrSelfPermission(
                Manifest.permission.ACCESS_WEATHER_MANAGER, null);
    }

    private final IBinder mService = new ICMWeatherManager.Stub() {

        @Override
        public void updateWeather(RequestInfo info) {
            enforcePermission();
            processWeatherUpdateRequest(info);
        }

        @Override
        public void lookupCity(RequestInfo info) {
            enforcePermission();
            processCityNameLookupRequest(info);
        }

        @Override
        public void registerWeatherServiceProviderChangeListener(
                IWeatherServiceProviderChangeListener listener) {
            enforcePermission();
            mProviderChangeListeners.register(listener);
        }

        @Override
        public void unregisterWeatherServiceProviderChangeListener(
                IWeatherServiceProviderChangeListener listener) {
            enforcePermission();
            mProviderChangeListeners.unregister(listener);
        }

        @Override
        public String getActiveWeatherServiceProviderLabel() {
            enforcePermission();
            final long identity = Binder.clearCallingIdentity();
            try {
                return getProviderLabel();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    };

    private String getProviderLabel() {
        String enabledProviderService = CMSettings.Secure.getString(
                    mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
        if (enabledProviderService != null) {
            final PackageManager pm = mContext.getPackageManager();
            Intent intent = new Intent().setComponent(ComponentName.unflattenFromString(
                    enabledProviderService));
            List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent,
                    PackageManager.GET_SERVICES);
            if (resolveInfos != null && resolveInfos.size() >= 1) {
                //We should get just one record anyway
                return resolveInfos.get(0).loadLabel(pm).toString();
            }
        }
        return enabledProviderService;
    }

    public CMWeatherManagerService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_WEATHER_SERVICE, mService);
        registerPackageMonitor();
        registerSettingsObserver();
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            bindActiveWeatherProviderService();
        }
    }

    private void bindActiveWeatherProviderService() {
        String activeProviderService = CMSettings.Secure.getString(mContext.getContentResolver(),
                CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
        if (activeProviderService != null) {
            if (!getContext().bindServiceAsUser(new Intent().setComponent(
                    ComponentName.unflattenFromString(activeProviderService)),
                    mWeatherServiceProviderConnection, Context.BIND_AUTO_CREATE,
                    UserHandle.CURRENT)) {
                Slog.w(TAG, "Failed to bind service " + activeProviderService);
            }
        }
    }

    private boolean canProcessWeatherUpdateRequest(RequestInfo info, long currentTimeMillis) {
        final IRequestInfoListener listener = info.getRequestListener();

        if ((mLastWeatherUpdateRequestTimestamp + REQUEST_THRESHOLD_MILLIS) > currentTimeMillis) {
            if (listener != null && listener.asBinder().pingBinder()) {
                try {
                    listener.onWeatherRequestCompleted(info,
                            CMWeatherManager.WEATHER_REQUEST_SUBMITTED_TOO_SOON, null);
                } catch (RemoteException e) {
                }
            }
            return false;
        }

        if (mIsProcessingRequest) {
            if (listener != null && listener.asBinder().pingBinder()) {
                try {
                    listener.onWeatherRequestCompleted(info,
                            CMWeatherManager.WEATHER_REQUEST_ALREADY_IN_PROGRESS, null);
                } catch (RemoteException e) {
                }
            }
            return false;
        }

        if (!mIsWeatherProviderServiceBound) {
            if (listener != null && listener.asBinder().pingBinder()) {
                try {
                    listener.onWeatherRequestCompleted(info,
                            CMWeatherManager.WEATHER_REQUEST_FAILED, null);
                } catch (RemoteException e) {
                }
            }
            return false;
        }
        return true;
    }

    private synchronized void processWeatherUpdateRequest(RequestInfo info) {
        final long currentTimeMillis = SystemClock.elapsedRealtime();

        if (!canProcessWeatherUpdateRequest(info, currentTimeMillis)) return;

        mLastWeatherUpdateRequestTimestamp = currentTimeMillis;
        mIsProcessingRequest = true;
        try {
            mWeatherProviderService.processWeatherUpdateRequest(info);
        } catch (RemoteException e) {
        }
    }

    private void processCityNameLookupRequest(RequestInfo info) {
        if (!mIsWeatherProviderServiceBound) {
            final IRequestInfoListener listener = info.getRequestListener();
            if (listener != null && listener.asBinder().pingBinder()) {
                try {
                    listener.onLookupCityRequestCompleted(info, null);
                } catch (RemoteException e) {
                }
            }
            return;
        }

        try {
            mWeatherProviderService.processCityNameLookupRequest(info);
        } catch(RemoteException e){
        }
    }

    private ServiceConnection mWeatherServiceProviderConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mWeatherProviderService = IWeatherProviderService.Stub.asInterface(service);
            mIsWeatherProviderServiceBound = true;
            try {
                mWeatherProviderService.setServiceClient(mServiceClient);
            } catch(RemoteException e) {
            }
            if (!mReconnectedDuePkgModified) {
                notifyProviderChanged(name);
            }
            mReconnectedDuePkgModified = false;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mWeatherProviderService = null;
            mIsWeatherProviderServiceBound = false;
            //We can't talk to the current service anyway...
            mIsProcessingRequest = false;
            Slog.d(TAG, "Connection with " + name.flattenToString() + " has been closed");
        }
    };

    private void notifyProviderChanged(ComponentName name) {
        int N = mProviderChangeListeners.beginBroadcast();
        for (int indx = 0; indx < N; indx++) {
            IWeatherServiceProviderChangeListener listener
                    = mProviderChangeListeners.getBroadcastItem(indx);
            try {
                listener.onWeatherServiceProviderChanged(name);
            } catch (RemoteException e){
            }
        }
        mProviderChangeListeners.finishBroadcast();
    }

    private boolean updateWeatherInfoLocked(WeatherInfo wi) {
        final int size = wi.getForecasts().size() + 1;
        List<ContentValues> contentValuesList = new ArrayList<>(size);
        ContentValues contentValues = new ContentValues();

        contentValues.put(WeatherColumns.CURRENT_CITY_ID, wi.getCityId());
        contentValues.put(WeatherColumns.CURRENT_CITY, wi.getCity());
        contentValues.put(WeatherColumns.CURRENT_CONDITION_CODE, wi.getConditionCode());
        contentValues.put(WeatherColumns.CURRENT_HUMIDITY, wi.getHumidity());
        contentValues.put(WeatherColumns.CURRENT_TEMPERATURE, wi.getTemperature());
        contentValues.put(WeatherColumns.CURRENT_TEMPERATURE_UNIT, wi.getTemperatureUnit());
        contentValues.put(WeatherColumns.CURRENT_TIMESTAMP, wi.getTimestamp());
        contentValues.put(WeatherColumns.CURRENT_WIND_DIRECTION, wi.getWindDirection());
        contentValues.put(WeatherColumns.CURRENT_WIND_SPEED, wi.getWindSpeed());
        contentValues.put(WeatherColumns.CURRENT_WIND_SPEED_UNIT, wi.getWindSpeedUnit());
        contentValuesList.add(contentValues);

        for (WeatherInfo.DayForecast df : wi.getForecasts()) {
            contentValues = new ContentValues();
            contentValues.put(WeatherColumns.FORECAST_LOW, df.getLow());
            contentValues.put(WeatherColumns.FORECAST_HIGH, df.getHigh());
            contentValues.put(WeatherColumns.FORECAST_CONDITION_CODE, df.getConditionCode());
            contentValuesList.add(contentValues);
        }

        ContentValues[] updateValues = new ContentValues[contentValuesList.size()];
        if (size != getContext().getContentResolver().bulkInsert(
                WeatherColumns.CURRENT_AND_FORECAST_WEATHER_URI,
                contentValuesList.toArray(updateValues))) {
            Slog.w(TAG, "Failed to update the weather content provider");
            return false;
        }
        return true;
    }

    private void registerPackageMonitor() {
        PackageMonitor monitor = new PackageMonitor() {
            @Override
            public void onPackageModified(String packageName) {
                String enabledProviderService = CMSettings.Secure.getString(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                if (enabledProviderService == null) return;
                ComponentName cn = ComponentName.unflattenFromString(enabledProviderService);
                if (!TextUtils.equals(packageName, cn.getPackageName())) return;

                if (cn.getPackageName().equals(packageName) && !mIsWeatherProviderServiceBound) {
                    //We were disconnected because the whole package changed
                    //(most likely remove->install)
                    if (!getContext().bindServiceAsUser(new Intent().setComponent(cn),
                            mWeatherServiceProviderConnection, Context.BIND_AUTO_CREATE,
                            UserHandle.CURRENT)) {
                        CMSettings.Secure.putStringForUser( mContext.getContentResolver(),
                                CMSettings.Secure.WEATHER_PROVIDER_SERVICE, null,
                                getChangingUserId());
                        Slog.w(TAG, "Unable to rebind " + cn.flattenToString() + " after receiving"
                                + " package modified notification. Settings updated.");
                    } else {
                        mReconnectedDuePkgModified = true;
                    }
                }
            }

            @Override
            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                String enabledProviderService = CMSettings.Secure.getString(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                if (enabledProviderService == null) return false;

                boolean packageChanged = false;
                ComponentName cn = ComponentName.unflattenFromString(enabledProviderService);
                for (String component : components) {
                    if (cn.getPackageName().equals(component)) {
                        packageChanged = true;
                        break;
                    }
                }

                if (packageChanged) {
                    try {
                        final IPackageManager pm = AppGlobals.getPackageManager();
                        final int enabled = pm.getApplicationEnabledSetting(packageName,
                                getChangingUserId());
                        if (enabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                || enabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                            return false;
                        } else {
                            disconnectClient();
                            //The package is not enabled so we can't use it anymore
                            CMSettings.Secure.putStringForUser(
                                    mContext.getContentResolver(),
                                    CMSettings.Secure.WEATHER_PROVIDER_SERVICE, null,
                                    getChangingUserId());
                            Slog.w(TAG, "Active provider " + cn.flattenToString() + " disabled");
                            notifyProviderChanged(null);
                        }
                    } catch (IllegalArgumentException e) {
                        Slog.d(TAG, "Exception trying to look up app enabled settings ", e);
                    } catch (RemoteException e) {
                        // Really?
                    }
                }
                return false;
            }

            @Override
            public void onPackageRemoved(String packageName, int uid) {
                String enabledProviderService = CMSettings.Secure.getString(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                if (enabledProviderService == null) return;

                ComponentName cn = ComponentName.unflattenFromString(enabledProviderService);
                if (!TextUtils.equals(packageName, cn.getPackageName())) return;

                disconnectClient();
                CMSettings.Secure.putStringForUser(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        null, getChangingUserId());
                notifyProviderChanged(null);
            }
        };

        monitor.register(mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
    }

    private void registerSettingsObserver() {
        final Uri enabledWeatherProviderServiceUri = CMSettings.Secure.getUriFor(
                CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
        ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if (enabledWeatherProviderServiceUri.equals(uri)) {
                    String activeSrvc = CMSettings.Secure.getString(mContext.getContentResolver(),
                            CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                    disconnectClient();
                    if (activeSrvc != null) {
                        ComponentName cn = ComponentName.unflattenFromString(activeSrvc);
                        getContext().bindServiceAsUser(new Intent().setComponent(cn),
                                mWeatherServiceProviderConnection, Context.BIND_AUTO_CREATE,
                                UserHandle.CURRENT);
                    }
                }
            }
        };
        mContext.getContentResolver().registerContentObserver(enabledWeatherProviderServiceUri,
                false, observer, UserHandle.USER_ALL);
    }

    private synchronized void disconnectClient() {
        if (mIsWeatherProviderServiceBound) {
            if (mIsProcessingRequest) {
                try {
                    mWeatherProviderService.cancelOngoingRequests();
                } catch (RemoteException e) {
                }
                mIsProcessingRequest = false;
            }
            try {
                mWeatherProviderService.setServiceClient(null);
            } catch (RemoteException e) {
            }

            getContext().unbindService(mWeatherServiceProviderConnection);
            mIsWeatherProviderServiceBound = false;
        }
    }
}
