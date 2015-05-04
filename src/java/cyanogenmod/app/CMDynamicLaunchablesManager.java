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

package cyanogenmod.app;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;

/**
 * The CMDynamicLaunchablesManager allows you to update the dynamic icons on the system
 * Catapult launcher.
 *
 * <p>
 * To get the instance of this class, utilize
 * CMDynamicLaunchablesManager#getInstance(Context context)
 */
public class CMDynamicLaunchablesManager {
    private static final String TAG = "CMDynamicLaunchablesManager";
    private static boolean localLOGV = false;

    private Context mContext;

    private static ICMDynamicLaunchablesManager sService;

    private static CMDynamicLaunchablesManager sCmDynamicLaunchablesManager;

    private CMDynamicLaunchablesManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link CMDynamicLaunchablesManager}
     * @param context
     * @return {@link CMDynamicLaunchablesManager}
     */
    public static CMDynamicLaunchablesManager getInstance(Context context) {
        if (sCmDynamicLaunchablesManager == null) {
            sCmDynamicLaunchablesManager = new CMDynamicLaunchablesManager(context);
        }
        return sCmDynamicLaunchablesManager;
    }

    /** @hide */
    public ICMDynamicLaunchablesManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_DYNAMIC_LAUNCHABLES_SERVICE);
        sService = ICMDynamicLaunchablesManager.Stub.asInterface(b);
        return sService;
    }

    // interface methods

    /**
     * Turn on widgetry. Doesn't do anything yet
     */
    public void enableDynamicWidgetry(boolean enable) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMDynamicLaunchablesService");
            return;
        }
        String pkg = mContext.getPackageName();
        try {
            sService.enableDynamicWidgetry(pkg, enable);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm dynamic launchables service");
        }
    }

    public void populateWidgetTemplate(Bundle values) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMDynamicLaunchablesService");
            return;
        }
        String pkg = mContext.getPackageName();
        try {
            sService.populateWidgetTemplate(pkg, values);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm dynamic launchables service");
        }
    }

    public void isEnabledDynamicWidgetry() {
        if (sService == null) {
            Log.w(TAG, "not connected to CMDynamicLaunchablesService");
            return;
        }
    }

    public void setWidgetTemplateLayout(int id) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMDynamicLaunchablesService");
            return;
        }
    }
}
