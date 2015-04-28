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

package cyanogenmod.net;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import android.util.Log;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.net.ICMProxyManager;

/**
 * The CMProxyManager allows you to set the global proxy information
 *
 * <p>
 * Each of the published public methods pass around a {@link cyanogenmod.net.GlobalProxyInfo}
 * object.
 * <p>
 * To get the instance of this class, utilize CMProxyManager#getInstance(Context context)
 *
 * @see cyanogenmod.net.GlobalProxyInfo
 */
public class CMProxyManager {
    private static final String TAG = "CMProxyManager";

    private static ICMProxyManager sService;
    private Context mContext;
    private static CMProxyManager sCMProxyManagerInstance;

    private CMProxyManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link CMProxyManager}
     * @param context
     * @return {@link CMProxyManager}
     */
    public static CMProxyManager getInstance(Context context) {
        if (sCMProxyManagerInstance == null) {
            sCMProxyManagerInstance = new CMProxyManager(context);
        }
        return sCMProxyManagerInstance;
    }

    /**
     * Set a network-independent global http proxy.  This is not normally what you want
     * for typical HTTP proxies - they are general network dependent.  However if you're
     * doing something unusual like general internal filtering this may be useful.  On
     * a private network where the proxy is not accessible, you may break HTTP using this.
     *
     * @param info The a {@link GlobalProxyInfo} object defining the new global
     *        HTTP proxy.  A {@code null} value will clear the global HTTP proxy.
     */
    public void setGlobalProxy(GlobalProxyInfo info) {
        try {
            sService.setGlobalProxy(info);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to connect to CMConnectivityService");
        }
    }

    /**
     * Retrieve any network-independent global HTTP proxy.
     *
     * @return {@link GlobalProxyInfo} for the current global HTTP proxy or {@code null}
     *        if no global HTTP proxy is set.
     */
    public GlobalProxyInfo getGlobalProxy() {
        GlobalProxyInfo info = null;
        try {
            info = sService.getGlobalProxy();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to connect to CMConnectivityService");
        }
        return info;
    }

    /** @hide */
    public ICMProxyManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_PROXY_SERVICE);
        sService = ICMProxyManager.Stub.asInterface(b);
        return sService;
    }
}
