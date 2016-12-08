/**
 * Copyright (c) 2016, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;

import cyanogenmod.platform.Manifest;
import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.List;

/** @hide */
public class MotionActionService extends SystemService {
    private static final String TAG = "MotionActionService";
    public static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private TelecomManager mTelecomManager;
    private PhoneStateListener mPhoneStateListener;
    private SensorManager mSensorManager;
    private Context mContext;
    private int mBehavior;
    private boolean mSensorDetectionStarted;

    private boolean mIsRinging;

    public MotionActionService(Context context) {
        super(context);
        mContext = context;
    }

    private class SensorHelper implements SensorEventListener {
        private static final int FACE_UP_LOWER_LIMIT = -45;
        private static final int FACE_UP_UPPER_LIMIT = 45;
        private static final int FACE_DOWN_UPPER_LIMIT = 135;
        private static final int FACE_DOWN_LOWER_LIMIT = -135;
        private static final int TILT_UPPER_LIMIT = 45;
        private static final int TILT_LOWER_LIMIT = -45;
        private static final int SENSOR_SAMPLES = 3;

        private boolean mWasFaceUp;
        private boolean[] mSamples = new boolean[SENSOR_SAMPLES];
        private int mSampleIndex;

        public void reset() {
            mWasFaceUp = false;
            mSampleIndex = 0;
            for (int i = 0; i < SENSOR_SAMPLES; i++) {
                mSamples[i] = false;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // Add a sample overwriting the oldest one. Several samples
            // are used
            // to avoid the erroneous values the sensor sometimes
            // returns.
            float y = event.values[1];
            float z = event.values[2];

            if (!mWasFaceUp) {
                // Check if its face up enough.
                mSamples[mSampleIndex] = y > FACE_UP_LOWER_LIMIT
                    && y < FACE_UP_UPPER_LIMIT
                    && z > TILT_LOWER_LIMIT && z < TILT_UPPER_LIMIT;

                // The device first needs to be face up.
                boolean faceUp = true;
                for (boolean sample : mSamples) {
                    faceUp = faceUp && sample;
                }
                if (faceUp) {
                    mWasFaceUp = true;
                    for (int i = 0; i < SENSOR_SAMPLES; i++) {
                        mSamples[i] = false;
                    }
                }
            } else {
                // Check if its face down enough. Note that wanted
                // values go from FACE_DOWN_UPPER_LIMIT to 180
                // and from -180 to FACE_DOWN_LOWER_LIMIT
                mSamples[mSampleIndex] = (y > FACE_DOWN_UPPER_LIMIT || y < FACE_DOWN_LOWER_LIMIT)
                        && z > TILT_LOWER_LIMIT
                        && z < TILT_UPPER_LIMIT;

                boolean faceDown = true;
                for (boolean sample : mSamples) {
                    faceDown = faceDown && sample;
                }
                if (faceDown) {
                    handleFlipAction();
                }
            }

            mSampleIndex = ((mSampleIndex + 1) % SENSOR_SAMPLES);
        }
    }

    private final SensorHelper mFlipListener = new SensorHelper();

    public void handleFlipAction() {
        mTelecomManager.silenceRinger();
        stopMotionListener();
    }

    @Override
    public void onStart() {
        mBehavior = CMSettings.Secure.getInt(mContext.getContentResolver(),
                CMSettings.Secure.MOTION_BEHAVIOR, CMSettings.Secure.MOTION_BEHAVIOR_DEFAULT);
        registerSettingsObserver();

        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public synchronized void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING && ! mIsRinging) {
                    Log.d(TAG, "Ringing started");
                    mIsRinging = true;
                    updateMotionListener();
                } else if (state != TelephonyManager.CALL_STATE_RINGING && mIsRinging) {
                    Log.d(TAG, "Ringing stopped");
                    mIsRinging = false;
                    updateMotionListener();
                }
            }
        };

        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private synchronized void stopMotionListener() {
        if (mSensorDetectionStarted) {
            mSensorDetectionStarted = false;
            mSensorManager.unregisterListener(mFlipListener);
        }
    }

    private synchronized void startMotionListener() {
        if (!mSensorDetectionStarted) {
            mFlipListener.reset();
            mSensorManager.registerListener(mFlipListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                    SensorManager.SENSOR_DELAY_NORMAL,
                    300 * 1000); // batch every 300 milliseconds
            mSensorDetectionStarted = true;
        }
    }

    private synchronized void updateMotionListener() {
        if (!mIsRinging) {
            stopMotionListener();
            return;
        }
        switch (mBehavior) {
            default:
            case CMSettings.Secure.MOTION_BEHAVIOR_NOTHING:
                stopMotionListener();
                return;
            case CMSettings.Secure.MOTION_BEHAVIOR_FLIP_TO_MUTE_INCOMING_CALL:
                startMotionListener();
                break;
        }
    }

    private void registerSettingsObserver() {
        final Uri motionBehaviorUri = CMSettings.Secure.getUriFor(
                CMSettings.Secure.MOTION_BEHAVIOR);
        ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) {
            @Override
            public synchronized void onChange(boolean selfChange, Uri uri, int userId) {
                if (motionBehaviorUri.equals(uri)) {
                    Log.d(TAG, "motion behavior changed");
                    mBehavior = CMSettings.Secure.getInt(mContext.getContentResolver(),
                            CMSettings.Secure.MOTION_BEHAVIOR,
                            CMSettings.Secure.MOTION_BEHAVIOR_DEFAULT);
                    updateMotionListener();
                }
            }
        };
        mContext.getContentResolver().registerContentObserver(motionBehaviorUri,
                false, observer, UserHandle.USER_ALL);
    }
}
