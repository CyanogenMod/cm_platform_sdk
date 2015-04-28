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
import android.net.IConnectivityManager;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.net.GlobalProxyInfo;
import cyanogenmod.net.ICMProxyManager;

/**
 * This service behaves as a dummy wrapper for the internal ConnectivityService
 * @hide
 */
public class CMProxyManagerService extends SystemService {

    private static final String TAG = "CMConnectivityManagerService";

    private IConnectivityManager mConnectivityManager;

    public CMProxyManagerService(Context context) {
        super(context);
        mConnectivityManager = getConnectivityService();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "registerCMConnectivity cmconnectivity: " + this);
        publishBinderService(CMContextConstants.CM_PROXY_SERVICE, mService);
    }

    private final IBinder mService = new ICMProxyManager.Stub() {
        @Override
        public GlobalProxyInfo getGlobalProxy() {
            ProxyInfo proxyInfo;
            try {
                proxyInfo = mConnectivityManager.getGlobalProxy();
                return rewrap(proxyInfo);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to retrieve global proxy");
            }
            return null;
        }

        @Override
        public void setGlobalProxy(GlobalProxyInfo info) {
            try {
                mConnectivityManager.setGlobalProxy(rewrap(info));
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to set global proxy " + info.toString());
            }
        }

    };

    private ProxyInfo rewrap(GlobalProxyInfo info) {
        ProxyInfo proxyInfo = new ProxyInfo(info.getHost(),
                info.getPort(), info.getExclusionListAsString());
        return proxyInfo;
    }

    private GlobalProxyInfo rewrap(ProxyInfo info) {
        GlobalProxyInfo proxyInfo = new GlobalProxyInfo(info.getHost(),
                info.getPort(), info.getExclusionListAsString());
        return proxyInfo;
    }

    private IConnectivityManager getConnectivityService() {
        synchronized (this) {
            if (mConnectivityManager != null) {
                return mConnectivityManager;
            }
            IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
            mConnectivityManager = IConnectivityManager.Stub.asInterface(b);
            return mConnectivityManager;
        }
    }
}
