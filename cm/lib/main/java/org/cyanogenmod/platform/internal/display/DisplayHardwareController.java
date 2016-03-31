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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.LiveDisplayManager;
import cyanogenmod.providers.CMSettings;

public class DisplayHardwareController extends LiveDisplayFeature {

    private CMHardwareManager mHardware;

    // hardware capabilities
    private boolean mUseAutoContrast;
    private boolean mUseColorAdjustment;
    private boolean mUseColorEnhancement;
    private boolean mUseCABC;

    // default values
    private boolean mDefaultAutoContrast;
    private boolean mDefaultColorEnhancement;
    private boolean mDefaultCABC;

    // current values
    private boolean mAutoContrast;
    private boolean mColorEnhancement;
    private boolean mCABC;

    // color adjustment holders
    private final float[] mColorAdjustment = new float[] { 1.0f, 1.0f, 1.0f };
    private final float[] mAdditionalAdjustment = new float[] { 1.0f, 1.0f, 1.0f };
    private final float[] mRGB = new float[] { 1.0f, 1.0f, 1.0f };

    // settings uris
    private static final Uri DISPLAY_AUTO_CONTRAST =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_AUTO_CONTRAST);
    private static final Uri DISPLAY_COLOR_ADJUSTMENT =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_COLOR_ADJUSTMENT);
    private static final Uri DISPLAY_COLOR_ENHANCE =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_COLOR_ENHANCE);
    private static final Uri DISPLAY_CABC =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_CABC);

    public DisplayHardwareController(Context context, Handler handler) {
        super(context, handler);
    }

    @Override
    public boolean onStart() {
        final ArrayList<Uri> settings = new ArrayList<Uri>();

        mHardware = CMHardwareManager.getInstance(mContext);
        mUseCABC = mHardware
                .isSupported(CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT);
        if (mUseCABC) {
            mDefaultCABC = mContext.getResources().getBoolean(
                    org.cyanogenmod.platform.internal.R.bool.config_defaultCABC);
            mCABC = mHardware.get(CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT);
            settings.add(DISPLAY_CABC);
        }

        mUseColorEnhancement = mHardware
                .isSupported(CMHardwareManager.FEATURE_COLOR_ENHANCEMENT);
        if (mUseColorEnhancement) {
            mDefaultColorEnhancement = mContext.getResources().getBoolean(
                    org.cyanogenmod.platform.internal.R.bool.config_defaultColorEnhancement);
            mColorEnhancement = mHardware.get(CMHardwareManager.FEATURE_COLOR_ENHANCEMENT);
            settings.add(DISPLAY_COLOR_ENHANCE);
        }

        mUseAutoContrast = mHardware
                .isSupported(CMHardwareManager.FEATURE_AUTO_CONTRAST);
        if (mUseAutoContrast) {
            mDefaultAutoContrast = mContext.getResources().getBoolean(
                    org.cyanogenmod.platform.internal.R.bool.config_defaultAutoContrast);
            mAutoContrast = mHardware.get(CMHardwareManager.FEATURE_AUTO_CONTRAST);
            settings.add(DISPLAY_AUTO_CONTRAST);
        }

        mUseColorAdjustment = mHardware
                .isSupported(CMHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION);
        if (mUseColorAdjustment) {
            settings.add(DISPLAY_COLOR_ADJUSTMENT);
        }

        if (settings.size() == 0) {
            return false;
        }

        registerSettings(settings.toArray(new Uri[settings.size()]));
        return true;
    }

    @Override
    void getCapabilities(final BitSet caps) {
        if (mUseAutoContrast) {
            caps.set(LiveDisplayManager.FEATURE_AUTO_CONTRAST);
        }
        if (mUseColorEnhancement) {
            caps.set(LiveDisplayManager.FEATURE_COLOR_ENHANCEMENT);
        }
        if (mUseCABC) {
            caps.set(LiveDisplayManager.FEATURE_CABC);
        }
        if (mUseColorAdjustment) {
            caps.set(LiveDisplayManager.FEATURE_COLOR_ADJUSTMENT);
        }
    }

    @Override
    public synchronized void onSettingsChanged(Uri uri) {
        if (uri == null || uri.equals(DISPLAY_CABC)) {
            updateCABCMode();
        }
        if (uri == null || uri.equals(DISPLAY_AUTO_CONTRAST)) {
            updateAutoContrast();
        }
        if (uri == null || uri.equals(DISPLAY_COLOR_ENHANCE)) {
            updateColorEnhancement();
        }
        if (uri == null || uri.equals(DISPLAY_COLOR_ADJUSTMENT)) {
            System.arraycopy(
                    parseColorAdjustment(getString(CMSettings.System.DISPLAY_COLOR_ADJUSTMENT)),
                    0, mColorAdjustment, 0, 3);
            updateColorAdjustment();
        }
    }

    @Override
    public synchronized void onLowPowerModeChanged(boolean lowPowerMode) {
        updateCABCMode();
        updateAutoContrast();
        updateColorEnhancement();
    }

    @Override
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("DisplayHardwareController Configuration:");
        pw.println("  mUseAutoContrast=" + mUseAutoContrast);
        pw.println("  mUseColorAdjustment=" + mUseColorAdjustment);
        pw.println("  mUseColorEnhancement="  + mUseColorEnhancement);
        pw.println("  mUseCABC=" + mUseCABC);
        pw.println();
        pw.println("  DisplayHardwareController State:");
        pw.println("    mAutoContrast=" + mAutoContrast);
        pw.println("    mColorEnhancement=" + mColorEnhancement);
        pw.println("    mCABC=" + mCABC);
        pw.println("    mColorAdjustment=" + Arrays.toString(mColorAdjustment));
        pw.println("    mAdditionalAdjustment=" + Arrays.toString(mAdditionalAdjustment));
        pw.println("    mRGB=" + Arrays.toString(mRGB));
    }

    boolean hasColorAdjustment() {
        return mUseColorAdjustment;
    }

    /**
     * Additional adjustments provided by night mode
     *
     * @param adj
     */
    synchronized void setAdditionalAdjustment(float[] adj) {
        if (DEBUG) {
            Slog.d(TAG, "setAdditionalAdjustment: " + Arrays.toString(adj));
        }
        // Sanity check this so we don't mangle the display
        if (adj != null && adj.length == 3 &&
                !(adj[0] <= 0.0f && adj[1] <= 0.0f && adj[2] <= 0.0f)) {
            for (int i = 0; i < 3; i++) {
                if (adj[i] > 1.0f) {
                    adj[i] = 1.0f;
                }
            }
            System.arraycopy(adj, 0, mAdditionalAdjustment, 0, 3);
            updateColorAdjustment();
        } else {
            mAdditionalAdjustment[0] = 1.0f;
            mAdditionalAdjustment[1] = 1.0f;
            mAdditionalAdjustment[2] = 1.0f;
        }

    }

    /**
     * Automatic contrast optimization
     */
    private synchronized void updateAutoContrast() {
        if (!mUseAutoContrast) {
            return;
        }

        boolean value = getInt(CMSettings.System.DISPLAY_AUTO_CONTRAST,
                (mDefaultAutoContrast ? 1 : 0)) == 1;

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
    private synchronized void updateColorEnhancement() {
        if (!mUseColorEnhancement) {
            return;
        }

        boolean value = getInt(CMSettings.System.DISPLAY_COLOR_ENHANCE,
                (mDefaultColorEnhancement ? 1 : 0)) == 1;

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
    private synchronized void updateCABCMode() {
        if (!mUseCABC) {
            return;
        }

        boolean value = getInt(CMSettings.System.DISPLAY_CABC,
                (mDefaultCABC ? 1 : 0)) == 1;

        boolean enabled = !isLowPowerMode() && value;

        if (enabled == mCABC) {
            return;
        }

        mHardware.set(CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT, value);
        mCABC = value;
    }

    private synchronized void updateColorAdjustment() {
        if (!mUseColorAdjustment) {
            return;
        }

        final float[] rgb = new float[] { 1.0f, 1.0f, 1.0f };

        if (!isLowPowerMode()) {

            System.arraycopy(mAdditionalAdjustment, 0, rgb, 0, 3);
            rgb[0] *= mColorAdjustment[0];
            rgb[1] *= mColorAdjustment[1];
            rgb[2] *= mColorAdjustment[2];
        }

        if (rgb[0] == mRGB[0] && rgb[1] == mRGB[1] && rgb[2] == mRGB[2]) {
            // no changes
            return;
        }

        if (DEBUG) {
            Slog.d(TAG, "updateColorAdjustment: " + Arrays.toString(rgb));
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

    boolean getDefaultCABC() {
        return mDefaultCABC;
    }

    boolean getDefaultAutoContrast() {
        return mDefaultAutoContrast;
    }

    boolean getDefaultColorEnhancement() {
        return mDefaultColorEnhancement;
    }

    boolean isAutoContrastEnabled() {
        return mUseAutoContrast &&
                getInt(CMSettings.System.DISPLAY_AUTO_CONTRAST,
                        (mDefaultAutoContrast ? 1 : 0)) == 1;
    }

    boolean setAutoContrastEnabled(boolean enabled) {
        if (!mUseAutoContrast) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_AUTO_CONTRAST, enabled ? 1 : 0);
        return true;
    }

    boolean isCABCEnabled() {
        return mUseCABC &&
                getInt(CMSettings.System.DISPLAY_CABC,
                        (mDefaultCABC ? 1 : 0)) == 1;
    }

    boolean setCABCEnabled(boolean enabled) {
        if (!mUseCABC) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_CABC, enabled ? 1 : 0);
        return true;
    }

    boolean isColorEnhancementEnabled() {
        return mUseColorEnhancement &&
                getInt(CMSettings.System.DISPLAY_COLOR_ENHANCE,
                        (mDefaultColorEnhancement ? 1 : 0)) == 1;
    }

    boolean setColorEnhancementEnabled(boolean enabled) {
        if (!mUseColorEnhancement) {
            return false;
        }
        putInt(CMSettings.System.DISPLAY_COLOR_ENHANCE, enabled ? 1 : 0);
        return true;
    }


    float[] getColorAdjustment() {
        if (!mUseColorAdjustment) {
            return new float[] { 1.0f, 1.0f, 1.0f };
        }
        return parseColorAdjustment(getString(CMSettings.System.DISPLAY_COLOR_ADJUSTMENT));
    }

    boolean setColorAdjustment(float[] adj) {
        // sanity check
        if (!mUseColorAdjustment || adj.length != 3 ||
                adj[0] < 0 || adj[0] > 1.0f ||
                adj[1] < 0 || adj[1] > 1.0f ||
                adj[2] < 0 || adj[2] > 1.0f) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(adj[0]).append(" ").append(adj[1]).append(" ").append(adj[2]);

        putString(CMSettings.System.DISPLAY_COLOR_ADJUSTMENT, sb.toString());
        return true;
    }

    /**
     * Parse and sanity check an RGB triplet from a string.
     */
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

        // sanity check
        for (int i = 0; i < parsed.length; i++) {
            if (parsed[i] <= 0.0f || parsed[i] > 1.0f) {
                parsed[i] = 1.0f;
            }
        }
        return parsed;
    }
}
