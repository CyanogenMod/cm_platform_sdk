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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import java.util.BitSet;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.providers.CMSettings;

public class LiveDisplayManager {

    /* All possible modes */
    public static final int MODE_OFF = 0;
    public static final int MODE_NIGHT = 1;
    public static final int MODE_AUTO = 2;
    public static final int MODE_OUTDOOR = 3;
    public static final int MODE_DAY = 4;

    private static final int MODE_FIRST = MODE_OFF;
    private static final int MODE_LAST = MODE_DAY;

    /* Advanced features */
    public static final int FEATURE_CABC = 10;
    public static final int FEATURE_AUTO_CONTRAST = 11;
    public static final int FEATURE_COLOR_ENHANCEMENT = 12;
    public static final int FEATURE_COLOR_ADJUSTMENT = 13;
    public static final int FEATURE_MANAGED_OUTDOOR_MODE = 14;

    private static final int FEATURE_FIRST = FEATURE_CABC;
    private static final int FEATURE_LAST = FEATURE_MANAGED_OUTDOOR_MODE;

    private static final String TAG = "LiveDisplay";

    private final Context mContext;
    private final BitSet mCapabilities;

    private static LiveDisplayManager sInstance;
    private static ILiveDisplayService sService;

    private int mDefaultDayTemperature;
    private int mDefaultNightTemperature;

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

        try {
            mCapabilities = BitSet.valueOf(new long[] { sService.getCapabilities() });
            mDefaultDayTemperature = sService.getDefaultDayTemperature();
            mDefaultNightTemperature = sService.getDefaultNightTemperature();
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to fetch LiveDisplay configuration!", e);
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

    public boolean hasFeature(int feature) {
        return ((feature >= MODE_FIRST && feature <= MODE_LAST) ||
                (feature >= FEATURE_FIRST && feature <= FEATURE_LAST)) &&
                mCapabilities.get(feature);
    }

    public int getMode() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE, MODE_OFF);
    }

    public boolean setMode(int mode) {
        if (mode < 0 || mode > MODE_LAST || !mCapabilities.get(mode)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE, mode);
        return true;
    }

    public boolean isAutoContrastEnabled() {
        return mCapabilities.get(FEATURE_AUTO_CONTRAST) &&
               getInt(CMSettings.System.DISPLAY_AUTO_CONTRAST, 0) == 1;
    }

    public boolean setAutoContrastEnabled(boolean enabled) {
        if (!mCapabilities.get(FEATURE_AUTO_CONTRAST)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_AUTO_CONTRAST, enabled ? 1 : 0);
        return true;
    }

    public boolean isCABCEnabled() {
        return mCapabilities.get(FEATURE_CABC) &&
               getInt(CMSettings.System.DISPLAY_LOW_POWER, 1) == 1;
    }

    public boolean setCABCEnabled(boolean enabled) {
        if (!mCapabilities.get(FEATURE_CABC)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_LOW_POWER, enabled ? 1 : 0);
        return true;
    }

    public boolean isColorEnhancementEnabled() {
        return mCapabilities.get(FEATURE_COLOR_ENHANCEMENT) &&
               getInt(CMSettings.System.DISPLAY_COLOR_ENHANCE, 1) == 1;
    }

    public boolean setColorEnhancementEnabled(boolean enabled) {
        if (!mCapabilities.get(FEATURE_COLOR_ENHANCEMENT)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_COLOR_ENHANCE, enabled ? 1 : 0);
        return true;
    }

    public int getDefaultDayTemperature() {
        return mDefaultDayTemperature;
    }

    public int getDayTemperature() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY, mDefaultDayTemperature);
    }

    public void setDayTemperature(int temperature) {
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY, temperature);
    }

    public int getDefaultNightTemperature() {
        return mDefaultNightTemperature;
    }

    public int getNightTemperature() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT, mDefaultNightTemperature);
    }

    public void setNightTemperature(int temperature) {
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT, temperature);
    }

    public boolean isAutomaticOutdoorModeEnabled() {
        return mCapabilities.get(MODE_OUTDOOR) &&
                (getInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, 1) == 1);
    }

    public boolean setAutomaticOutdoorModeEnabled(boolean enabled) {
        if (!mCapabilities.get(MODE_OUTDOOR)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, enabled ? 1 : 0);
        return true;
    }

    public float[] getColorAdjustment() {
        if (!mCapabilities.get(FEATURE_COLOR_ADJUSTMENT)) {
            return new float[] { 1.0f, 1.0f, 1.0f };
        }
        return parseColorAdjustment(CMSettings.System.getStringForUser(
                mContext.getContentResolver(),
                CMSettings.System.DISPLAY_COLOR_ADJUSTMENT,
                UserHandle.USER_CURRENT));
    }

    public boolean setColorAdjustment(float[] adj) {
        // sanity check
        if (!mCapabilities.get(FEATURE_COLOR_ADJUSTMENT) || adj.length != 3 ||
                adj[0] < 0 || adj[0] > 1.0f ||
                adj[1] < 0 || adj[1] > 1.0f ||
                adj[2] < 0 || adj[2] > 1.0f) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(adj[0]).append(" ").append(adj[1]).append(" ").append(adj[2]);

        CMSettings.System.putStringForUser(mContext.getContentResolver(),
                CMSettings.System.DISPLAY_COLOR_ADJUSTMENT,
                sb.toString(), UserHandle.USER_CURRENT);

        return true;
    }

    private int getInt(String setting, int defValue) {
        return CMSettings.System.getIntForUser(mContext.getContentResolver(),
                setting, defValue, UserHandle.USER_CURRENT);
    }

    private void putInt(String setting, int value) {
        CMSettings.System.putIntForUser(mContext.getContentResolver(),
                setting, value, UserHandle.USER_CURRENT);
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
