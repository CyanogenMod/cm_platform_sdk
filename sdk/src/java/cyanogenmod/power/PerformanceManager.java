/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package cyanogenmod.power;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import cyanogenmod.app.CMContextConstants;

/**
 *
 */
public class PerformanceManager {

    public static final String TAG = "PerformanceManager";

    /**
     * Power save profile
     *
     * This mode sacrifices performance for maximum power saving.
     */
    public static final int PROFILE_POWER_SAVE = 0;

    /**
     * Balanced power profile
     * 
     * The default mode for balanced power savings and performance
     */
    public static final int PROFILE_BALANCED = 1;

    /**
     * High-performance profile
     * 
     * This mode sacrifices power for maximum performance
     */
    public static final int PROFILE_HIGH_PERFORMANCE = 2;

    /**
     * Power save bias profile
     * 
     * This mode decreases performance slightly to improve
     * power savings. 
     */
    public static final int PROFILE_BIAS_POWER_SAVE = 3;
    
    /**
     * Performance bias profile
     * 
     * This mode improves performance at the cost of some power.
     */
    public static final int PROFILE_BIAS_PERFORMANCE = 4;

    /**
     * @hide
     */
    public static final int[] POSSIBLE_POWER_PROFILES = new int[] {
            PROFILE_POWER_SAVE,
            PROFILE_BALANCED,
            PROFILE_HIGH_PERFORMANCE,
            PROFILE_BIAS_POWER_SAVE,
            PROFILE_BIAS_PERFORMANCE
    };

    private int mNumberOfProfiles = 0;

    /**
     * Broadcast sent when profile is changed
     */
    public static final String POWER_PROFILE_CHANGED = "cyanogenmod.power.PROFILE_CHANGED";

    private static IPerformanceManager sService;
    private static PerformanceManager sInstance;

    private PerformanceManager(Context context) {
        sService = getService();
        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.PERFORMANCE) && sService == null) {
            throw new RuntimeException("Unable to get PerformanceManagerService. The service" +
                    " either crashed, was not started, or the interface has been called to early" +
                    " in SystemServer init");
        }
        try {
            if (sService != null) {
                mNumberOfProfiles = sService.getNumberOfProfiles();
            }
        } catch (RemoteException e) {
        }
    }

    public static PerformanceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PerformanceManager(context);
        }
        return sInstance;
    }

    /** @hide */
    public static IPerformanceManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_PERFORMANCE_SERVICE);
        if (b != null) {
            sService = IPerformanceManager.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    private boolean checkService() {
        if (sService == null) {
            Log.w(TAG, "not connected to PerformanceManagerService");
            return false;
        }
        return true;
    }

    /**
     * Boost the CPU. Boosts the cpu for the given duration in microseconds.
     * Requires the {@link android.Manifest.permission#CPU_BOOST} permission.
     *
     * @param duration in microseconds to boost the CPU
     * @hide
     */
    public void cpuBoost(int duration)
    {
        try {
            if (checkService()) {
                sService.cpuBoost(duration);
            }
        } catch (RemoteException e) {
        }
    }

    /**
     * Returns the number of supported profiles, -1 if unsupported
     * This is queried via the PowerHAL.
     */
    public int getNumberOfProfiles() {
        return mNumberOfProfiles;
    }

    /**
     * Set the system power profile
     *
     * @throws IllegalArgumentException if invalid
     */
    public boolean setPowerProfile(int profile) {
        if (mNumberOfProfiles < 1) {
            throw new IllegalArgumentException("Power profiles not enabled on this system!");
        }

        boolean changed = false;
        try {
            if (checkService()) {
                changed = sService.setPowerProfile(profile);
            }
        } catch (RemoteException e) {
            throw new IllegalArgumentException(e);
        }
        return changed;
    }

    /**
     * Gets the current power profile
     *
     * Returns null if power profiles are not enabled
     */
    public int getPowerProfile() {
        int ret = -1;
        if (mNumberOfProfiles > 0) {
            try {
                if (checkService()) {
                    ret = sService.getPowerProfile();
                }
            } catch (RemoteException e) {
                // nothing
            }
        }
        return ret;
    }

    /**
     * Check if profile has app-specific profiles
     *
     * Returns true if profile has app-specific profiles.
     */
    public boolean getProfileHasAppProfiles(int profile) {
        boolean ret = false;
        if (mNumberOfProfiles > 0) {
            try {
                if (checkService()) {
                    ret = sService.getProfileHasAppProfiles(profile);
                }
            } catch (RemoteException e) {
                // nothing
            }
        }
        return ret;
    }
}
