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
import android.util.Range;
import android.util.Slog;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.HSIC;
import cyanogenmod.hardware.LiveDisplayManager;
import cyanogenmod.providers.CMSettings;

public class PictureAdjustmentController extends LiveDisplayFeature {

    private static final String TAG = "LiveDisplay-PAC";

    private final CMHardwareManager mHardware;
    private final boolean mUsePictureAdjustment;
    private HSIC mDefaultAdjustment;

    private List<Range<Float>> mRanges = new ArrayList<Range<Float>>();

    public PictureAdjustmentController(Context context, Handler handler) {
        super(context, handler);
        mHardware = CMHardwareManager.getInstance(context);

        boolean usePA = mHardware.isSupported(CMHardwareManager.FEATURE_PICTURE_ADJUSTMENT);
        HSIC defaultAdjustment = null;
        if (usePA) {
            mRanges.addAll(mHardware.getPictureAdjustmentRanges());
            if (mRanges.size() < 4) {
                usePA = false;
            } else {
                defaultAdjustment = getPictureAdjustment();
            }
        }
        if (!usePA) {
            mRanges.clear();
        }
        mDefaultAdjustment = defaultAdjustment;
        mUsePictureAdjustment = usePA;
    }

    @Override
    public void onStart() {
        if (!mUsePictureAdjustment) {
            return;
        }

        registerSettings(
                CMSettings.System.getUriFor(CMSettings.System.DISPLAY_PICTURE_ADJUSTMENT));
    }

    @Override
    protected void onSettingsChanged(Uri uri) {// nothing to do for mode switch
        updatePictureAdjustment();
    }

    @Override
    protected void onUpdate() {
        updatePictureAdjustment();
    }

    private void updatePictureAdjustment() {
        if (mUsePictureAdjustment && isScreenOn()) {
            final HSIC hsic = getPictureAdjustment();
            if (hsic != null) {
                if (!mHardware.setPictureAdjustment(hsic)) {
                    Slog.e(TAG, "Failed to set picture adjustment! " + hsic.toString());
                }
            }
        }
    }

    @Override
    public void dump(PrintWriter pw) {
        if (mUsePictureAdjustment) {
            pw.println();
            pw.println("PictureAdjustmentController Configuration:");
            pw.println("  adjustment=" + getPictureAdjustment());
            pw.println("  hueRange=" + getHueRange());
            pw.println("  saturationRange=" + getSaturationRange());
            pw.println("  intensityRange=" + getIntensityRange());
            pw.println("  contrastRange=" + getContrastRange());
            pw.println("  saturationThresholdRange=" + getSaturationThresholdRange());
            pw.println("  defaultAdjustment=" + getDefaultPictureAdjustment());
        }
    }

    @Override
    public boolean getCapabilities(BitSet caps) {
        if (mUsePictureAdjustment) {
            caps.set(LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT);
        }
        return mUsePictureAdjustment;
    }

    Range<Float> getHueRange() {
        return mUsePictureAdjustment && mRanges.size() > 0
                ? mRanges.get(0) : Range.create(0.0f, 0.0f);
    }

    Range<Float> getSaturationRange() {
        return mUsePictureAdjustment && mRanges.size() > 1
                ? mRanges.get(1) : Range.create(0.0f, 0.0f);
    }

    Range<Float> getIntensityRange() {
        return mUsePictureAdjustment && mRanges.size() > 2
                ? mRanges.get(2) : Range.create(0.0f, 0.0f);
    }

    Range<Float> getContrastRange() {
        return mUsePictureAdjustment && mRanges.size() > 3 ?
                mRanges.get(3) : Range.create(0.0f, 0.0f);
    }

    Range<Float> getSaturationThresholdRange() {
        return mUsePictureAdjustment && mRanges.size() > 4 ?
                mRanges.get(4) : Range.create(0.0f, 0.0f);
    }

    HSIC getDefaultPictureAdjustment() {
        return mUsePictureAdjustment ? mDefaultAdjustment : null;
    }

    HSIC getPictureAdjustment() {
        if (mUsePictureAdjustment) {
            String pref = getString(CMSettings.System.DISPLAY_PICTURE_ADJUSTMENT);
            if (pref != null) {
                return HSIC.unflattenFrom(pref);
            }
        }
        return mDefaultAdjustment;
    }

    boolean setPictureAdjustment(HSIC hsic) {
        if (mUsePictureAdjustment && hsic != null) {
            putString(CMSettings.System.DISPLAY_PICTURE_ADJUSTMENT, hsic.flatten());
            return true;
        }
        return false;
    }
}
