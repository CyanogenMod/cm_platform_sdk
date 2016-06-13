/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
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
import android.util.Log;

import cyanogenmod.app.CMContextConstants;

import java.io.UnsupportedEncodingException;
import java.lang.IllegalArgumentException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Manages access to CyanogenMod hardware extensions
 *
 *  <p>
 *  This manager requires the HARDWARE_ABSTRACTION_ACCESS permission.
 *  <p>
 *  To get the instance of this class, utilize CMHardwareManager#getInstance(Context context)
 */
public final class CMHardwareManager {
    private static final String TAG = "CMHardwareManager";

    private static ICMHardwareService sService;

    private Context mContext;
    /**
     * Adaptive backlight support (this refers to technologies like NVIDIA SmartDimmer,
     * QCOM CABL or Samsung CABC)
     */
    public static final int FEATURE_ADAPTIVE_BACKLIGHT = 0x1;

    /**
     * Color enhancement support
     */
    public static final int FEATURE_COLOR_ENHANCEMENT = 0x2;

    /**
     * Display RGB color calibration
     */
    public static final int FEATURE_DISPLAY_COLOR_CALIBRATION = 0x4;

    /**
     * Display gamma calibration
     */
    public static final int FEATURE_DISPLAY_GAMMA_CALIBRATION = 0x8;

    /**
     * High touch sensitivity for touch panels
     */
    public static final int FEATURE_HIGH_TOUCH_SENSITIVITY = 0x10;

    /**
     * Hardware navigation key disablement
     */
    public static final int FEATURE_KEY_DISABLE = 0x20;

    /**
     * Long term orbits (LTO)
     */
    public static final int FEATURE_LONG_TERM_ORBITS = 0x40;

    /**
     * Serial number other than ro.serialno
     */
    public static final int FEATURE_SERIAL_NUMBER = 0x80;

    /**
     * Increased display readability in bright light
     */
    public static final int FEATURE_SUNLIGHT_ENHANCEMENT = 0x100;

    /**
     * Double-tap the touch panel to wake up the device
     */
    public static final int FEATURE_TAP_TO_WAKE = 0x200;

    /**
     * Variable vibrator intensity
     */
    public static final int FEATURE_VIBRATOR = 0x400;

    /**
     * Touchscreen hovering
     */
    public static final int FEATURE_TOUCH_HOVERING = 0x800;

    /**
     * Auto contrast
     */
    public static final int FEATURE_AUTO_CONTRAST = 0x1000;

    /**
     * Display modes
     */
    public static final int FEATURE_DISPLAY_MODES = 0x2000;

    /**
     * Persistent storage
     */
    public static final int FEATURE_PERSISTENT_STORAGE = 0x4000;

    /**
     * Thermal change monitor
     */
    public static final int FEATURE_THERMAL_MONITOR = 0x8000;

    /**
     * Unique device ID
     */
    public static final int FEATURE_UNIQUE_DEVICE_ID = 0x10000;

    private static final List<Integer> BOOLEAN_FEATURES = Arrays.asList(
        FEATURE_ADAPTIVE_BACKLIGHT,
        FEATURE_COLOR_ENHANCEMENT,
        FEATURE_HIGH_TOUCH_SENSITIVITY,
        FEATURE_KEY_DISABLE,
        FEATURE_SUNLIGHT_ENHANCEMENT,
        FEATURE_TAP_TO_WAKE,
        FEATURE_TOUCH_HOVERING,
        FEATURE_AUTO_CONTRAST,
        FEATURE_THERMAL_MONITOR
    );

    private static CMHardwareManager sCMHardwareManagerInstance;

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    private CMHardwareManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.HARDWARE_ABSTRACTION) && !checkService()) {
            Log.wtf(TAG, "Unable to get CMHardwareService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.hardware.CMHardwareManager}
     * @param context
     * @return {@link CMHardwareManager}
     */
    public static CMHardwareManager getInstance(Context context) {
        if (sCMHardwareManagerInstance == null) {
            sCMHardwareManagerInstance = new CMHardwareManager(context);
        }
        return sCMHardwareManagerInstance;
    }

    /** @hide */
    public static ICMHardwareService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_HARDWARE_SERVICE);
        if (b != null) {
            sService = ICMHardwareService.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    /**
     * @return the supported features bitmask
     */
    public int getSupportedFeatures() {
        try {
            if (checkService()) {
                return sService.getSupportedFeatures();
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    /**
     * Determine if a CM Hardware feature is supported on this device
     *
     * @param feature The CM Hardware feature to query
     *
     * @return true if the feature is supported, false otherwise.
     */
    public boolean isSupported(int feature) {
        return feature == (getSupportedFeatures() & feature);
    }

    /**
     * Determine if the given feature is enabled or disabled.
     *
     * Only used for features which have simple enable/disable controls.
     *
     * @param feature the CM Hardware feature to query
     *
     * @return true if the feature is enabled, false otherwise.
     */
    public boolean get(int feature) {
        if (!BOOLEAN_FEATURES.contains(feature)) {
            throw new IllegalArgumentException(feature + " is not a boolean");
        }

        try {
            if (checkService()) {
                return sService.get(feature);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Enable or disable the given feature
     *
     * Only used for features which have simple enable/disable controls.
     *
     * @param feature the CM Hardware feature to set
     * @param enable true to enable, false to disale
     *
     * @return true if the feature is enabled, false otherwise.
     */
    public boolean set(int feature, boolean enable) {
        if (!BOOLEAN_FEATURES.contains(feature)) {
            throw new IllegalArgumentException(feature + " is not a boolean");
        }

        try {
            if (checkService()) {
                return sService.set(feature, enable);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    private int getArrayValue(int[] arr, int idx, int defaultValue) {
        if (arr == null || arr.length <= idx) {
            return defaultValue;
        }

        return arr[idx];
    }

    /**
     * {@hide}
     */
    public static final int VIBRATOR_INTENSITY_INDEX = 0;
    /**
     * {@hide}
     */
    public static final int VIBRATOR_DEFAULT_INDEX = 1;
    /**
     * {@hide}
     */
    public static final int VIBRATOR_MIN_INDEX = 2;
    /**
     * {@hide}
     */
    public static final int VIBRATOR_MAX_INDEX = 3;
    /**
     * {@hide}
     */
    public static final int VIBRATOR_WARNING_INDEX = 4;

    private int[] getVibratorIntensityArray() {
        try {
            if (checkService()) {
                return sService.getVibratorIntensity();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return The current vibrator intensity.
     */
    public int getVibratorIntensity() {
        return getArrayValue(getVibratorIntensityArray(), VIBRATOR_INTENSITY_INDEX, 0);
    }

    /**
     * @return The default vibrator intensity.
     */
    public int getVibratorDefaultIntensity() {
        return getArrayValue(getVibratorIntensityArray(), VIBRATOR_DEFAULT_INDEX, 0);
    }

    /**
     * @return The minimum vibrator intensity.
     */
    public int getVibratorMinIntensity() {
        return getArrayValue(getVibratorIntensityArray(), VIBRATOR_MIN_INDEX, 0);
    }

    /**
     * @return The maximum vibrator intensity.
     */
    public int getVibratorMaxIntensity() {
        return getArrayValue(getVibratorIntensityArray(), VIBRATOR_MAX_INDEX, 0);
    }

    /**
     * @return The warning threshold vibrator intensity.
     */
    public int getVibratorWarningIntensity() {
        return getArrayValue(getVibratorIntensityArray(), VIBRATOR_WARNING_INDEX, 0);
    }

    /**
     * Set the current vibrator intensity
     *
     * @param intensity the intensity to set, between {@link #getVibratorMinIntensity()} and
     * {@link #getVibratorMaxIntensity()} inclusive.
     *
     * @return true on success, false otherwise.
     */
    public boolean setVibratorIntensity(int intensity) {
        try {
            if (checkService()) {
                return sService.setVibratorIntensity(intensity);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_RED_INDEX = 0;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_GREEN_INDEX = 1;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_BLUE_INDEX = 2;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_DEFAULT_INDEX = 3;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_MIN_INDEX = 4;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_MAX_INDEX = 5;

    private int[] getDisplayColorCalibrationArray() {
        try {
            if (checkService()) {
                return sService.getDisplayColorCalibration();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the current RGB calibration, where int[0] = R, int[1] = G, int[2] = B.
     */
    public int[] getDisplayColorCalibration() {
        int[] arr = getDisplayColorCalibrationArray();
        if (arr == null || arr.length < 3) {
            return null;
        }
        return Arrays.copyOf(arr, 3);
    }

    /**
     * @return the default value for all colors
     */
    public int getDisplayColorCalibrationDefault() {
        return getArrayValue(getDisplayColorCalibrationArray(), COLOR_CALIBRATION_DEFAULT_INDEX, 0);
    }

    /**
     * @return The minimum value for all colors
     */
    public int getDisplayColorCalibrationMin() {
        return getArrayValue(getDisplayColorCalibrationArray(), COLOR_CALIBRATION_MIN_INDEX, 0);
    }

    /**
     * @return The minimum value for all colors
     */
    public int getDisplayColorCalibrationMax() {
        return getArrayValue(getDisplayColorCalibrationArray(), COLOR_CALIBRATION_MAX_INDEX, 0);
    }

    /**
     * Set the display color calibration to the given rgb triplet
     *
     * @param rgb RGB color calibration.  Each value must be between
     * {@link #getDisplayColorCalibrationMin()} and {@link #getDisplayColorCalibrationMax()},
     * inclusive.
     *
     * @return true on success, false otherwise.
     */
    public boolean setDisplayColorCalibration(int[] rgb) {
        try {
            if (checkService()) {
                return sService.setDisplayColorCalibration(rgb);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Write a string to persistent storage, which persists thru factory reset
     *
     * @param key String identifier for this item. Must not exceed 64 characters.
     * @param value The UTF-8 encoded string to store of at least 1 character. null deletes the key/value pair.
     * @return true on success
     */
    public boolean writePersistentString(String key, String value) {
        try {
            if (checkService()) {
                return sService.writePersistentBytes(key,
                        value == null ? null : value.getBytes("UTF-8"));
            }
        } catch (RemoteException e) {
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Write an integer to persistent storage, which persists thru factory reset
     *
     * @param key String identifier for this item. Must not exceed 64 characters.
     * @param value The integer to store
     * @return true on success
     */
    public boolean writePersistentInt(String key, int value) {
        try {
            if (checkService()) {
                return sService.writePersistentBytes(key,
                        ByteBuffer.allocate(4).putInt(value).array());
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Write a byte array to persistent storage, which persists thru factory reset
     *
     * @param key String identifier for this item. Must not exceed 64 characters.
     * @param value The byte array to store, must be 1-4096 bytes. null deletes the key/value pair.
     * @return true on success
     */
    public boolean writePersistentBytes(String key, byte[] value) {
        try {
            if (checkService()) {
                return sService.writePersistentBytes(key, value);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Read a string from persistent storage
     *
     * @param key String identifier for this item. Must not exceed 64 characters.
     * @return the stored UTF-8 encoded string, null if not found
     */
    public String readPersistentString(String key) {
        try {
            if (checkService()) {
                byte[] bytes = sService.readPersistentBytes(key);
                if (bytes != null) {
                    return new String(bytes, "UTF-8");
                }
            }
        } catch (RemoteException e) {
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Read an integer from persistent storage
     *
     * @param key String identifier for this item. Must not exceed 64 characters.
     * @return the stored integer, zero if not found
     */
    public int readPersistentInt(String key) {
        try {
            if (checkService()) {
                byte[] bytes = sService.readPersistentBytes(key);
                if (bytes != null) {
                    return ByteBuffer.wrap(bytes).getInt();
                }
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    /**
     * Read a byte array from persistent storage
     *
     * @param key String identifier for this item. Must not exceed 64 characters.
     * @return the stored byte array, null if not found
     */
    public byte[] readPersistentBytes(String key) {
        try {
            if (checkService()) {
                return sService.readPersistentBytes(key);
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /** Delete an object from persistent storage
     *
     * @param key String identifier for this item
     * @return true if an item was deleted
     */
    public boolean deletePersistentObject(String key) {
        try {
            if (checkService()) {
                return sService.writePersistentBytes(key, null);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * {@hide}
     */
    public static final int GAMMA_CALIBRATION_RED_INDEX = 0;
    /**
     * {@hide}
     */
    public static final int GAMMA_CALIBRATION_GREEN_INDEX = 1;
    /**
     * {@hide}
     */
    public static final int GAMMA_CALIBRATION_BLUE_INDEX = 2;
    /**
     * {@hide}
     */
    public static final int GAMMA_CALIBRATION_MIN_INDEX = 3;
    /**
     * {@hide}
     */
    public static final int GAMMA_CALIBRATION_MAX_INDEX = 4;

    private int[] getDisplayGammaCalibrationArray(int idx) {
        try {
            if (checkService()) {
                return sService.getDisplayGammaCalibration(idx);
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the number of RGB controls the device supports
     */
    @Deprecated
    public int getNumGammaControls() {
        try {
            if (checkService()) {
                return sService.getNumGammaControls();
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    /**
     * @param idx the control to query
     *
     * @return the current RGB gamma calibration for the given control
     */
    @Deprecated
    public int[] getDisplayGammaCalibration(int idx) {
        int[] arr = getDisplayGammaCalibrationArray(idx);
        if (arr == null || arr.length < 3) {
            return null;
        }
        return Arrays.copyOf(arr, 3);
    }

    /**
     * @return the minimum value for all colors
     */
    @Deprecated
    public int getDisplayGammaCalibrationMin() {
        return getArrayValue(getDisplayGammaCalibrationArray(0), GAMMA_CALIBRATION_MIN_INDEX, 0);
    }

    /**
     * @return the maximum value for all colors
     */
    @Deprecated
    public int getDisplayGammaCalibrationMax() {
        return getArrayValue(getDisplayGammaCalibrationArray(0), GAMMA_CALIBRATION_MAX_INDEX, 0);
    }

    /**
     * Set the display gamma calibration for a specific control
     *
     * @param idx the control to set
     * @param rgb RGB color calibration.  Each value must be between
     * {@link #getDisplayGammaCalibrationMin()} and {@link #getDisplayGammaCalibrationMax()},
     * inclusive.
     *
     * @return true on success, false otherwise.
     */
    @Deprecated
    public boolean setDisplayGammaCalibration(int idx, int[] rgb) {
        try {
            if (checkService()) {
                return sService.setDisplayGammaCalibration(idx, rgb);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * @return the source location of LTO data, or null on failure
     */
    public String getLtoSource() {
        try {
            if (checkService()) {
                return sService.getLtoSource();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the destination location of LTO data, or null on failure
     */
    public String getLtoDestination() {
        try {
            if (checkService()) {
                return sService.getLtoDestination();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the interval, in milliseconds, to trigger LTO data download
     */
    public long getLtoDownloadInterval() {
        try {
            if (checkService()) {
                return sService.getLtoDownloadInterval();
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    /**
     * @return the serial number to display instead of ro.serialno, or null on failure
     */
    public String getSerialNumber() {
        try {
            if (checkService()) {
                return sService.getSerialNumber();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return an id that's both unique and deterministic for the device
     */
    public String getUniqueDeviceId() {
        try {
            if (checkService()) {
                return sService.getUniqueDeviceId();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return true if adaptive backlight should be enabled when sunlight enhancement
     * is enabled.
     */
    public boolean requireAdaptiveBacklightForSunlightEnhancement() {
        try {
            if (checkService()) {
                return sService.requireAdaptiveBacklightForSunlightEnhancement();
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * @return true if this implementation does it's own lux metering
     */
    public boolean isSunlightEnhancementSelfManaged() {
        try {
            if (checkService()) {
                return sService.isSunlightEnhancementSelfManaged();
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * @return a list of available display modes on the devices
     */
    public DisplayMode[] getDisplayModes() {
        try {
            if (checkService()) {
                return sService.getDisplayModes();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the currently active display mode
     */
    public DisplayMode getCurrentDisplayMode() {
        try {
            if (checkService()) {
                return sService.getCurrentDisplayMode();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the default display mode to be set on boot
     */
    public DisplayMode getDefaultDisplayMode() {
        try {
            if (checkService()) {
                return sService.getDefaultDisplayMode();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return true if setting the mode was successful
     */
    public boolean setDisplayMode(DisplayMode mode, boolean makeDefault) {
        try {
            if (checkService()) {
                return sService.setDisplayMode(mode, makeDefault);
            }
        } catch (RemoteException e) {
        }
        return false;
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
     * @return current thermal {@link cyanogenmod.hardware.ThermalListenerCallback.State}
     */
    public int getThermalState() {
        try {
            if (checkService()) {
                return sService.getThermalState();
            }
        } catch (RemoteException e) {
        }
        return ThermalListenerCallback.State.STATE_UNKNOWN;
    }

   /**
    * Register a callback to be notified of thermal state changes
    * @return boolean indicating whether register succeeded or failed
    */
    public boolean registerThermalListener(ThermalListenerCallback thermalCallback) {
        try {
            if (checkService()) {
                return sService.registerThermalListener(thermalCallback);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

   /**
    * Unregister a callback previously registered to be notified of thermal state changes
    * @return boolean indicating whether un-registering succeeded or failed
    */
    public boolean unRegisterThermalListener(ThermalListenerCallback thermalCallback) {
        try {
            if (checkService()) {
                return sService.unRegisterThermalListener(thermalCallback);
            }
        } catch (RemoteException e) {
        }
        return false;
    }
}
