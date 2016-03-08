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

package cyanogenmod.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/**
 * Manages enabling/disabling Live lock screens as well as what Live lock screen to display when
 * enabled.
 */
public class LiveLockScreenManager {
    private static final String TAG = LiveLockScreenManager.class.getSimpleName();
    private static ILiveLockScreenManager sService;
    private static LiveLockScreenManager sInstance;

    private Context mContext;

    /**
     * The {@link android.content.Intent} that must be declared as handled by the service.
     */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE
            = "cyanogenmod.app.LiveLockScreenManagerService";

    private LiveLockScreenManager(Context context) {
        mContext = context;
        sService = getService();
    }

    private ILiveLockScreenManager getService() {
        if (sService == null) {
            IBinder b = ServiceManager.getService(CMContextConstants.CM_LIVE_LOCK_SCREEN_SERVICE);
            if (b != null) {
                sService = ILiveLockScreenManager.Stub.asInterface(b);
            }
        }

        return sService;
    }

    private void logServiceException(Exception e) {
        Log.w(TAG, "Unable to access LiveLockScreenServiceBroker", e);
    }

    public static LiveLockScreenManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LiveLockScreenManager(context);
        }

        return sInstance;
    }

    /**
     * Requests a Live lock screen, defined in {@param lls}, to be displayed with the given id.
     * @param id An identifier for this notification unique within your application.
     * @param lls A {@link LiveLockScreenInfo} object describing what Live lock screen to show the
     *            user.
     */
    public void show(int id, @NonNull LiveLockScreenInfo lls) {
        String pkg = mContext.getPackageName();
        try {
            sService.enqueueLiveLockScreen(pkg, id, lls);
        } catch (RemoteException e) {
            logServiceException(e);
        }
    }

    /**
     * Cancels a previously shown Live lock screen.
     * @param id An identifier for this notification unique within your application.
     */
    public void cancel(int id) {
        String pkg = mContext.getPackageName();
        try {
            sService.cancelLiveLockScreen(pkg, id);
        } catch (RemoteException e) {
            logServiceException(e);
        }
    }

    /** @hide */
    public LiveLockScreenInfo getCurrentLiveLockScreen() {
        LiveLockScreenInfo lls = null;
        try {
            lls = sService.getCurrentLiveLockScreen();
        } catch (RemoteException e) {
            logServiceException(e);
        }

        return lls;
    }

    /** @hide */
    public boolean getLiveLockScreenEnabled() {
        try {
            return sService.getLiveLockScreenEnabled();
        } catch (RemoteException e) {
            logServiceException(e);
        }

        return false;
    }

    /** @hide */
    public void setLiveLockScreenEnabled(boolean enabled) {
        try {
            sService.setLiveLockScreenEnabled(enabled);
        } catch (RemoteException e) {
            logServiceException(e);
        }
    }

    /** @hide */
    public void setDefaultLiveLockScreen(@Nullable LiveLockScreenInfo llsInfo) {
        try {
            sService.setDefaultLiveLockScreen(llsInfo);
        } catch (RemoteException e) {
            logServiceException(e);
        }
    }

    /** @hide */
    public LiveLockScreenInfo getDefaultLiveLockScreen() {
        try {
            return sService.getDefaultLiveLockScreen();
        } catch (RemoteException e) {
            logServiceException(e);
        }

        return null;
    }
}
