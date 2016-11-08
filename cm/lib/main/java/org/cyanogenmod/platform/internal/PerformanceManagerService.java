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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.server.ServiceThread;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManagerInternal;
import cyanogenmod.power.PerformanceProfile;

import static cyanogenmod.power.PerformanceManager.PROFILE_BALANCED;
import static cyanogenmod.power.PerformanceManager.PROFILE_HIGH_PERFORMANCE;
import static cyanogenmod.power.PerformanceManager.PROFILE_POWER_SAVE;
import static cyanogenmod.providers.CMSettings.Secure.APP_PERFORMANCE_PROFILES_ENABLED;
import static cyanogenmod.providers.CMSettings.Secure.PERFORMANCE_PROFILE;
import static cyanogenmod.providers.CMSettings.Secure.getInt;
import static cyanogenmod.providers.CMSettings.Secure.getUriFor;
import static cyanogenmod.providers.CMSettings.Secure.putInt;

/**
 * @hide
 */
public class PerformanceManagerService extends CMSystemService {

    private static final String TAG = "PerformanceManager";

    private static final boolean DEBUG = false;

    private final Context mContext;

    private final LinkedHashMap<Pattern, Integer>    mAppProfiles = new LinkedHashMap<>();
    private final ArrayMap<Integer, PerformanceProfile> mProfiles = new ArrayMap<>();

    private int mNumProfiles = 0;

    private final ServiceThread mHandlerThread;
    private final BoostHandler mHandler;

    // keep in sync with hardware/libhardware/include/hardware/power.h
    private final int POWER_HINT_CPU_BOOST    = 0x00000110;
    private final int POWER_HINT_LAUNCH_BOOST = 0x00000111;
    private final int POWER_HINT_SET_PROFILE  = 0x00000113;

    private final int POWER_FEATURE_SUPPORTED_PROFILES = 0x00001000;

    private PowerManagerInternal mPm;

    // Observes user-controlled settings
    private PerformanceSettingsObserver mObserver;

    // Max time (microseconds) to allow a CPU boost for
    private static final int MAX_CPU_BOOST_TIME = 5000000;

    // Standard weights
    private static final float WEIGHT_POWER_SAVE       = 0.0f;
    private static final float WEIGHT_BALANCED         = 0.5f;
    private static final float WEIGHT_HIGH_PERFORMANCE = 1.0f;

    // Take lock when accessing mProfiles
    private final Object mLock = new Object();

    // Manipulate state variables under lock
    private boolean mLowPowerModeEnabled = false;
    private boolean mSystemReady         = false;
    private boolean mBoostEnabled        = true;
    private int     mUserProfile         = -1;
    private int     mActiveProfile       = -1;
    private String  mCurrentActivityName = null;

    // Dumpable circular buffer for boost logging
    private final BoostLog mBoostLog = new BoostLog();

    // Events on the handler
    private static final int MSG_CPU_BOOST    = 1;
    private static final int MSG_LAUNCH_BOOST = 2;
    private static final int MSG_SET_PROFILE  = 3;

    public PerformanceManagerService(Context context) {
        super(context);

        mContext = context;
        Resources res = context.getResources();

        String[] activities = res.getStringArray(R.array.config_auto_perf_activities);
        if (activities != null && activities.length > 0) {
            for (int i = 0; i < activities.length; i++) {
                String[] info = activities[i].split(",");
                if (info.length == 2) {
                    mAppProfiles.put(Pattern.compile(info[0]), Integer.valueOf(info[1]));
                    if (DEBUG) {
                        Slog.d(TAG, String.format(Locale.US,"App profile #%d: %s => %s",
                                i, info[0], info[1]));
                    }
                }
            }
        }

        // We need a higher priority thread to handle these requests in front of
        // everything else asynchronously
        mHandlerThread = new ServiceThread(TAG,
                Process.THREAD_PRIORITY_DISPLAY, false /*allowIo*/);
        mHandlerThread.start();

        mHandler = new BoostHandler(mHandlerThread.getLooper());
    }

    private class PerformanceSettingsObserver extends ContentObserver {

        private final Uri APP_PERFORMANCE_PROFILES_ENABLED_URI =
                getUriFor(APP_PERFORMANCE_PROFILES_ENABLED);

        private final Uri PERFORMANCE_PROFILE_URI =
                getUriFor(PERFORMANCE_PROFILE);

        private final ContentResolver mCR;

        public PerformanceSettingsObserver(Context context, Handler handler) {
            super(handler);
            mCR = context.getContentResolver();
        }

        public void observe(boolean enabled) {
            if (enabled) {
                mCR.registerContentObserver(APP_PERFORMANCE_PROFILES_ENABLED_URI, false, this);
                mCR.registerContentObserver(PERFORMANCE_PROFILE_URI, false, this);
                onChange(false);

            } else {
                mCR.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            int profile = getInt(mCR, PERFORMANCE_PROFILE, PROFILE_BALANCED);
            boolean boost = getInt(mCR, APP_PERFORMANCE_PROFILES_ENABLED, 1) == 1;

            synchronized (mLock) {
                if (hasProfiles() && mProfiles.containsKey(profile)) {
                    boost = boost && mProfiles.get(profile).isBoostEnabled();
                }

                mBoostEnabled = boost;
                if (mUserProfile < 0) {
                    mUserProfile = profile;
                }
            }
        }
    };

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.PERFORMANCE;
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PERFORMANCE_SERVICE, mBinder);
        publishLocalService(PerformanceManagerInternal.class, new LocalService());
    }

    private void populateProfilesLocked() {
        mProfiles.clear();

        Resources res = mContext.getResources();
        String[] profileNames = res.getStringArray(R.array.perf_profile_entries);
        int[] profileIds = res.getIntArray(R.array.perf_profile_values);
        String[] profileWeights = res.getStringArray(R.array.perf_profile_weights);
        String[] profileDescs = res.getStringArray(R.array.perf_profile_summaries);

        for (int i = 0; i < profileIds.length; i++) {
            if (profileIds[i] >= mNumProfiles) {
                continue;
            }
            float weight = Float.valueOf(profileWeights[i]);
            mProfiles.put(profileIds[i], new PerformanceProfile(profileIds[i],
                    weight, profileNames[i], profileDescs[i], shouldUseOptimizations(weight)));
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY && !mSystemReady) {
            synchronized (mLock) {
                mPm = getLocalService(PowerManagerInternal.class);
                mNumProfiles = mPm.getFeature(POWER_FEATURE_SUPPORTED_PROFILES);

                if (hasProfiles()) {
                    populateProfilesLocked();

                    mObserver = new PerformanceSettingsObserver(mContext, mHandler);
                    mObserver.observe(true);
                }

                mSystemReady = true;

                if (hasProfiles()) {
                    if (mUserProfile == PROFILE_HIGH_PERFORMANCE) {
                        Slog.w(TAG, "Reverting profile HIGH_PERFORMANCE to BALANCED");
                        setPowerProfileLocked(PROFILE_BALANCED, true);
                    } else {
                        setPowerProfileLocked(mUserProfile, true);
                    }

                    mPm.registerLowPowerModeObserver(mLowPowerModeListener);
                    mContext.registerReceiver(mLocaleChangedReceiver,
                            new IntentFilter(Intent.ACTION_LOCALE_CHANGED));
                }
            }
        }
    }

    private boolean hasProfiles() {
        return mNumProfiles > 0;
    }

    private boolean hasAppProfiles() {
        return hasProfiles() && mBoostEnabled && mAppProfiles.size() > 0;
    }

    /**
     * Apply a power profile and persist if fromUser = true
     * <p>
     * Must call with lock held.
     *
     * @param profile  power profile
     * @param fromUser true to persist the profile
     * @return true if the active profile changed
     */
    private boolean setPowerProfileLocked(int profile, boolean fromUser) {
        if (DEBUG) {
            Slog.v(TAG, String.format(Locale.US,"setPowerProfileL(%d, fromUser=%b)", profile, fromUser));
        }

        if (!mSystemReady) {
            Slog.e(TAG, "System is not ready, dropping profile request");
            return false;
        }

        if (!mProfiles.containsKey(profile)) {
            Slog.e(TAG, "Invalid profile: " + profile);
            return false;
        }

        boolean isProfileSame = profile == mActiveProfile;

        if (!isProfileSame) {
            if (profile == PROFILE_POWER_SAVE) {
                // Handle the case where toggle power saver mode failed
                long token = Binder.clearCallingIdentity();
                try {
                    if (!mPm.setPowerSaveMode(true)) {
                        return false;
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else if (mActiveProfile == PROFILE_POWER_SAVE) {
                long token = Binder.clearCallingIdentity();
                mPm.setPowerSaveMode(false);
                Binder.restoreCallingIdentity(token);
            }
        }

        /**
         * It's possible that mCurrrentProfile != getUserProfile() because of a
         * per-app profile. Store the user's profile preference and then bail
         * early if there is no work to be done.
         */
        if (fromUser) {
            putInt(mContext.getContentResolver(), PERFORMANCE_PROFILE, profile);
            mUserProfile = profile;
        }

        if (isProfileSame) {
            return false;
        }

        // Enforce the performance access permission declared by cm's res package
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.PERFORMANCE_ACCESS, null);

        long token = Binder.clearCallingIdentity();

        mActiveProfile = profile;

        mHandler.obtainMessage(MSG_SET_PROFILE, profile,
                (fromUser ? 1 : 0)).sendToTarget();

        Binder.restoreCallingIdentity(token);

        return true;
    }

    private int getProfileForActivity(String componentName) {
        int profile = -1;
        if (componentName != null) {
            for (Map.Entry<Pattern, Integer> entry : mAppProfiles.entrySet()) {
                if (entry.getKey().matcher(componentName).matches()) {
                    profile = entry.getValue();
                    break;
                }
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "getProfileForActivity: activity=" + componentName + " profile=" + profile);
        }
        return profile < 0 ? mUserProfile : profile;
    }

    private static boolean shouldUseOptimizations(float weight) {
        return weight >= (WEIGHT_BALANCED / 2) &&
               weight <= (WEIGHT_BALANCED + (WEIGHT_BALANCED / 2));
    }

    private void cpuBoostInternal(int duration) {
        if (!mSystemReady) {
            Slog.e(TAG, "System is not ready, dropping cpu boost request");
            return;
        }

        if (!mBoostEnabled) {
            return;
        }

        if (duration > 0 && duration <= MAX_CPU_BOOST_TIME) {
            mHandler.obtainMessage(MSG_CPU_BOOST, duration, 0).sendToTarget();
        } else {
            Slog.e(TAG, "Invalid boost duration: " + duration);
        }
    }

    private void applyAppProfileLocked(boolean fromUser) {
        if (!hasProfiles()) {
            // don't have profiles, bail.
            return;
        }

        final int profile;
        if (mLowPowerModeEnabled) {
            // LPM always wins
            profile = PROFILE_POWER_SAVE;
        } else if (fromUser && mActiveProfile == PROFILE_POWER_SAVE) {
            // leaving LPM
            profile = PROFILE_BALANCED;
        } else if (hasAppProfiles()) {
            profile = getProfileForActivity(mCurrentActivityName);
        } else {
            profile = mUserProfile;
        }

        setPowerProfileLocked(profile, fromUser);
    }

    private final IBinder mBinder = new IPerformanceManager.Stub() {

        @Override
        public boolean setPowerProfile(int profile) {
            synchronized (mLock) {
                return setPowerProfileLocked(profile, true);
            }
        }

        /**
         * Boost the CPU
         *
         * @param duration Duration to boost the CPU for, in milliseconds.
         */
        @Override
        public void cpuBoost(int duration) {
            cpuBoostInternal(duration);
        }

        @Override
        public int getPowerProfile() {
            synchronized (mLock) {
                return mUserProfile;
            }
        }

        @Override
        public PerformanceProfile getPowerProfileById(int profile) {
            synchronized (mLock) {
                return mProfiles.get(profile);
            }
        }

        @Override
        public PerformanceProfile getActivePowerProfile() {
            synchronized (mLock) {
                return mProfiles.get(mUserProfile);
            }
        }

        @Override
        public int getNumberOfProfiles() {
            return mNumProfiles;
        }

        @Override
        public PerformanceProfile[] getPowerProfiles() throws RemoteException {
            synchronized (mLock) {
                return mProfiles.values().toArray(
                        new PerformanceProfile[mProfiles.size()]);
            }
        }

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);

            synchronized (mLock) {
                pw.println();
                pw.println("PerformanceManager Service State:");
                pw.println();
                pw.println(" Boost enabled: " + mBoostEnabled);

                if (!hasProfiles()) {
                    pw.println(" No profiles available.");
                } else {
                    pw.println(" User-selected profile: " +
                            Objects.toString(mProfiles.get(mUserProfile)));
                    if (mUserProfile != mActiveProfile) {
                        pw.println(" System-selected profile: " +
                                Objects.toString(mProfiles.get(mActiveProfile)));
                    }
                    pw.println();
                    pw.println(" Supported profiles:");
                    for (Map.Entry<Integer, PerformanceProfile> profile : mProfiles.entrySet()) {
                        pw.println("  " + profile.getKey() + ": " + profile.getValue().toString());
                    }
                    if (hasAppProfiles()) {
                        pw.println();
                        pw.println(" App trigger count: " + mAppProfiles.size());
                    }
                    pw.println();
                    mBoostLog.dump(pw);
                }
            }
        }
    };

    private final class LocalService implements PerformanceManagerInternal {

        @Override
        public void cpuBoost(int duration) {
            cpuBoostInternal(duration);
        }

        @Override
        public void launchBoost(int pid, String packageName) {
            if (!mSystemReady) {
                Slog.e(TAG, "System is not ready, dropping launch boost request");
                return;
            }
            if (!mBoostEnabled) {
                return;
            }
            mHandler.obtainMessage(MSG_LAUNCH_BOOST, pid, 0, packageName).sendToTarget();
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

            synchronized (mLock) {
                mCurrentActivityName = activityName;
                applyAppProfileLocked(false);
            }
        }
    }

    private static class BoostLog {
        static final int APP_PROFILE  = 0;
        static final int CPU_BOOST    = 1;
        static final int LAUNCH_BOOST = 2;
        static final int USER_PROFILE = 3;

        static final String[] EVENTS = new String[] {
                "APP_PROFILE", "CPU_BOOST", "LAUNCH_BOOST", "USER_PROFILE" };

        private static final int LOG_BUF_SIZE = 25;

        static class Entry {
            private final long timestamp;
            private final int event;
            private final String info;

            Entry(long timestamp_, int event_, String info_) {
                timestamp = timestamp_;
                event = event_;
                info = info_;
            }
        }

        private final ArrayDeque<Entry> mBuffer = new ArrayDeque<>(LOG_BUF_SIZE);

        void log(int event, String info) {
            synchronized (mBuffer) {
                mBuffer.add(new Entry(System.currentTimeMillis(), event, info));
                if (mBuffer.size() >= LOG_BUF_SIZE) {
                    mBuffer.poll();
                }
            }
        }

        void dump(PrintWriter pw) {
            synchronized (mBuffer) {
                pw.println(" Boost log:");
                for (Entry entry : mBuffer) {
                    pw.println(String.format("  %1$tH:%1$tM:%1$tS.%1$tL: %2$14s  %3$s",
                            new Date(entry.timestamp), EVENTS[entry.event], entry.info));
                }
                pw.println();
            }
        }
    }

    /**
     * Handler for asynchronous operations performed by the performance manager.
     */
    private final class BoostHandler extends Handler {

        public BoostHandler(Looper looper) {
            super(looper, null, true /*async*/);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CPU_BOOST:
                    mPm.powerHint(POWER_HINT_CPU_BOOST, msg.arg1);
                    mBoostLog.log(BoostLog.CPU_BOOST, "duration=" + msg.arg1);
                    break;
                case MSG_LAUNCH_BOOST:
                    int pid = msg.arg1;
                    String packageName = (String) msg.obj;
                    if (NativeHelper.isNativeLibraryAvailable() && packageName != null) {
                        native_launchBoost(pid, packageName);
                    }
                    mBoostLog.log(BoostLog.LAUNCH_BOOST, "package=" + packageName);
                    break;
                case MSG_SET_PROFILE:
                    mPm.powerHint(POWER_HINT_SET_PROFILE, msg.arg1);
                    mBoostLog.log((msg.arg2 == 1 ? BoostLog.USER_PROFILE : BoostLog.APP_PROFILE),
                            "profile=" + msg.arg1);
                    break;
            }
        }
    }

    private final PowerManagerInternal.LowPowerModeListener mLowPowerModeListener = new
            PowerManagerInternal.LowPowerModeListener() {

                @Override
                public void onLowPowerModeChanged(boolean enabled) {
                    synchronized (mLock) {
                        if (enabled == mLowPowerModeEnabled) {
                            return;
                        }
                        if (DEBUG) {
                            Slog.d(TAG, "low power mode enabled: " + enabled);
                        }
                        mLowPowerModeEnabled = enabled;
                        applyAppProfileLocked(true);
                    }
                }
            };

    private final BroadcastReceiver mLocaleChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mLock) {
                populateProfilesLocked();
            }
        }
    };

    private native final void native_launchBoost(int pid, String packageName);
}
