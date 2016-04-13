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
package org.cyanogenmod.platform.internal.display;

import static cyanogenmod.hardware.LiveDisplayManager.MODE_AUTO;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_DAY;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_OUTDOOR;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.android.server.twilight.TwilightState;

import java.io.PrintWriter;
import java.util.BitSet;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.LiveDisplayManager;
import cyanogenmod.providers.CMSettings;

public class OutdoorModeController extends LiveDisplayFeature {

    private CMHardwareManager mHardware;
    private AmbientLuxObserver mLuxObserver;

    // hardware capabilities
    private boolean mUseOutdoorMode;
    private boolean mSelfManaged;

    // default values
    private int mDefaultOutdoorLux;
    private boolean mDefaultAutoOutdoorMode;

    // current values
    private boolean mAutoOutdoorMode;

    // internal state
    private boolean mIsOutdoor;
    private boolean mIsSensorEnabled;

    // sliding window for sensor event smoothing
    private static final int SENSOR_WINDOW_MS = 3000;

    public OutdoorModeController(Context context, Handler handler) {
        super(context, handler);
    }

    @Override
    public boolean onStart() {
        mHardware = CMHardwareManager.getInstance(mContext);
        if (!mHardware.isSupported(CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT)) {
            return false;
        }

        mUseOutdoorMode = true;

        mDefaultOutdoorLux = mContext.getResources().getInteger(
                org.cyanogenmod.platform.internal.R.integer.config_outdoorAmbientLux);
        mDefaultAutoOutdoorMode = mContext.getResources().getBoolean(
                org.cyanogenmod.platform.internal.R.bool.config_defaultAutoOutdoorMode);

        mSelfManaged = mHardware.isSunlightEnhancementSelfManaged();
        if (!mSelfManaged) {
            mLuxObserver = new AmbientLuxObserver(mContext, mHandler.getLooper(),
                    mDefaultOutdoorLux, SENSOR_WINDOW_MS);
        }

        registerSettings(
                CMSettings.System.getUriFor(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE));
        return true;
    }

    @Override
    void getCapabilities(final BitSet caps) {
        if (mUseOutdoorMode) {
            caps.set(LiveDisplayManager.MODE_OUTDOOR);
            if (mSelfManaged) {
                caps.set(LiveDisplayManager.FEATURE_MANAGED_OUTDOOR_MODE);
            }
        }
    }

    @Override
    public void onModeChanged(int mode) {
        super.onModeChanged(mode);
        updateOutdoorMode();
    }

    @Override
    public void onDisplayStateChanged(boolean screenOn) {
        super.onDisplayStateChanged(screenOn);
        if (mSelfManaged) {
            return;
        }
        updateOutdoorMode();
    }

    @Override
    public void onLowPowerModeChanged(boolean lowPowerMode) {
        super.onLowPowerModeChanged(lowPowerMode);
        updateOutdoorMode();
    }

    @Override
    public synchronized void onSettingsChanged(Uri uri) {
        mAutoOutdoorMode = getInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE,
                (mDefaultAutoOutdoorMode ? 1 : 0)) == 1;
        updateOutdoorMode();
    }

    @Override
    public void onTwilightUpdated(TwilightState twilight) {
        super.onTwilightUpdated(twilight);
        updateOutdoorMode();
    }

    @Override
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("OutdoorModeController Configuration:");
        pw.println("  mSelfManaged=" + mSelfManaged);
        if (!mSelfManaged) {
            pw.println("  mDefaultOutdoorLux=" + mDefaultOutdoorLux);
            pw.println();
            pw.println("  OutdoorModeController State:");
            pw.println("    mAutoOutdoorMode=" + mAutoOutdoorMode);
            pw.println("    mIsOutdoor=" + mIsOutdoor);
            pw.println("    mIsNight=" + isNight());
        }
        mLuxObserver.dump(pw);
    }

    boolean setAutomaticOutdoorModeEnabled(boolean enabled) {
        if (!mUseOutdoorMode) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, (enabled ? 1 : 0));
        return true;
    }

    boolean isAutomaticOutdoorModeEnabled() {
        return mUseOutdoorMode;
    }

    boolean getDefaultAutoOutdoorMode() {
        return mDefaultAutoOutdoorMode;
    }

    private void observeAmbientLuxLocked(boolean observe) {
        mLuxObserver.setTransitionListener(observe ? mListener : null);
    }

    /**
     * Outdoor mode is optionally enabled when ambient lux > 10000 and it's daytime
     * Melt faces!
     *
     * TODO: Use the camera or RGB sensor to determine if it's really sunlight
     */
    private synchronized void updateOutdoorMode() {
        /*
         * Hardware toggle:
         *   Enabled if outdoor mode explictly selected
         *   Enabled if outdoor lux exceeded and day mode or auto mode (if not night)
         */
        boolean enabled = !isLowPowerMode() &&
                 (getMode() == MODE_OUTDOOR ||
                 (mAutoOutdoorMode && (mSelfManaged || mIsOutdoor) &&
                 ((getMode() == MODE_AUTO && !isNight()) || getMode() == MODE_DAY)));
        mHardware.set(CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT, enabled);

        /* Sensor:
         *  Enabled in day mode
         *  Enabled in auto mode if it's not night
         *  Disabled if outdoor mode explicitly selected
         *  Disabled in low power mode
         *  Disabled if screen is off
         */
        boolean sensorEnabled = !isLowPowerMode() && isScreenOn() &&
                getMode() != MODE_OUTDOOR && mAutoOutdoorMode &&
                ((getMode() == MODE_AUTO && !isNight()) || getMode() == MODE_DAY);
        if (mIsSensorEnabled != sensorEnabled) {
            mIsSensorEnabled = sensorEnabled;
            observeAmbientLuxLocked(sensorEnabled);
        }
    }

    private final AmbientLuxObserver.TransitionListener mListener =
            new AmbientLuxObserver.TransitionListener() {
        @Override
        public void onTransition(final int state, float ambientLux) {
            final boolean outdoor = state == 1;
            synchronized (OutdoorModeController.this) {
                if (mIsOutdoor == outdoor) {
                    return;
                }

                mIsOutdoor = outdoor;
                updateOutdoorMode();
            }
        }
    };

}
