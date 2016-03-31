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
package cyanogenmod.hardware;

import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

import cyanogenmod.app.CMContextConstants;

public class LiveDisplayManager {

    private static final String TAG = "LiveDisplay";

    private final Context mContext;

    private static LiveDisplayManager sInstance;

    private static ILiveDisplayService sService;

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    private LiveDisplayManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.LIVEDISPLAY) && !checkService()) {
            throw new RuntimeException("Unable to get LiveDisplayService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.hardware.LiveDisplayManager}
     * @param context
     * @return {@link LiveDisplayManager}
     */
    public synchronized static LiveDisplayManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LiveDisplayManager(context);
        }
        return sInstance;
    }

    /** @hide */
    public static ILiveDisplayService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_LIVEDISPLAY_SERVICE);
        if (b != null) {
            sService = ILiveDisplayService.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    /**
     * @return true if service is valid
     */
    private boolean checkService() {
        if (sService == null) {
            Log.w(TAG, "not connected to CMHardwareManagerService");
            return false;
        }
        return true;
    }
}
