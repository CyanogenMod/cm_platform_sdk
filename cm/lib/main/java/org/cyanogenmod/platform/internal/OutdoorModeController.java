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
package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.android.server.twilight.TwilightState;

import java.io.PrintWriter;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

public class OutdoorModeController extends LiveDisplayFeature {

    private CMHardwareManager mHardware;
    private AmbientLuxObserver mLuxObserver;

    private final Object mLock = new Object();

    private boolean mOutdoorMode;
    private boolean mOutdoorModeIsSelfManaged;
    private boolean mOutdoorModeSetByUser;

    private boolean mIsNight;
    private boolean mIsOutdoor;

    private int mDefaultOutdoorLux;

    private static final int OUTDOOR_THRESHOLD_DURATION = 2000;

    public OutdoorModeController(Context context, Handler handler) {
        super(context, handler);
    }

    @Override
    public boolean onStart() {
        synchronized (mLock) {
            mHardware = CMHardwareManager.getInstance(mContext);
            if (!mHardware.isSupported(CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT)) {
                return false;
            }

            mDefaultOutdoorLux = mContext.getResources().getInteger(
                    org.cyanogenmod.platform.internal.R.integer.config_outdoorAmbientLux);

            mOutdoorModeIsSelfManaged = mHardware.isSunlightEnhancementSelfManaged();
            if (!mOutdoorModeIsSelfManaged) {
                mLuxObserver = new AmbientLuxObserver(mContext, mHandler.getLooper(),
                        mDefaultOutdoorLux, OUTDOOR_THRESHOLD_DURATION);
            }

            registerSettings(
                    CMSettings.System.getUriFor(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE));
        }
        return true;
    }

    @Override
    public void onModeChanged(int mode) {
        super.onModeChanged(mode);

        if (mOutdoorModeIsSelfManaged) {
            return;
        }

        synchronized (mLock) {
            if (mode == LiveDisplayService.MODE_OUTDOOR) {
                mOutdoorModeSetByUser = true;
                updateOutdoorModeLocked();
            } else if (mOutdoorModeSetByUser) {
                mOutdoorModeSetByUser = false;
                updateOutdoorModeLocked();
            }
        }
    }

    @Override
    public void onDisplayStateChanged(boolean screenOn) {
        if (mOutdoorModeIsSelfManaged) {
            return;
        }
        synchronized (mLock) {
            if (screenOn) {
                updateOutdoorModeLocked();
            } else {
                observeAmbientLuxLocked(false);
            }
        }
    }

    @Override
    public void onSettingsChanged(Uri uri) {
        synchronized (mLock) {
            mOutdoorMode = getInt(CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, 1) == 1;
            if (mOutdoorModeIsSelfManaged) {
                mHardware.set(CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT, mOutdoorMode);
            } else {
                updateOutdoorModeLocked();
            }
        }
    }

    @Override
    public void onTwilightUpdated(TwilightState twilight) {
        mIsNight = twilight == null ? false : twilight.isNight();
    }

    @Override
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("OutdoorModeController Configuration:");
        pw.println("  mOutdoorModeIsSelfManaged=" + mOutdoorModeIsSelfManaged);
        pw.println("  mDefaultOutdoorLux=" + mDefaultOutdoorLux);
        pw.println();
        pw.println("  OutdoorModeController State:");
        pw.println("    mOutdoorMode=" + mOutdoorMode);
        pw.println("    mOutdoorModeSetByUser=" + mOutdoorModeSetByUser);
        pw.println("    mIsOutdoor=" + mIsOutdoor);
        pw.println("    mIsNight=" + mIsNight);
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
    private void updateOutdoorModeLocked() {

        boolean enabled = !isLowPowerMode() && (mOutdoorModeSetByUser ||
                 (mOutdoorMode && getMode() == LiveDisplayService.MODE_AUTO && !mIsNight && mIsOutdoor));

        boolean sensorEnabled = !isLowPowerMode() && !mOutdoorModeSetByUser &&
                getMode() == LiveDisplayService.MODE_AUTO && mOutdoorMode;

        mHardware.set(CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT, enabled);
        observeAmbientLuxLocked(sensorEnabled);
    }

    private final AmbientLuxObserver.TransitionListener mListener =
            new AmbientLuxObserver.TransitionListener() {
        @Override
        public void onTransition(final int state, float ambientLux) {
            final boolean outdoor = state == 1;
            if (mIsOutdoor == outdoor) {
                return;
            }

            mIsOutdoor = outdoor;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLock) {
                        mIsOutdoor = outdoor;
                        updateOutdoorModeLocked();
                    }
                }
            });
        }
    };

}
