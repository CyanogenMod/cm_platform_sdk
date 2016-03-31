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
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Slog;

import java.io.PrintWriter;
import java.util.Arrays;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

public class DisplayHardwareController extends LiveDisplayFeature {
    
    private CMHardwareManager mHardware;

    private boolean mUseAutoContrast;
    private boolean mUseColorAdjustment;
    private boolean mUseColorEnhancement;
    private boolean mUseLowPower;
    
    private boolean mAutoContrast;
    private boolean mColorEnhancement;
    private boolean mLowPower;
    
    private final float[] mColorAdjustment = new float[] { 1.0f, 1.0f, 1.0f };
    private final float[] mAdditionalAdjustment = new float[] { 1.0f, 1.0f, 1.0f };
    
    private final float[] mRGB = new float[] { 1.0f, 1.0f, 1.0f };
    
    private final Object mLock = new Object();
    
    private static final Uri DISPLAY_AUTO_CONTRAST =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_AUTO_CONTRAST);
    private static final Uri DISPLAY_COLOR_ADJUSTMENT =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_COLOR_ADJUSTMENT);
    private static final Uri DISPLAY_COLOR_ENHANCE =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_COLOR_ENHANCE);
    private static final Uri DISPLAY_LOW_POWER =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_LOW_POWER);

    public DisplayHardwareController(Context context, Handler handler) {
        super(context, handler);
    }
    
    @Override
    public boolean onStart() {
        synchronized (mLock) {
            final ArraySet<Uri> settings = new ArraySet<Uri>();

            mUseLowPower = mHardware.isSupported(CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT);
            if (mUseLowPower) {
                mLowPower = mHardware.get(CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT);
                settings.add(DISPLAY_LOW_POWER);
            }

            mUseColorEnhancement = mHardware
                    .isSupported(CMHardwareManager.FEATURE_COLOR_ENHANCEMENT);
            if (mUseColorEnhancement) {
                mColorEnhancement = mHardware.get(CMHardwareManager.FEATURE_COLOR_ENHANCEMENT);
                settings.add(DISPLAY_COLOR_ENHANCE);
            }

            mUseAutoContrast = mHardware
                    .isSupported(CMHardwareManager.FEATURE_AUTO_CONTRAST);
            if (mUseAutoContrast) {
                mAutoContrast = mHardware.get(CMHardwareManager.FEATURE_AUTO_CONTRAST);
                settings.add(DISPLAY_COLOR_ENHANCE);
            }
            
            mUseColorAdjustment = mHardware
                    .isSupported(CMHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION);
            if (mUseColorAdjustment) {
                parseColorAdjustment(getString(CMSettings.System.DISPLAY_COLOR_ADJUSTMENT));
                settings.add(DISPLAY_COLOR_ADJUSTMENT);
            }

            if (settings.size() == 0) {
                return false;
            }

            registerSettings(settings.toArray(new Uri[settings.size()]));
            return true;
        }
    }

    @Override
    public void onSettingsChanged(Uri uri) {
        synchronized (mLock) {
            if (uri == null || uri.equals(DISPLAY_LOW_POWER)) {
                updateLowPowerMode();
            }
            if (uri == null || uri.equals(DISPLAY_AUTO_CONTRAST)) {
                updateAutoContrast();
            }
            if (uri == null || uri.equals(DISPLAY_COLOR_ENHANCE)) {
                updateColorEnhancement();
            }
            if (uri == null || uri.equals(DISPLAY_COLOR_ADJUSTMENT)) {
                updateColorAdjustment();
            }
        }
    }
    
    @Override
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("DisplayHardwareController Configuration:");
        pw.println("  mUseAutoContrast=" + mUseAutoContrast);
        pw.println("  mUseColorAdjustment=" + mUseColorAdjustment);
        pw.println("  mUseColorEnhancement="  + mUseColorEnhancement);
        pw.println("  mUseLowPower=" + mUseLowPower);
        pw.println();
        pw.println("  DisplayHardwareController State:");
        pw.println("    mAutoContrast=" + mAutoContrast);
        pw.println("    mColorEnhancement=" + mColorEnhancement);
        pw.println("    mLowPower=" + mLowPower);
        pw.println("    mColorAdjustment=" + Arrays.toString(mColorAdjustment));
        pw.println("    mAdditionalAdjustment=" + Arrays.toString(mAdditionalAdjustment));
        pw.println("    mRGB=" + Arrays.toString(mRGB));
    }
        
    /**
     * Additional adjustments provided by night mode
     * 
     * @param adj
     */
    void setAdditionalAdjustment(float[] adj) {
        synchronized (mLock) {
            if (adj != null && adj.length == 3) {
                System.arraycopy(adj, 0, mAdditionalAdjustment, 0, 3);
                updateColorAdjustment();
            }
        }
    }
    
    /**
     * Automatic contrast optimization
     */
    private void updateAutoContrast() {
        if (!mUseAutoContrast) {
            return;
        }

        boolean value = getInt(CMSettings.System.DISPLAY_AUTO_CONTRAST, 0) == 1;

        boolean enabled = !isLowPowerMode() && value;

        if (enabled == mAutoContrast) {
            return;
        }

        mHardware.set(CMHardwareManager.FEATURE_AUTO_CONTRAST, enabled);
        mAutoContrast = enabled;
    }
    
    /**
     * Color enhancement is optional
     */
    private void updateColorEnhancement() {
        if (!mUseColorEnhancement) {
            return;
        }

        boolean value = getInt(CMSettings.System.DISPLAY_COLOR_ENHANCE, 1) == 1;

        boolean enabled = !isLowPowerMode() && value;

        if (enabled == mColorEnhancement) {
            return;
        }

        mHardware.set(CMHardwareManager.FEATURE_COLOR_ENHANCEMENT, enabled);
        mColorEnhancement = enabled;
    }
    
    /**
     * Adaptive backlight / low power mode. Turn it off when under very bright light.
     */
    private void updateLowPowerMode() {
        if (!mUseLowPower) {
            return;
        }

        boolean value = getInt(CMSettings.System.DISPLAY_LOW_POWER, 1) == 1;

        boolean enabled = !isLowPowerMode() && value;
        
        if (enabled == mLowPower) {
            return;
        }

        mHardware.set(CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT, value);
        mLowPower = value;
    }
    
    private float[] parseColorAdjustment(String rgbString) {
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
        
        return parsed;
    }
    
    private void updateColorAdjustment() {
        if (!mUseColorAdjustment) {
            return;
        }
        
        final float[] rgb = new float[3];
        
        if (!isLowPowerMode()) {
            System.arraycopy(rgb, 0, mAdditionalAdjustment, 0, 3);
            rgb[0] *= mColorAdjustment[0];
            rgb[1] *= mColorAdjustment[1];
            rgb[2] *= mColorAdjustment[2];
        }

        if (rgb[0] == mRGB[0] && rgb[1] == mRGB[1] && rgb[2] == mRGB[2]) {
            // no changes
            return;
        }

        int max = mHardware.getDisplayColorCalibrationMax();
        mHardware.setDisplayColorCalibration(new int[] {
            (int) Math.ceil(rgb[0] * max),
            (int) Math.ceil(rgb[1] * max),
            (int) Math.ceil(rgb[2] * max)
        });
        System.arraycopy(rgb, 0, mRGB, 0, 3);
        
        screenRefresh();
        
    }

    /**
     * Tell SurfaceFlinger to repaint the screen. This is called after updating
     * hardware registers for display calibration to have an immediate effect.
     */
    private void screenRefresh() {
        try {
            final IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                final Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1004, data, null, 0);
                data.recycle();
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "Failed to refresh screen", ex);
        }
    }    
}
