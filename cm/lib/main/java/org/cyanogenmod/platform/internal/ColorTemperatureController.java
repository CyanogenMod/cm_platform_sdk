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

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.MathUtils;
import android.util.Slog;

import com.android.server.twilight.TwilightState;

import java.io.PrintWriter;

import cyanogenmod.providers.CMSettings;
import cyanogenmod.util.ColorUtils;

public class ColorTemperatureController extends LiveDisplayFeature {
        
    private ValueAnimator mAnimator;
    private DisplayHardwareController mDisplayHardware;
        
    private int mDefaultDayTemperature;
    private int mDefaultNightTemperature;
    
    private int mDayTemperature;
    private int mNightTemperature;
    
    private TwilightState mTwilight;
    
    private static final long TWILIGHT_ADJUSTMENT_TIME = DateUtils.HOUR_IN_MILLIS * 1;

    private static final int OFF_TEMPERATURE = 6500;
    
    private int mColorTemperature = OFF_TEMPERATURE;
    
    private final Object mLock = new Object();
    
    public ColorTemperatureController(Context context, Handler handler,
            DisplayHardwareController displayHardware) {
        super(context, handler);
        mDisplayHardware = displayHardware;
    }
    
    @Override
    public boolean onStart() {
        synchronized (mLock) {
            mDefaultDayTemperature = mContext.getResources().getInteger(
                    org.cyanogenmod.platform.internal.R.integer.config_dayColorTemperature);
            mDefaultNightTemperature = mContext.getResources().getInteger(
                    org.cyanogenmod.platform.internal.R.integer.config_nightColorTemperature);

            registerSettings(
                    CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_DAY),
                    CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT));
        }
        return true;
    }
    
    @Override
    public void onModeChanged(int mode) {
        synchronized (mLock) {
            updateColorTemperatureLocked();
        }
    }
    
    @Override
    public void onSettingsChanged(Uri uri) {
        synchronized (mLock) {
            mDayTemperature = getInt(CMSettings.System.DISPLAY_TEMPERATURE_DAY,
                    mDefaultDayTemperature);
            mNightTemperature = getInt(CMSettings.System.DISPLAY_TEMPERATURE_NIGHT,
                    mDefaultNightTemperature);
            updateColorTemperatureLocked();
        }
    }
    
    @Override
    public void onTwilightUpdated(TwilightState twilight) {
        synchronized (mLock) {
            mTwilight = twilight;
            mHandler.post(mTransitionRunnable);
        }
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
        if (mTwilight != null) {
            pw.println("    mTwilight=" + mTwilight.toString());
        }
        pw.println("    transitioning=" + mHandler.hasCallbacks(mTransitionRunnable));
    }
    
    private final Runnable mTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                updateColorTemperatureLocked();

                boolean transition = getMode() == LiveDisplayService.MODE_AUTO &&
                        mColorTemperature != mDayTemperature &&
                        mColorTemperature != mNightTemperature;
                
                if (transition) {
                    // fire again in a minute
                    mHandler.postDelayed(mTransitionRunnable, DateUtils.MINUTE_IN_MILLIS);
                }
            }
        }
    };
    
    private void updateColorTemperatureLocked() {
        mHandler.removeCallbacks(mTransitionRunnable);
        
        int temperature = mDayTemperature;
        int mode = getMode();
        
        if (mode == LiveDisplayService.MODE_OFF || isLowPowerMode()) {
            temperature = OFF_TEMPERATURE;
        } else if (mode == LiveDisplayService.MODE_NIGHT) {
            temperature = mNightTemperature;
        } else if (mode == LiveDisplayService.MODE_AUTO) {
            temperature = getTwilightK(mTwilight);
        }
        
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = ValueAnimator.ofInt(mColorTemperature, temperature);
        mAnimator.setDuration(Math.abs(mColorTemperature - temperature) / 2);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mLock) {
                            setDisplayTemperatureLocked((Integer)animation.getAnimatedValue());
                        }
                    }
                });
            }
        });
        mAnimator.start();
    }
    

    private void setDisplayTemperatureLocked(int temperature) {
        mColorTemperature = temperature;

        final float[] rgb = ColorUtils.temperatureToRGB(temperature);
        mDisplayHardware.setAdditionalAdjustment(rgb);
        
        Slog.d(TAG, "Adjust display temperature to " + temperature + "K");
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
     * @param state
     * @return color temperature in Kelvin
     */
    private int getTwilightK(TwilightState state) {
        float adjustment = 1.0f;

        if (state != null) {
            final long now = System.currentTimeMillis();
            adjustment = adj(now, state.getYesterdaySunset(), state.getTodaySunrise()) *
                         adj(now, state.getTodaySunset(), state.getTomorrowSunrise());
        }

        return (int)MathUtils.lerp(mNightTemperature, mDayTemperature, adjustment);
    }
}
