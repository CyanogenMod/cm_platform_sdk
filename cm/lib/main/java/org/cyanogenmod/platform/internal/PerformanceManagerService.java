/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.Watchdog;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManager;
import cyanogenmod.power.PerformanceManagerInternal;
import cyanogenmod.providers.CMSettings;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

public class PerformanceManagerService extends SystemService {

    private static final String TAG = "PerformanceManager";

    private final Context mContext;

    private Pattern[] mPatterns = null;
    private int[] mProfiles = null;

    /** Active profile that based on low power mode, user and app rules */
    private int mCurrentProfile = -1;
    private int mNumProfiles = 0;

    private final ServiceThread mHandlerThread;
    private final PerformanceManagerHandler mHandler;

    // keep in sync with hardware/libhardware/include/hardware/power.h
    private final int POWER_HINT_CPU_BOOST    = 0x00000010;
    private final int POWER_HINT_LAUNCH_BOOST = 0x00000011;
    private final int POWER_HINT_SET_PROFILE  = 0x00000030;

    private final int POWER_FEATURE_SUPPORTED_PROFILES = 0x00001000;

    private PowerManagerInternal mPm;
    private boolean mLowPowerModeEnabled = false;
    private String mCurrentActivityName = null;

    // Max time (microseconds) to allow a CPU boost for
    private static final int MAX_CPU_BOOST_TIME = 5000000;
    private static final boolean DEBUG = false;
    
    public PerformanceManagerService(Context context) {
        super(context);
        
        mContext = context;

        String[] activities = context.getResources().getStringArray(
                R.array.config_auto_perf_activities);
        if (activities != null && activities.length > 0) {
            mPatterns = new Pattern[activities.length];
            mProfiles = new int[activities.length];
            for (int i = 0; i < activities.length; i++) {
                String[] info = activities[i].split(",");
                if (info.length == 2) {
                    mPatterns[i] = Pattern.compile(info[0]);
                    mProfiles[i] = Integer.valueOf(info[1]);
                    if (DEBUG) {
                        Slog.d(TAG, String.format("App profile #%d: %s => %s",
                            i, info[0], info[1]));
                    }
                }
            }
        }

        // We need a high priority thread to handle these requests in front of
        // everything else asynchronously
        mHandlerThread = new ServiceThread(TAG,
                Process.THREAD_PRIORITY_URGENT_DISPLAY + 1, false /*allowIo*/);
        mHandlerThread.start();

        mHandler = new PerformanceManagerHandler(mHandlerThread.getLooper());
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PERFORMANCE_SERVICE, mBinder);
        publishLocalService(PerformanceManagerInternal.class, new LocalService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            synchronized (this) {
                mPm = getLocalService(PowerManagerInternal.class);
                mNumProfiles = mPm.getFeature(POWER_FEATURE_SUPPORTED_PROFILES);
                if (mNumProfiles > 0) {
                    int profile = getUserProfile();
                    if (profile == PerformanceManager.PROFILE_HIGH_PERFORMANCE) {
                        Slog.i(TAG, String.format("Reverting profile %d to %d",
                            profile, PerformanceManager.PROFILE_BALANCED));
                        setPowerProfileInternal(
                            PerformanceManager.PROFILE_BALANCED, true);
                    } else {
                        setPowerProfileInternal(profile, false);
                    }

                    mPm.registerLowPowerModeObserver(mLowPowerModeListener);
                }
            }
        }
    }

    private boolean hasAppProfiles() {
        return mNumProfiles > 0 && mPatterns != null &&
               (CMSettings.Secure.getInt(mContext.getContentResolver(),
                       CMSettings.Secure.APP_PERFORMANCE_PROFILES_ENABLED, 1) == 1);
    }

    private boolean getProfileHasAppProfilesInternal(int profile) {
        if (profile < 0 || profile > mNumProfiles) {
            Slog.e(TAG, "Invalid profile: " + profile);
            return false;
        }

        if (profile == PerformanceManager.PROFILE_BALANCED) {
            return mPatterns != null;
        }

        return false;
    }

    /**
     * Get the profile saved by the user
     */
    private int getUserProfile() {
        return CMSettings.Secure.getInt(mContext.getContentResolver(),
                CMSettings.Secure.PERFORMANCE_PROFILE,
                PerformanceManager.PROFILE_BALANCED);
    }

    /**
     * Apply a power profile and persist if fromUser = true
     *
     * @param  profile  power profile
     * @param  fromUser true to persist the profile
     * @return          true if the active profile changed
     */
    private synchronized boolean setPowerProfileInternal(int profile, boolean fromUser) {
        if (DEBUG) {
            Slog.v(TAG, String.format(
                "setPowerProfileInternal(profile=%d, fromUser=%b)",
                profile, fromUser));
        }
        if (profile < 0 || profile > mNumProfiles) {
            Slog.e(TAG, "Invalid profile: " + profile);
            return false;
        }

        /**
         * It's possible that mCurrrentProfile != getUserProfile() because of a
         * per-app profile. Store the user's profile preference and then bail
         * early if there is no work to be done.
         */
        if (fromUser) {
            CMSettings.Secure.putInt(mContext.getContentResolver(),
                    CMSettings.Secure.PERFORMANCE_PROFILE, profile);
        }

        if (profile == mCurrentProfile) {
            return false;
        }

        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DEVICE_POWER, null);

        long token = Binder.clearCallingIdentity();

        mCurrentProfile = profile;
        
        mHandler.removeMessages(MSG_CPU_BOOST);
        mHandler.removeMessages(MSG_LAUNCH_BOOST);
        mHandler.sendMessage(
                Message.obtain(mHandler, MSG_SET_PROFILE, profile,
                               (fromUser ? 1 : 0)));

        Binder.restoreCallingIdentity(token);

        return true;
    }

    private int getProfileForActivity(String componentName) {
        if (componentName != null) {
            for (int i = 0; i < mPatterns.length; i++) {
                if (mPatterns[i].matcher(componentName).matches()) {
                    return mProfiles[i];
                }
            }
        }
        return PerformanceManager.PROFILE_BALANCED;
    }

    private void cpuBoostInternal(int duration) {
        if (duration > 0 && duration <= MAX_CPU_BOOST_TIME) {
            // Don't send boosts if we're in another power profile
            if (mCurrentProfile == PerformanceManager.PROFILE_POWER_SAVE ||
                    mCurrentProfile == PerformanceManager.PROFILE_HIGH_PERFORMANCE) {
                return;
            }
            mHandler.removeMessages(MSG_CPU_BOOST);
            mHandler.sendMessage(Message.obtain(mHandler, MSG_CPU_BOOST, duration, 0));
        } else {
            Slog.e(TAG, "Invalid boost duration: " + duration);
        }
    }

    private void applyProfile() {
        if (mNumProfiles < 1) {
            // don't have profiles, bail.
            return;
        }

        int profile;
        if (mLowPowerModeEnabled) {
            // LPM always wins
            profile = PerformanceManager.PROFILE_POWER_SAVE;
        } else {
            profile = getUserProfile();
            // use app specific rules if profile is balanced
            if (hasAppProfiles() && getProfileHasAppProfilesInternal(profile)) {
                profile = getProfileForActivity(mCurrentActivityName);
            }
        }

        setPowerProfileInternal(profile, false);
    }

    private final IBinder mBinder = new IPerformanceManager.Stub() {

        @Override
        public boolean setPowerProfile(int profile) {
            return setPowerProfileInternal(profile, true);
        }

        /**
         * Boost the CPU
         * 
         * @param duration Duration to boost the CPU for, in milliseconds.
         * @hide
         */
        @Override
        public void cpuBoost(int duration) {
            cpuBoostInternal(duration);
        }

        @Override
        public int getPowerProfile() {
            return getUserProfile();
        }

        @Override
        public int getNumberOfProfiles() {
            return mNumProfiles;
        }

        @Override
        public boolean getProfileHasAppProfiles(int profile) {
            return getProfileHasAppProfilesInternal(profile);
        }
    };

    private final class LocalService implements PerformanceManagerInternal {

        @Override
        public void cpuBoost(int duration) {
            cpuBoostInternal(duration);
        }
        
        @Override
        public void launchBoost() {
            // Don't send boosts if we're in another power profile
            if (mCurrentProfile == PerformanceManager.PROFILE_POWER_SAVE ||
                    mCurrentProfile == PerformanceManager.PROFILE_HIGH_PERFORMANCE) {
                return;
            }
            mHandler.removeMessages(MSG_CPU_BOOST);
            mHandler.removeMessages(MSG_LAUNCH_BOOST);
            mHandler.sendEmptyMessage(MSG_LAUNCH_BOOST);
        }

        @Override
        public void activityResumed(Intent intent) {
            String activityName = null;
            if (intent != null) {
                final ComponentName cn = intent.getComponent();
                if (cn != null) {
                    activityName = cn.flattenToString();
                }
            }

            mCurrentActivityName = activityName;
            applyProfile();
        }
    }

    private static final int MSG_CPU_BOOST = 1;
    private static final int MSG_LAUNCH_BOOST = 2;
    private static final int MSG_SET_PROFILE = 3;
    
    /**
     * Handler for asynchronous operations performed by the performance manager.
     */
    private final class PerformanceManagerHandler extends Handler {
        public PerformanceManagerHandler(Looper looper) {
            super(looper, null, true /*async*/);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CPU_BOOST:
                    mPm.powerHint(POWER_HINT_CPU_BOOST, msg.arg1);
                    break;
                case MSG_LAUNCH_BOOST:
                    mPm.powerHint(POWER_HINT_LAUNCH_BOOST, 0);
                    break;
                case MSG_SET_PROFILE:
                    mPm.powerHint(POWER_HINT_SET_PROFILE, msg.arg1);
                    break;
            }
        }
    }

    private final PowerManagerInternal.LowPowerModeListener mLowPowerModeListener = new
            PowerManagerInternal.LowPowerModeListener() {

                @Override
                public void onLowPowerModeChanged(boolean enabled) {
                    if (enabled == mLowPowerModeEnabled) {
                        return;
                    }
                    if (DEBUG) {
                        Slog.d(TAG, "low power mode enabled: " + enabled);
                    }
                    mLowPowerModeEnabled = enabled;
                    applyProfile();
                }
            };
}
