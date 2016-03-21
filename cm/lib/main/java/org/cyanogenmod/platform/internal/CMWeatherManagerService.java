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
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.WeatherContract.WeatherColumns;
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.ICMWeatherManager;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weatherservice.IWeatherProviderService;
import cyanogenmod.weatherservice.IWeatherProviderServiceClient;
import cyanogenmod.weatherservice.WeatherProviderService;

import java.util.ArrayList;
import java.util.List;

public class CMWeatherManagerService extends SystemService{

    private static final String TAG = CMWeatherManagerService.class.getSimpleName();
    /**
     * How long clients will have to wait until a new weather update request can be honored
     * TODO Allow weather service providers to specify this threshold
     */
    private static final long REQUEST_THRESHOLD_MILLIS = 1000L * 60L;

    private IWeatherProviderService mWeatherProviderService;
    private boolean mIsWeatherProviderServiceBound;
    private long mLastWeatherUpdateRequestTimestamp = 0;
    private boolean mIsProcessingRequest = false;
    private Object mMutex = new Object();
    private Context mContext;

    private final IWeatherProviderServiceClient mServiceClient
            = new IWeatherProviderServiceClient.Stub() {
        @Override
        public void setWeatherRequestState(RequestInfo requestInfo, WeatherInfo weatherInfo,
                                                int state) {
            synchronized (mMutex) {
                final long identity = Binder.clearCallingIdentity();
                try {
                    boolean providerUpdated = false;
                    Log.d(TAG, "Updating state for request [" + requestInfo + "] State: " + state);
                    if (state == RequestInfo.WEATHER_REQUEST_COMPLETED && weatherInfo == null) {
                        //This should never happen! Weather Provider Services should set
                        //WEATHER_REQUEST_COMPLETED only if the update request was successful
                        //This is just defensive code to prevent undesired behavior and avoid
                        //corrupting the content provider
                        state = RequestInfo.WEATHER_REQUEST_FAILED;
                    }
                    if (state == RequestInfo.WEATHER_REQUEST_COMPLETED) {
                        providerUpdated = updateWeatherInfoLocked(weatherInfo);
                        if (providerUpdated) {
                            notifyRequestResultLocked(state, requestInfo.getPackageName(),
                                    requestInfo.getUserHandle());
                        }
                    }
                    if (providerUpdated) {
                        //Send broadcast to registered receivers
                        //TODO Don't send broadcast to the process that submitted the request
                        //That process already received a "request completed" notification
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

    private final IBinder mService = new ICMWeatherManager.Stub() {

        @Override
        public void forceWeatherUpdate(RequestInfo info) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.READ_WEATHER, null);
            processWeatherUpdateRequest(info);
        }

    };

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
            Log.d(TAG, "Binding to active provider service " + activeProviderService);
            if (!getContext().bindServiceAsUser(new Intent().setComponent(
                    ComponentName.unflattenFromString(activeProviderService)),
                    mWeatherServiceProviderConnection, Context.BIND_AUTO_CREATE,
                    UserHandle.CURRENT)) {
                Log.w(TAG, "Failed to bind service " + activeProviderService);
            }
        }
    }

    private synchronized void processWeatherUpdateRequest(RequestInfo info) {
        final long currentTimeMillis = SystemClock.elapsedRealtime();

        if(((mLastWeatherUpdateRequestTimestamp + REQUEST_THRESHOLD_MILLIS) > currentTimeMillis)) {
            Log.d(TAG, "Force weather update request not honored. Last Request time ["
                    + mLastWeatherUpdateRequestTimestamp + "]");
            Intent intent = new Intent(CMWeatherManager.WEATHER_REQUEST_STATE_CHANGED_ACTION);
            intent.putExtra(CMWeatherManager.EXTRA_WEATHER_UPDATE_RESULT,
                    RequestInfo.WEATHER_REQUEST_SUBMITTED_TOO_SOON);
            intent.setPackage(info.getPackageName());
            mContext.sendBroadcastAsUser(intent, info.getUserHandle());
            return;
        }

        if (mIsProcessingRequest) {
            Log.d(TAG, "Force weather update request not honored. Request in progress");
            Intent intent = new Intent(CMWeatherManager.WEATHER_REQUEST_STATE_CHANGED_ACTION);
            intent.putExtra(CMWeatherManager.EXTRA_WEATHER_UPDATE_RESULT,
                    RequestInfo.WEATHER_REQUEST_ALREADY_IN_PROGRESS);
            intent.setPackage(info.getPackageName());
            mContext.sendBroadcastAsUser(intent, info.getUserHandle());
            return;
        }

        if (!mIsWeatherProviderServiceBound) {
            Log.d(TAG, "WeatherProviderService not bound");
            return;
        }

        mLastWeatherUpdateRequestTimestamp = currentTimeMillis;
        mIsProcessingRequest = true;
        try {
            mWeatherProviderService.processRequest(info);
        } catch (RemoteException e) {
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
            Log.d(TAG, "Weather Service Provider " + name.flattenToString()
                    + " successfully connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mWeatherProviderService = null;
            mIsWeatherProviderServiceBound = false;
            if (mIsProcessingRequest) {
                //We can't talk to the current service anyway...
                mIsProcessingRequest = false;
            }
            Log.d(TAG, "Connection with " + name.flattenToString() + " has been closed");
        }
    };

    private boolean updateWeatherInfoLocked(WeatherInfo wi) {
        Log.d(TAG, "Got new WeatherInfo " + wi.toString());
        int size = wi.getForecasts().size() + 1;
        Log.d(TAG, "Forecast days: " + (size - 1));
        List<ContentValues> contentValuesList = new ArrayList<>(size);
        ContentValues contentValues = new ContentValues();

        contentValues.put(WeatherColumns.CURRENT_CITY_ID, wi.getCityId());
        contentValues.put(WeatherColumns.CURRENT_CITY, wi.getCity());
        contentValues.put(WeatherColumns.CURRENT_CONDITION, wi.getCondition());
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
            contentValues.put(WeatherColumns.FORECAST_CONDITION, df.getCondition());
            contentValues.put(WeatherColumns.FORECAST_CONDITION_CODE, df.getConditionCode());
            contentValuesList.add(contentValues);
        }

        ContentValues[] updateValues = new ContentValues[contentValuesList.size()];
        if (size != getContext().getContentResolver().bulkInsert(
                WeatherColumns.CURRENT_AND_FORECAST_WEATHER_URI,
                contentValuesList.toArray(updateValues))) {
            Log.w(TAG, "Failed to update the weather provider");
            return false;
        }
        Log.d(TAG, "Content provider has been updated!");
        return true;
    }

    private void notifyRequestResultLocked(int state, String pkgName, UserHandle userHandle) {
        Intent intent = new Intent(CMWeatherManager.WEATHER_REQUEST_STATE_CHANGED_ACTION);
        intent.putExtra(CMWeatherManager.EXTRA_WEATHER_UPDATE_RESULT, state);
        intent.setPackage(pkgName);
        Log.d(TAG, "broadcasting " + CMWeatherManager.WEATHER_REQUEST_STATE_CHANGED_ACTION
            + " to package " + pkgName + " user handle "  + userHandle.toString());
        mContext.sendBroadcastAsUser(intent, userHandle);
    }

    private void registerPackageMonitor() {
        PackageMonitor monitor = new PackageMonitor() {
            @Override
            public void onPackageModified(String packageName) {
                Log.d(TAG, "onPackageChanged package name " +packageName);
                String enabledProviderService = CMSettings.Secure.getString(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                if (enabledProviderService == null) return;
                ComponentName cn = ComponentName.unflattenFromString(enabledProviderService);
                if (!TextUtils.equals(packageName, cn.getPackageName())) return;

                try {
                    final IPackageManager pm = AppGlobals.getPackageManager();
                    final int enabled = pm.getApplicationEnabledSetting(packageName,
                            getChangingUserId());
                    if (enabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            || enabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "Exception trying to look up app enabled setting", e);
                } catch (RemoteException e) {
                    // Really?
                }
                disconnectClient();
                CMSettings.Secure.putStringForUser(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        null, getChangingUserId());
            }

            @Override
            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                Log.d(TAG, "onPackageChanged package name " + packageName + " user " + uid);
                for (String component : components) {
                    Log.d(TAG, "    Component " + component);
                }
                //TODO Rebind the service
                return false;
            }

            @Override
            public void onPackageRemoved(String packageName, int uid) {
                Log.d(TAG, "onPackageRemoved package name " + packageName + " user " + uid);
                String enabledProviderService = CMSettings.Secure.getString(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                if (enabledProviderService == null) return;

                ComponentName cn = ComponentName.unflattenFromString(enabledProviderService);
                if (!TextUtils.equals(packageName, cn.getPackageName())) return;

                disconnectClient();
                CMSettings.Secure.putStringForUser(
                        mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        null, getChangingUserId());
            }

            @Override
            public void onPackageAdded(String packageName, int uid) {
                Log.d(TAG, "onPackageAdded package name " + packageName + " user " + uid);
                Intent intent = new Intent(WeatherProviderService.SERVICE_INTERFACE);
                intent.setPackage(packageName);

                final List<ResolveInfo> installedServices = mContext.getPackageManager()
                        .queryIntentServicesAsUser(intent, PackageManager.GET_SERVICES,
                                getChangingUserId());

                for (ResolveInfo resolveInfo : installedServices) {
                    ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                    ComponentName cn = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    Log.d(TAG, "New component added: " + cn.flattenToString());
                }
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
                Log.d(TAG, "Weather provider service changed! " + uri.toString()
                        + " by user " + userId);
                if (enabledWeatherProviderServiceUri.equals(uri)) {
                    String activeSrvc = CMSettings.Secure.getString(mContext.getContentResolver(),
                            CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
                    disconnectClient();
                    if (activeSrvc != null) {
                        ComponentName cn = ComponentName.unflattenFromString(activeSrvc);
                        getContext().bindServiceAsUser(new Intent().setComponent(cn),
                                mWeatherServiceProviderConnection, Context.BIND_AUTO_CREATE,
                                new UserHandle(userId));
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
                // TODO Cancel ongoing request before we disconnect
                mIsProcessingRequest = false;
            }

            try {
                mWeatherProviderService.setServiceClient(null);
            } catch (RemoteException e) {
            }

            Log.d(TAG, "Unbinding from current service...");
            getContext().unbindService(mWeatherServiceProviderConnection);
            mIsWeatherProviderServiceBound = false;
        }
    }
}
