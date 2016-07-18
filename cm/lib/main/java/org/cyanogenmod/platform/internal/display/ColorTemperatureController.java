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
import static cyanogenmod.hardware.LiveDisplayManager.MODE_NIGHT;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_OFF;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.MathUtils;
import android.util.Range;
import android.util.Slog;
import android.view.animation.LinearInterpolator;

import com.android.server.twilight.TwilightState;

import java.io.PrintWriter;
import java.util.BitSet;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.LiveDisplayManager;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.util.ColorUtils;

public class ColorTemperatureController extends LiveDisplayFeature {

    private final DisplayHardwareController mDisplayHardware;

    private final boolean mUseTemperatureAdjustment;
    private final boolean mUseColorBalance;
    private final Range<Integer> mColorBalanceRange;
    private final Range<Integer> mColorTemperatureRange;
    private final double[] mColorBalanceCurve;

    private final int mDefaultDayTemperature;
    private final int mDefaultNightTemperature;

    private int mColorTemperature = -1;
    private int mDayTemperature;
    private int mNightTemperature;

    private ValueAnimator mAnimator;

    private final CMHardwareManager mHardware;

    private static final long TWILIGHT_ADJUSTMENT_TIME = DateUtils.HOUR_IN_MILLIS * 1;

    private static final Uri DISPLAY_TEMPERATURE_DAY =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_DAY);
    private static final Uri DISPLAY_TEMPERATURE_NIGHT =
            CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT);

    public ColorTemperatureController(Context context,
            Handler handler, DisplayHardwareController displayHardware) {
        super(context, handler);
        mDisplayHardware = displayHardware;
        mHardware = CMHardwareManager.getInstance(mContext);

        mUseColorBalance = mHardware
                .isSupported(CMHardwareManager.FEATURE_COLOR_BALANCE);
        mColorBalanceRange = mHardware.getColorBalanceRange();

        mUseTemperatureAdjustment = mUseColorBalance ||
                mDisplayHardware.hasColorAdjustment();

        mDefaultDayTemperature = mContext.getResources().getInteger(
                org.cyanogenmod.platform.internal.R.integer.config_dayColorTemperature);
        mDefaultNightTemperature = mContext.getResources().getInteger(
                org.cyanogenmod.platform.internal.R.integer.config_nightColorTemperature);

        mColorTemperatureRange = Range.create(
                mContext.getResources().getInteger(
                        org.cyanogenmod.platform.internal.R.integer.config_minColorTemperature),
                mContext.getResources().getInteger(
                        org.cyanogenmod.platform.internal.R.integer.config_maxColorTemperature));

        mColorBalanceCurve = org.cyanogenmod.internal.util.MathUtils.powerCurve(
                mColorTemperatureRange.getLower(),
                mDefaultDayTemperature,
                mColorTemperatureRange.getUpper());
    }

    @Override
    public void onStart() {
        if (!mUseTemperatureAdjustment) {
            return;
        }

        mDayTemperature = getDayColorTemperature();
        mNightTemperature = getNightColorTemperature();

        registerSettings(DISPLAY_TEMPERATURE_DAY, DISPLAY_TEMPERATURE_NIGHT);
    }

    @Override
    public boolean getCapabilities(final BitSet caps) {
        if (mUseTemperatureAdjustment) {
            caps.set(MODE_AUTO);
            caps.set(MODE_DAY);
            caps.set(MODE_NIGHT);
            if (mUseColorBalance) {
                caps.set(LiveDisplayManager.FEATURE_COLOR_BALANCE);
            }
        }
        return mUseTemperatureAdjustment;
    }

    @Override
    protected void onUpdate() {
        updateColorTemperature();
    }

    @Override
    protected void onScreenStateChanged() {
        if (mAnimator != null && mAnimator.isRunning() && !isScreenOn()) {
            mAnimator.cancel();
        } else {
            updateColorTemperature();
        }
    }

    @Override
    protected void onTwilightUpdated() {
        updateColorTemperature();
    }

    @Override
    protected synchronized void onSettingsChanged(Uri uri) {
        if (uri == null || uri.equals(DISPLAY_TEMPERATURE_DAY)) {
            mDayTemperature = getDayColorTemperature();
        }
        if (uri == null || uri.equals(DISPLAY_TEMPERATURE_NIGHT)) {
            mNightTemperature = getNightColorTemperature();
        }
        updateColorTemperature();
    }

    @Override
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("ColorTemperatureController Configuration:");
        pw.println("  mDayTemperature=" + mDayTemperature);
        pw.println("  mNightTemperature=" + mNightTemperature);
        pw.println();
        pw.println("  ColorTemperatureController State:");
        pw.println("    mColorTemperature=" + mColorTemperature);
        pw.println("    isTransitioning=" + isTransitioning());
    }

    private final Runnable mTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            updateColorTemperature();
        }
    };

    private boolean isTransitioning() {
        return getMode() == MODE_AUTO &&
                mColorTemperature != mDayTemperature &&
                mColorTemperature != mNightTemperature;
    }

    private synchronized void updateColorTemperature() {
        if (!mUseTemperatureAdjustment || !isScreenOn()) {
            return;
        }
        int temperature = mDayTemperature;
        int mode = getMode();

        if (mode == MODE_OFF || isLowPowerMode()) {
            temperature = mDefaultDayTemperature;
        } else if (mode == MODE_NIGHT) {
            temperature = mNightTemperature;
        } else if (mode == MODE_AUTO) {
            temperature = getTwilightK();
        }

        if (DEBUG) {
            Slog.d(TAG, "updateColorTemperature mode=" + mode +
                       " temperature=" + temperature + " mColorTemperature=" + mColorTemperature);
        }

        setDisplayTemperature(temperature);

        if (isTransitioning()) {
            // fire again in a minute
            mHandler.postDelayed(mTransitionRunnable, DateUtils.MINUTE_IN_MILLIS);
        }
    }

    /**
     * Smoothly animate the current display color balance
     */
    private synchronized void animateColorBalance(int balance) {

        // always start with the current values in the hardware
        int current = mHardware.getColorBalance();

        if (current == balance) {
            return;
        }

        long duration = (long)(5 * Math.abs(current - balance));


        if (DEBUG) {
            Slog.d(TAG, "animateDisplayColor current=" + current +
                    " target=" + balance + " duration=" + duration);
        }

        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator.removeAllUpdateListeners();
        }

        mAnimator = ValueAnimator.ofInt(current, balance);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                synchronized (ColorTemperatureController.this) {
                    if (isScreenOn()) {
                        int value = (int) animation.getAnimatedValue();
                        mHardware.setColorBalance(value);
                    }
                }
            }
        });
        mAnimator.start();
    }

    /*
     * Map the color temperature to a color balance value using a power curve. This assumes the
     * correct configuration at the device level!
     */
    private int mapColorTemperatureToBalance(int temperature) {
        double z = org.cyanogenmod.internal.util.MathUtils.powerCurveToLinear(mColorBalanceCurve, temperature);
        return Math.round(MathUtils.lerp((float)mColorBalanceRange.getLower(),
                (float)mColorBalanceRange.getUpper(), (float)z));
    }

    private synchronized void setDisplayTemperature(int temperature) {
        if (!mColorTemperatureRange.contains(temperature)) {
            Slog.e(TAG, "Color temperature out of range: " + temperature);
            return;
        }

        mColorTemperature = temperature;

        if (mUseColorBalance) {
            int balance = mapColorTemperatureToBalance(temperature);
            Slog.d(TAG, "Set color balance = " + balance + " (temperature=" + temperature + ")");
            animateColorBalance(balance);
            return;
        }

        final float[] rgb = ColorUtils.temperatureToRGB(temperature);
        if (mDisplayHardware.setAdditionalAdjustment(rgb)) {
            if (DEBUG) {
                Slog.d(TAG, "Adjust display temperature to " + temperature + "K");
            }
        }
    }

    /**
     * Where is the sun anyway? This calculation determines day or night, and scales
     * the value around sunset/sunrise for a smooth transition.
     *
     * @param now
     * @param sunset
     * @param sunrise
     * @return float between 0 and 1
     */
    private static float adj(long now, long sunset, long sunrise) {
        if (sunset < 0 || sunrise < 0
                || now < sunset || now > (sunrise + TWILIGHT_ADJUSTMENT_TIME)) {
            return 1.0f;
        }

        if (now <= (sunset + TWILIGHT_ADJUSTMENT_TIME)) {
            return MathUtils.lerp(1.0f, 0.0f,
                    (float)(now - sunset) / TWILIGHT_ADJUSTMENT_TIME);
        }

        if (now >= sunrise) {
            return MathUtils.lerp(1.0f, 0.0f,
                    (float)((sunrise + TWILIGHT_ADJUSTMENT_TIME) - now) / TWILIGHT_ADJUSTMENT_TIME);
        }

        return 0.0f;
    }

    /**
     * Determine the color temperature we should use for the display based on
     * the position of the sun.
     *
     * @return color temperature in Kelvin
     */
    private int getTwilightK() {
        float adjustment = 1.0f;
        final TwilightState twilight = getTwilight();

        if (twilight != null) {
            final long now = System.currentTimeMillis();
            adjustment = adj(now, twilight.getYesterdaySunset(), twilight.getTodaySunrise()) *
                         adj(now, twilight.getTodaySunset(), twilight.getTomorrowSunrise());
        }

        return (int)MathUtils.lerp(mNightTemperature, mDayTemperature, adjustment);
    }

    int getDefaultDayTemperature() {
        return mDefaultDayTemperature;
    }

    int getDefaultNightTemperature() {
        return mDefaultNightTemperature;
    }

    int getColorTemperature() {
        return mColorTemperature;
    }

    int getDayColorTemperature() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY,
                mDefaultDayTemperature);
    }

    void setDayColorTemperature(int temperature) {
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY, temperature);
    }

    int getNightColorTemperature() {
        return getInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT,
                mDefaultNightTemperature);
    }

    void setNightColorTemperature(int temperature) {
        putInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT, temperature);
    }

    Range<Integer> getColorTemperatureRange() {
        return mColorTemperatureRange;
    }

    Range<Integer> getColorBalanceRange() {
        return mColorBalanceRange;
    }
}
