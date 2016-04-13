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
import android.os.UserHandle;
import android.text.format.DateUtils;
import android.util.MathUtils;
import android.util.Slog;

import com.android.server.twilight.TwilightState;

import java.io.PrintWriter;
import java.util.BitSet;

import cyanogenmod.providers.CMSettings;
import cyanogenmod.util.ColorUtils;

public class ColorTemperatureController extends LiveDisplayFeature {

    private ValueAnimator mAnimator;
    private DisplayHardwareController mDisplayHardware;

    private boolean mUseTemperatureAdjustment;

    private int mDefaultDayTemperature;
    private int mDefaultNightTemperature;

    private int mDayTemperature;
    private int mNightTemperature;

    private static final long TWILIGHT_ADJUSTMENT_TIME = DateUtils.HOUR_IN_MILLIS * 1;

    private static final int OFF_TEMPERATURE = 6500;

    private int mColorTemperature = OFF_TEMPERATURE;

    public ColorTemperatureController(Context context, Handler handler,
            DisplayHardwareController displayHardware) {
        super(context, handler);
        mDisplayHardware = displayHardware;
    }

    @Override
    public boolean onStart() {
        if (!mDisplayHardware.hasColorAdjustment()) {
            return false;
        }

        mUseTemperatureAdjustment = true;

        mDefaultDayTemperature = mContext.getResources().getInteger(
                org.cyanogenmod.platform.internal.R.integer.config_dayColorTemperature);
        mDefaultNightTemperature = mContext.getResources().getInteger(
                org.cyanogenmod.platform.internal.R.integer.config_nightColorTemperature);

        registerSettings(
                CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_DAY),
                CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT));
        return true;
    }

    void getCapabilities(final BitSet caps) {
        if (mUseTemperatureAdjustment) {
            caps.set(MODE_AUTO);
            caps.set(MODE_DAY);
            caps.set(MODE_NIGHT);
        }
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

    @Override
    public void onModeChanged(int mode) {
        super.onModeChanged(mode);
        updateColorTemperature();
    }

    @Override
    public synchronized void onSettingsChanged(Uri uri) {
        mDayTemperature = getDayColorTemperature();
        mNightTemperature = getNightColorTemperature();
        updateColorTemperature();
    }

    @Override
    public void onTwilightUpdated(TwilightState twilight) {
        super.onTwilightUpdated(twilight);
        mHandler.post(mTransitionRunnable);
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
        if (getTwilight() != null) {
            pw.println("    mTwilight=" + getTwilight().toString());
        }
        pw.println("    transitioning=" + mHandler.hasCallbacks(mTransitionRunnable));
    }

    private final Runnable mTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (ColorTemperatureController.this) {
                updateColorTemperature();

                boolean transition = getMode() == MODE_AUTO &&
                        mColorTemperature != mDayTemperature &&
                        mColorTemperature != mNightTemperature;

                if (transition) {
                    // fire again in a minute
                    mHandler.postDelayed(mTransitionRunnable, DateUtils.MINUTE_IN_MILLIS);
                }
            }
        }
    };

    private void updateColorTemperature() {
        mHandler.removeCallbacks(mTransitionRunnable);

        int temperature = mDayTemperature;
        int mode = getMode();

        if (mode == MODE_OFF || isLowPowerMode()) {
            temperature = OFF_TEMPERATURE;
        } else if (mode == MODE_NIGHT) {
            temperature = mNightTemperature;
        } else if (mode == MODE_AUTO) {
            temperature = getTwilightK();
        }

        if (DEBUG) {
            Slog.d(TAG, "updateColorTemperatureLocked mode=" + mode +
                       " temperature=" + temperature + " mColorTemperature=" + mColorTemperature);
        }

        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator.removeAllUpdateListeners();
        }
        mAnimator = ValueAnimator.ofInt(mColorTemperature, temperature);
        mAnimator.setDuration(Math.abs(mColorTemperature - temperature) / 2);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setDisplayTemperature((Integer)animation.getAnimatedValue());
                    }
                });
            }
        });
        mAnimator.start();
    }


    private synchronized void setDisplayTemperature(int temperature) {
        mColorTemperature = temperature;

        final float[] rgb = ColorUtils.temperatureToRGB(temperature);
        mDisplayHardware.setAdditionalAdjustment(rgb);

        if (DEBUG) {
            Slog.d(TAG, "Adjust display temperature to " + temperature + "K");
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
                || now < sunset || now > sunrise) {
            return 1.0f;
        }

        if (now < sunset + TWILIGHT_ADJUSTMENT_TIME) {
            return MathUtils.lerp(1.0f, 0.0f,
                    (float)(now - sunset) / TWILIGHT_ADJUSTMENT_TIME);
        }

        if (now > sunrise - TWILIGHT_ADJUSTMENT_TIME) {
            return MathUtils.lerp(1.0f, 0.0f,
                    (float)(sunrise - now) / TWILIGHT_ADJUSTMENT_TIME);
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
}
