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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.PrintWriter;

public class AmbientLuxObserver {

    private final Sensor mLightSensor;
    private final SensorManager mSensorManager;

    private final float mThresholdLux;
    private final int mThresholdDuration;

    private boolean mLightSensorEnabled = false;
    private int mLightSensorRate;

    private float mAmbientLux = 0.0f;

    private static final int LOW = 0;
    private static final int HIGH = 1;

    private int mState = LOW;
    private boolean mTransitioning = false;

    private static final float SMOOTHING_FACTOR = 0.2f;

    private final AmbientLuxHandler mLuxHandler;

    private TransitionListener mCallback;

    public interface TransitionListener {
        public void onTransition(int state, float ambientLux);
    }

    public AmbientLuxObserver(Context context, Looper looper,
            float thresholdLux, int thresholdDuration) {
        mLuxHandler = new AmbientLuxHandler(looper);
        mThresholdLux = thresholdLux;
        mThresholdDuration = thresholdDuration;

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mLightSensorRate = context.getResources().getInteger(
                com.android.internal.R.integer.config_autoBrightnessLightSensorRate);
    }

    private class AmbientLuxHandler extends Handler {

        private static final int MSG_UPDATE_LUX = 0;
        private static final int MSG_THRESHOLD = 1;

        AmbientLuxHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_LUX:
                    final float lux = (Float)msg.obj;
                    if (mAmbientLux == 0.0f) {
                        mAmbientLux = lux;
                    } else {
                        mAmbientLux = (lux * SMOOTHING_FACTOR) +
                                      (mAmbientLux * (1 - SMOOTHING_FACTOR));
                    }
                    if (mAmbientLux >= mThresholdLux) {
                        transition(LOW, HIGH);
                    } else {
                        transition(HIGH, LOW);
                    }
                    break;
                case MSG_THRESHOLD:
                    if (mTransitioning) {
                        mState = msg.arg1;
                        mTransitioning = false;
                        if (mCallback != null) {
                            mCallback.onTransition(mState, mAmbientLux);
                        }
                    }
                    break;
            }
        }

        private void transition(int from, int to) {
            if (mState == from && !mTransitioning) {
                // threshold crossed, queue up a transition
                mTransitioning = true;
                sendMessageDelayed(Message.obtain(mLuxHandler, MSG_THRESHOLD, to), mThresholdDuration);
            } else if (mState == to && mTransitioning) {
                // cancel in-progress transition
                mTransitioning = false;
                removeMessages(MSG_THRESHOLD);
            }
        }

        void clear() {
            removeMessages(MSG_UPDATE_LUX);
            removeMessages(MSG_THRESHOLD);
        }
    };

    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mLightSensorEnabled) {
                mLuxHandler.sendMessage(Message.obtain(
                        mLuxHandler, AmbientLuxHandler.MSG_UPDATE_LUX, event.values[0]));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used.
        }
    };

    public int getState() {
        return mState;
    }

    public void setTransitionListener(TransitionListener callback) {
        if (callback == null) {
            enableLightSensor(false);
            mCallback = null;
        } else {
            mCallback = callback;
            enableLightSensor(true);
        }
    }

    private void enableLightSensor(boolean enable) {
        if (enable && !mLightSensorEnabled) {
            mAmbientLux = 0.0f;
            mState = LOW;
            mTransitioning = false;
            mLightSensorEnabled = true;
            mSensorManager.registerListener(mListener, mLightSensor,
                    mLightSensorRate * 1000, mLuxHandler);
        } else if (!enable && mLightSensorEnabled) {
            mLightSensorEnabled = false;
            mSensorManager.unregisterListener(mListener);
            mLuxHandler.clear();
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("AmbientLuxObserver State:");
        pw.println("  mLightSensorEnabled=" + mLightSensorEnabled);
        pw.println("  mState=" + mState);
        pw.println("  mAmbientLux=" + mAmbientLux);
    }
}
