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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;

/**
 *
 */
public class PerformanceManager {
    
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
    
    private int mSupportedProfiles = -1;
    
    /**
     * Broadcast sent when profile is changed
     */
    public static final String POWER_PROFILE_CHANGED = "cyanogenmod.power.PROFILE_CHANGED";
    
    private final Context mContext;
    private final IPerformanceManager mService;
    private final Handler mHandler;
    
    private boolean mHasPowerProfilesSupport = false;
    
    public PerformanceManager(Context context, IPerformanceManager service, Handler handler) {
        mContext = context;
        mService = service;
        mHandler = handler;

        try {
            if (mService != null) {
                mHasPowerProfilesSupport = mService.hasPowerProfiles();
            }
        } catch (RemoteException e) {
        }
    }
    
    /**
     * Boost the CPU. Boosts the cpu for the given duration in microseconds.
     * Requires the {@link android.Manifest.permission#CPU_BOOST} permission.
     *
     * @param duration in microseconds to boost the CPU
     */
    public void cpuBoost(int duration)
    {
        try {
            if (mService != null) {
                mService.cpuBoost(duration);
            }
        } catch (RemoteException e) {
        }
    }
    
    /**
     * True if the system supports power profiles
     */
    public boolean hasPowerProfiles() {
        return mHasPowerProfilesSupport;
    }

    /**
     * Set the system power profile
     *
     * @throws IllegalArgumentException if invalid
     */
    public boolean setPowerProfile(int profile) {
        if (!hasPowerProfiles()) {
            throw new IllegalArgumentException("Power profiles not enabled on this system!");
        }

        boolean changed = false;
        try {
            if (mService != null) {
                changed = mService.setPowerProfile(profile);
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
        if (hasPowerProfiles()) {
            try {
                if (mService != null) {
                    ret = mService.getPowerProfile();
                }
            } catch (RemoteException e) {
                // nothing
            }
        }
        return ret;
    }
}
