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

/**
 * LiveDisplay is an advanced set of features for improving
 * display quality under various ambient conditions.
 *
 * The backend service is constructed with a set of LiveDisplayFeatures
 * which provide capabilities such as outdoor mode, night mode,
 * and calibration. It interacts with CMHardwareService to relay
 * changes down to the lower layers.
 *
 * Multiple adaptive modes are supported, and various hardware
 * features such as CABC, ACO and color enhancement are also
 * managed by LiveDisplay.
 */
public class LiveDisplayManager {

    /**
     * Enumeration of available adaptive modes.
     *
     * MODE_OFF: Disable all LiveDisplay adaptive features
     * MODE_NIGHT: Change color temperature to night mode
     * MODE_AUTO: Enable automatic detection of appropriate mode
     * MODE_OUTDOOR: Increase brightness/contrast/saturation for sunlight
     * MODE_DAY: Change color temperature to day mode, and allow
     *           detection of outdoor conditions
     */
    public static final int MODE_OFF = 0;
    public static final int MODE_NIGHT = 1;
    public static final int MODE_AUTO = 2;
    public static final int MODE_OUTDOOR = 3;
    public static final int MODE_DAY = 4;

    private static final int MODE_FIRST = MODE_OFF;
    private static final int MODE_LAST = MODE_DAY;

    /**
     * Additional hardware features managed by LiveDisplay
     *
     * CABC: Content adaptive backlight control, adjust images to
     *     increase brightness in order to reduce backlight level
     * ACO: Adjust images to increase contrast
     * CE: Adjust image to improve saturation and color
     * COLOR_ADJUSTMENT: Capable of adjusting RGB levels overall
     * MANAGED_OUTDOOR_MODE: System supports outdoor mode, but
     *     is provided externally by something like Qualcomm SVI.
     *     If this is the case, we don't need to measure ambient
     *     conditions ourselves and the toggle only enables or
     *     disables the backend.
     * DISPLAY_MODES: System supports multiple display calibrations
     *     for different viewing intents.
     */
    public static final int FEATURE_CABC = 10;
    public static final int FEATURE_AUTO_CONTRAST = 11;
    public static final int FEATURE_COLOR_ENHANCEMENT = 12;
    public static final int FEATURE_COLOR_ADJUSTMENT = 13;
    public static final int FEATURE_MANAGED_OUTDOOR_MODE = 14;
    public static final int FEATURE_DISPLAY_MODES = 15;

    private static final int FEATURE_FIRST = FEATURE_CABC;
    private static final int FEATURE_LAST = FEATURE_DISPLAY_MODES;

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

    /**
     * Checks if a particular feature or mode is supported by the system.
     *
     * @param feature
     * @return true if capable
     */
    public boolean hasFeature(int feature) {
        return ((feature >= MODE_FIRST && feature <= MODE_LAST) ||
                (feature >= FEATURE_FIRST && feature <= FEATURE_LAST)) &&
                mCapabilities.get(feature);
    }

    /**
     * Returns the current adaptive mode.
     *
     * @return id of the selected mode
     */
    public int getMode() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE, MODE_OFF);
    }

    /**
     * Selects a new adaptive mode.
     *
     * @param mode
     * @return true if the mode was selected
     */
    public boolean setMode(int mode) {
        if (!hasFeature(mode)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE, mode);
        return true;
    }

    /**
     * Checks if the auto contrast optimization feature is enabled.
     *
     * @return true if enabled
     */
    public boolean isAutoContrastEnabled() {
        return mCapabilities.get(FEATURE_AUTO_CONTRAST) &&
               getInt(CMSettings.System.DISPLAY_AUTO_CONTRAST, 0) == 1;
    }

    /**
     * Sets the state of auto contrast optimization
     *
     * @param enabled
     * @return true if state was changed
     */
    public boolean setAutoContrastEnabled(boolean enabled) {
        if (!mCapabilities.get(FEATURE_AUTO_CONTRAST)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_AUTO_CONTRAST, enabled ? 1 : 0);
        return true;
    }

    /**
     * Checks if the CABC feature is enabled
     *
     * @return true if enabled
     */
    public boolean isCABCEnabled() {
        return mCapabilities.get(FEATURE_CABC) &&
               getInt(CMSettings.System.DISPLAY_CABC, 1) == 1;
    }

    /**
     * Sets the state of CABC
     *
     * @param enabled
     * @return true if state was changed
     */
    public boolean setCABCEnabled(boolean enabled) {
        if (!mCapabilities.get(FEATURE_CABC)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_CABC, enabled ? 1 : 0);
        return true;
    }

    /**
     * Checks if the color enhancement feature is enabled
     *
     * @return true if enabled
     */
    public boolean isColorEnhancementEnabled() {
        return mCapabilities.get(FEATURE_COLOR_ENHANCEMENT) &&
               getInt(CMSettings.System.DISPLAY_COLOR_ENHANCE, 1) == 1;
    }

    /**
     * Sets the state of color enhancement
     *
     * @param enabled
     * @return true if state was changed
     */
    public boolean setColorEnhancementEnabled(boolean enabled) {
        if (!mCapabilities.get(FEATURE_COLOR_ENHANCEMENT)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_COLOR_ENHANCE, enabled ? 1 : 0);
        return true;
    }

    /**
     * Gets the default color temperature to use in the daytime. This is typically
     * set to 6500K, however this may not be entirely accurate. Use this value for
     * resetting controls to the default.
     *
     * @return the default day temperature in K
     */
    public int getDefaultDayTemperature() {
        return mDefaultDayTemperature;
    }

    /**
     * Gets the user-specified color temperature to use in the daytime.
     *
     * @return the day color temperature
     */
    public int getDayTemperature() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY, mDefaultDayTemperature);
    }

    /**
     * Sets the color temperature to use in the daytime.
     *
     * @param temperature
     */
    public void setDayTemperature(int temperature) {
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY, temperature);
    }

    /**
     * Gets the default color temperature to use at night. This is typically set
     * to 4500K, but this may not be entirely accurate. Use this value for resetting
     * controls to defaults.
     *
     * @return the default night color temperature
     */
    public int getDefaultNightTemperature() {
        return mDefaultNightTemperature;
    }

    /**
     * Gets the user-specified color temperature to use at night.
     *
     * @return the night color temperature
     */
    public int getNightTemperature() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT, mDefaultNightTemperature);
    }

    /**
     * Sets the color temperature to use at night.
     *
     * @param temperature
     */
    public void setNightTemperature(int temperature) {
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT, temperature);
    }

    /**
     * Checks if outdoor mode should be enabled automatically when under extremely high
     * ambient light. This is typically around 12000 lux.
     *
     * @return if outdoor conditions should be detected
     */
    public boolean isAutomaticOutdoorModeEnabled() {
        return mCapabilities.get(MODE_OUTDOOR) &&
                (getInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, 1) == 1);
    }

    /**
     * Enables automatic detection of outdoor conditions. Outdoor mode is triggered
     * when high ambient light is detected and it's not night.
     *
     * @param enabled
     * @return true if state was changed
     */
    public boolean setAutomaticOutdoorModeEnabled(boolean enabled) {
        if (!mCapabilities.get(MODE_OUTDOOR)) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, enabled ? 1 : 0);
        return true;
    }

    /**
     * Gets the current RGB triplet which is applied as a color adjustment.
     * The values are floats between 0 and 1. A setting of { 1.0, 1.0, 1.0 }
     * means that no adjustment is made.
     *
     * @return array of { R, G, B } offsets
     */
    public float[] getColorAdjustment() {
        if (!mCapabilities.get(FEATURE_COLOR_ADJUSTMENT)) {
            return new float[] { 1.0f, 1.0f, 1.0f };
        }
        return parseColorAdjustment(CMSettings.System.getStringForUser(
                mContext.getContentResolver(),
                CMSettings.System.DISPLAY_COLOR_ADJUSTMENT,
                UserHandle.USER_CURRENT));
    }

    /**
     * Sets the color adjustment to use. This can be set by the user to calibrate
     * their screen. This should be sent in the format { R, G, B } as floats from
     * 0 to 1. A setting of { 1.0, 1.0, 1.0 } means that no adjustment is made.
     * The hardware implementation may refuse certain values which make the display
     * unreadable, such as { 0, 0, 0 }. This calibration will be combined with other
     * internal adjustments, such as night mode, if necessary.
     *
     * @param array of { R, G, B } offsets
     * @return true if state was changed
     */
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
     * Parse and sanity check an RGB triplet from a string. Used internally.
     *
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
