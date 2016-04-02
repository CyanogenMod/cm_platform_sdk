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
import android.app.AppGlobals;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import cyanogenmod.platform.Manifest;

/**
 * Base Live lock screen manager service to be extended by applications that implement the
 * {@link LiveLockScreenManager#SERVICE_INTERFACE}
 *
 * @hide
 */
abstract public class BaseLiveLockManagerService extends Service
        implements ILiveLockScreenManagerProvider {
    private static final String TAG = BaseLiveLockManagerService.class.getSimpleName();

    private final RemoteCallbackList<ILiveLockScreenChangeListener> mChangeListeners =
            new RemoteCallbackList<>();

    @Override
    public final IBinder onBind(Intent intent) {
        return mService;
    }

    @Override
    public final IBinder asBinder() {
        return mService;
    }

    @Override
    abstract public void enqueueLiveLockScreen(String pkg, int id, LiveLockScreenInfo lls,
            int[] idReceived, int userId) throws RemoteException;

    @Override
    abstract public void cancelLiveLockScreen(String pkg, int id, int userId)
            throws RemoteException;

    @Override
    abstract public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException;

    @Override
    abstract public void updateDefaultLiveLockScreen(LiveLockScreenInfo llsInfo)
            throws RemoteException;

    @Override
    public boolean getLiveLockScreenEnabled() throws RemoteException {
        return false;
    }

    @Override
    public final boolean registerChangeListener(
            ILiveLockScreenChangeListener listener) throws RemoteException {
        return mChangeListeners.register(listener);
    }

    @Override
    public final boolean unregisterChangeListener(
            ILiveLockScreenChangeListener listener) throws RemoteException {
        return mChangeListeners.unregister(listener);
    }

    /**
     * This method should be called whenever there is an update to the current Live lock screen
     * to be displayed.
     *
     * @param llsInfo LiveLockScreenInfo for the current Live lock screen
     */
    protected final void notifyChangeListeners(LiveLockScreenInfo llsInfo) {
        int N = mChangeListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            ILiveLockScreenChangeListener listener = mChangeListeners.getBroadcastItem(i);
            try {
                listener.onLiveLockScreenChanged(llsInfo);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to notifiy change listener", e);
            }
        }
        mChangeListeners.finishBroadcast();
    }

    /**
     * Returns true if the caller has been granted the
     * {@link cyanogenmod.platform.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE}
     * permission.
     *
     * @return
     */
    private final boolean hasPrivatePermissions() {
        return checkCallingPermission(Manifest.permission
                .LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Enforces the {@link cyanogenmod.platform.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_ACCESS}
     * permission.
     */
    protected final void enforceAccessPermission() {
        if (hasPrivatePermissions()) return;

        enforceCallingPermission(Manifest.permission.LIVE_LOCK_SCREEN_MANAGER_ACCESS,
                null);
    }

    /**
     * Enforces the
     * {@link cyanogenmod.platform.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE}
     * permission.
     */
    protected final void enforcePrivateAccessPermission() {
        enforceCallingPermission(
                Manifest.permission.LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE, null);
    }

    /**
     * Enforces the LLS being shown/canceled is from the calling package or from a system app that
     * has the
     * {@link cyanogenmod.platform.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE}
     * permission.
     *
     * @param pkg Package name of caller
     * @param llsInfo Live lock screen info with component to check
     */
    protected final void enforceSamePackageOrSystem(String pkg,
            @NonNull LiveLockScreenInfo llsInfo) {
        // only apps with the private permission can show/cancel live lock screens from other
        // packages
        if (hasPrivatePermissions()) return;

        if (llsInfo.component != null && !llsInfo.component.getPackageName().equals(pkg)) {
            throw new SecurityException("Modifying Live lock screen from different packages not " +
                    "allowed.  Calling package: " + pkg + " LLS package: " +
                    llsInfo.component.getPackageName());
        }

        final int uid = Binder.getCallingUid();
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(
                    pkg, 0, UserHandle.getCallingUserId());
            if (ai == null) {
                throw new SecurityException("Unknown package " + pkg);
            }
            if (!UserHandle.isSameApp(ai.uid, uid)) {
                throw new SecurityException("Calling uid " + uid + " gave package"
                        + pkg + " which is owned by uid " + ai.uid);
            }
        } catch (RemoteException re) {
            throw new SecurityException("Unknown package " + pkg + "\n" + re);
        }
    }

    private final IBinder mService = new ILiveLockScreenManagerProvider.Stub() {
        @Override
        public void enqueueLiveLockScreen(String pkg, int id, LiveLockScreenInfo llsInfo,
                int[] idReceived, int userId) throws RemoteException {
            enforceAccessPermission();
            enforceSamePackageOrSystem(pkg, llsInfo);
            BaseLiveLockManagerService.this.enqueueLiveLockScreen(pkg, id, llsInfo, idReceived,
                    userId);
        }

        @Override
        public void cancelLiveLockScreen(String pkg, int id, int userId) throws RemoteException {
            enforceAccessPermission();
            BaseLiveLockManagerService.this.cancelLiveLockScreen(pkg, id, userId);
        }

        @Override
        public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
            enforceAccessPermission();
            return BaseLiveLockManagerService.this.getCurrentLiveLockScreen();
        }

        @Override
        public void updateDefaultLiveLockScreen(LiveLockScreenInfo llsInfo) throws RemoteException {
            enforcePrivateAccessPermission();
            BaseLiveLockManagerService.this.updateDefaultLiveLockScreen(llsInfo);
        }

        @Override
        public boolean getLiveLockScreenEnabled() throws RemoteException {
            enforceAccessPermission();
            return BaseLiveLockManagerService.this.getLiveLockScreenEnabled();
        }

        @Override
        public boolean registerChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            enforcePrivateAccessPermission();
            return BaseLiveLockManagerService.this.registerChangeListener(listener);
        }

        @Override
        public boolean unregisterChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            enforcePrivateAccessPermission();
            return BaseLiveLockManagerService.this.unregisterChangeListener(listener);
        }
    };
}
