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
 * The PartnerInterface allows applications to interact with a subset of system settings.
 * </p>
 */
public class PartnerInterface {
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
    /**
     * Sets the zen mode duration to 15 minutes
     * for #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_15_MINUTES_MS = 15 * 60000;
    /**
     * Sets the zen mode duration to 30 minutes
     * for #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_30_MINUTES_MS = 30 * 60000;
    /**
     * Sets the zen mode duration to 45 minutes
     * for #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_45_MINUTES_MS = 45 * 60000;
    /**
     * Sets the zen mode duration to 1 Hour
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_1_HOUR_MS = 60 * 60000;
    /**
     * Sets the zen mode duration to 2 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_2_HOURS_MS = 2 * ZEN_MODE_DURATION_1_HOUR_MS;
    /**
     * Sets the zen mode duration to 3 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_3_HOURS_MS = 3 * ZEN_MODE_DURATION_1_HOUR_MS;
    /**
     * Sets the zen mode duration to 4 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_4_HOURS_MS = 4 * ZEN_MODE_DURATION_1_HOUR_MS;
    /**
     * Sets the zen mode duration to 6 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_6_HOURS_MS = 6 * ZEN_MODE_DURATION_1_HOUR_MS;
    /**
     * Sets the zen mode duration to 8 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_8_HOURS_MS = 8 * ZEN_MODE_DURATION_1_HOUR_MS;
    /**
     * Sets the zen mode duration to 10 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_10_HOURS_MS = 10 * ZEN_MODE_DURATION_1_HOUR_MS;
    /**
     * Sets the zen mode duration to 12 Hours
     * Applies to #ZEN_MODE_NO_INTERRUPTIONS and #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * This is the duration in milliseconds that the mode will persist before being
     * set to #ZEN_MODE_OFF
     *
     * @see #setZenMode
     */
    public static final long ZEN_MODE_DURATION_12_HOURS_MS = 12 * ZEN_MODE_DURATION_1_HOUR_MS;

    private static IPartnerInterface sService;

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

    private static final String TAG = "PartnerInterface";

    private static PartnerInterface sPartnerInterfaceInstance;

    private PartnerInterface(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.PartnerInterface}
     * @param context
     * @return {@link PartnerInterface}
     */
    public static PartnerInterface getInstance(Context context) {
        if (sPartnerInterfaceInstance == null) {
            sPartnerInterfaceInstance = new PartnerInterface(context);
        }
        return sPartnerInterfaceInstance;
    }

    /** @hide */
    static public IPartnerInterface getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_PARTNER_INTERFACE);
        if (b != null) {
            sService = IPartnerInterface.Stub.asInterface(b);
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
            sService.setAirplaneModeEnabled(enabled);
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
            sService.setMobileDataEnabled(enabled);
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
     * @param durationMillis The duration in milliseconds
     */
    public boolean setZenMode(int mode) {
        if (sService == null) {
            return false;
        }
        try {
            return sService.setZenMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    /**
     * Set the zen mode for the device, allow a duration parameter
     *
     * You will need {@link #MODIFY_SOUND_SETTINGS_PERMISSION}
     * to utilize this functionality.
     *
     * @see #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * @see #ZEN_MODE_NO_INTERRUPTIONS
     * @param mode The zen mode to set the device to.
     *             One of {@link #ZEN_MODE_IMPORTANT_INTERRUPTIONS},
     *             {@link #ZEN_MODE_NO_INTERRUPTIONS}.
     *             If using {@link #ZEN_MODE_OFF}, the duration will be ignored
     * @param durationMillis The duration in milliseconds
     *  -1 for indefinitely
     */
    public boolean setZenMode(int mode, long durationMillis) {
        if (sService == null) {
            return false;
        }
        try {
            return sService.setZenModeWithDuration(mode, durationMillis);
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
            sService.shutdown();
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
            sService.reboot();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Retrieves the package name of the application that currently holds the
     * {@link cyanogenmod.media.MediaRecorder.AudioSource#HOTWORD} input.
     * @return The package name or null if no application currently holds the HOTWORD input.
     */
    public String getCurrentHotwordPackageName() {
        if (sService == null) {
            return null;
        }
        try {
            return sService.getCurrentHotwordPackageName();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }
}
