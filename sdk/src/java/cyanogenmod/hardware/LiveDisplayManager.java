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
import android.util.Slog;

import cyanogenmod.app.CMContextConstants;

public class LiveDisplayManager {

    /* All possible modes */
    public static final int MODE_OFF = 0;
    public static final int MODE_NIGHT = 1;
    public static final int MODE_AUTO = 2;
    public static final int MODE_OUTDOOR = 3;
    public static final int MODE_DAY = 4;

    /* Advanced features */
    public static final int FEATURE_CABC = 10;
    public static final int FEATURE_AUTO_CONTRAST = 11;
    public static final int FEATURE_COLOR_ENHANCEMENT = 12;
    public static final int FEATURE_COLOR_ADJUSTMENT = 13;
    public static final int FEATURE_MANAGED_OUTDOOR_MODE = 14;

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

    /**
     * @hide
     */
    public static float[] parseColorAdjustment(String rgbString) {
        String[] adj = rgbString == null ? null : rgbString.split(" ");
        float[] parsed = new float[3];

        if (adj == null || adj.length != 3) {
            adj = new String[] { "1.0", "1.0", "1.0" };
        }

        try {
            parsed[0] = Float.parseFloat(adj[0]);
            parsed[1] = Float.parseFloat(adj[1]);
            parsed[2] = Float.parseFloat(adj[2]);
        } catch (NumberFormatException e) {
            Slog.e(TAG, e.getMessage(), e);
            parsed[0] = 1.0f;
            parsed[1] = 1.0f;
            parsed[2] = 1.0f;
        }

        // sanity check
        for (int i = 0; i < parsed.length; i++) {
            if (parsed[i] <= 0.0f || parsed[i] > 1.0f) {
                parsed[i] = 1.0f;
            }
        }
        return parsed;
    }
}
