/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.ServiceWatcher;

import cyanogenmod.app.suggest.ApplicationSuggestion;
import cyanogenmod.app.suggest.IAppSuggestProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
public class AppSuggestProviderProxy implements AppSuggestProviderInterface {
    private static final String TAG = AppSuggestProviderProxy.class.getSimpleName();
    private static final boolean DEBUG = AppSuggestManagerService.DEBUG;

    public static AppSuggestProviderProxy createAndBind(
            Context context, String name, String action,
            int overlaySwitchResId, int defaultServicePackageNameResId,
            int initialPackageNamesResId) {
        AppSuggestProviderProxy proxy = new AppSuggestProviderProxy(context, name, action,
                overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId);
        if (proxy.bind()) {
            return proxy;
        } else {
            return null;
        }
    }

    private Context mContext;
    private ServiceWatcher mServiceWatcher;

    private AppSuggestProviderProxy(Context context, String name, String action,
            int overlaySwitchResId, int defaultServicePackageNameResId,
            int initialPackageNamesResId) {
        mContext = context;
        mServiceWatcher = new ServiceWatcher(mContext, TAG + "-" + name, action, overlaySwitchResId,
                defaultServicePackageNameResId, initialPackageNamesResId, null, null);
    }

    private boolean bind() {
        return mServiceWatcher.start();
    }

    private IAppSuggestProvider getService() {
        return IAppSuggestProvider.Stub.asInterface(mServiceWatcher.getBinder());
    }

    @Override
    public boolean handles(Intent intent) {
        IAppSuggestProvider service = getService();
        if (service == null) return false;

        try {
            return service.handles(intent);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        } catch (Exception e) {
            // never let remote service crash system server
            Log.e(TAG, "Exception from " + mServiceWatcher.getBestPackageName(), e);
        }
        return false;
    }

    @Override
    public List<ApplicationSuggestion> getSuggestions(Intent intent) {
        IAppSuggestProvider service = getService();
        if (service == null) return new ArrayList<>(0);

        try {
            return service.getSuggestions(intent);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        } catch (Exception e) {
            // never let remote service crash system server
            Log.e(TAG, "Exception from " + mServiceWatcher.getBestPackageName(), e);
        }
        return new ArrayList<>(0);
    }
}
