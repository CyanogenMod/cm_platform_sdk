/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package cyanogenmod.app;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/**
 * <p>
 * The SettingsManager allows applications to modify a subset of system settings.
 * </p>
 */
public class SettingsManager {
    /**
     * Turn off zen mode. This restores the original ring and interruption
     * settings that the user had set before zen mode was enabled.
     *
     * @see #setZenMode
     */
    public static final int ZEN_MODE_OFF = 0;
    /**
     * Sets zen mode to important interruptions mode, so that
     * only notifications that the user has chosen in Settings
     * to be of high importance will cause the user's device to notify them.
     *
     * This condition is held indefinitely until changed again.
     *
     * @see #setZenMode
     */
    public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;
    /**
     * Sets zen mode so that no interruptions will be allowed. The device is
     * effectively silenced indefinitely, until the mode is changed again.
     *
     * @see #setZenMode
     */
    public static final int ZEN_MODE_NO_INTERRUPTIONS = 2;

    private static ISettingsManager sService;

    private Context mContext;

    /**
     * Allows an application to change network settings,
     * such as enabling or disabling airplane mode.
     */
    public static final String MODIFY_NETWORK_SETTINGS_PERMISSION =
            "cyanogenmod.permission.MODIFY_NETWORK_SETTINGS";

    /**
     * Allows an application to change system sound settings, such as the zen mode.
     */
    public static final String MODIFY_SOUND_SETTINGS_PERMISSION =
            "cyanogenmod.permission.MODIFY_SOUND_SETTINGS";

    private static final String TAG = "SettingsManager";

    private static SettingsManager sSettingsManagerInstance;

    private SettingsManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.SettingsManager}
     * @param context
     * @return {@link SettingsManager}
     */
    public static SettingsManager getInstance(Context context) {
        if (sSettingsManagerInstance == null) {
            sSettingsManagerInstance = new SettingsManager(context);
        }
        return sSettingsManagerInstance;
    }

    /** @hide */
    static public ISettingsManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_SETTINGS_SERVICE);
        if (b != null) {
            sService = ISettingsManager.Stub.asInterface(b);
            return sService;
        } else {
            return null;
        }
    }

    /**
     * Turns on or off airplane mode.
     *
     * You will need {@link #MODIFY_NETWORK_SETTINGS_PERMISSION}
     * to utilize this functionality.
     * @param enabled if true, sets airplane mode to enabled, otherwise disables airplane mode.
     */
    public void setAirplaneModeEnabled(boolean enabled) {
        if (sService == null) {
            return;
        }
        try {
            getService().setAirplaneModeEnabled(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Turns on or off mobile network.
     *
     * You will need {@link #MODIFY_NETWORK_SETTINGS_PERMISSION}
     * to utilize this functionality.
     * @param enabled if true, sets mobile network to enabled, otherwise disables mobile network.
     */
    public void setMobileDataEnabled(boolean enabled) {
        if (sService == null) {
            return;
        }
        try {
            getService().setMobileDataEnabled(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Set the zen mode for the device.
     *
     * You will need {@link #MODIFY_SOUND_SETTINGS_PERMISSION}
     * to utilize this functionality.
     *
     * @see #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * @see #ZEN_MODE_NO_INTERRUPTIONS
     * @see #ZEN_MODE_OFF
     * @param mode The zen mode to set the device to.
     *             One of {@link #ZEN_MODE_IMPORTANT_INTERRUPTIONS},
     *             {@link #ZEN_MODE_NO_INTERRUPTIONS} or
     *             {@link #ZEN_MODE_OFF}.
     */
    public boolean setZenMode(int mode) {
        if (sService == null) {
            return false;
        }
        try {
            return getService().setZenMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    /**
     * Shuts down the device, immediately.
     *
     * You will need {@link android.Manifest.permission.REBOOT}
     * to utilize this functionality.
     */
    public void shutdownDevice() {
        if (sService == null) {
            return;
        }
        try {
            getService().shutdown();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Reboots the device, immediately.
     *
     * You will need {@link android.Manifest.permission.REBOOT}
     * to utilize this functionality.
     */
    public void rebootDevice() {
        if (sService == null) {
            return;
        }
        try {
            getService().reboot();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }
}
