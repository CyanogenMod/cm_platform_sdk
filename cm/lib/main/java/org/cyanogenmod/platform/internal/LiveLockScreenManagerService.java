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

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ILiveLockScreenChangeListener;
import cyanogenmod.app.ILiveLockScreenManager;
import cyanogenmod.app.LiveLockScreenInfo;
import cyanogenmod.platform.Manifest;
import cyanogenmod.providers.CMSettings;

import libcore.util.Objects;
import org.cyanogenmod.internal.util.CmLockPatternUtils;

import java.util.Iterator;
import java.util.TreeSet;

/** {@hide} */
public class LiveLockScreenManagerService extends SystemService{
    private static final String TAG = LiveLockScreenManagerService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private Context mContext;
    private LiveLockScreenInfo mCurrentLiveLockScreen;

    private TreeSet<LiveLockScreenRecord> mLiveLockScreens;

    private CmLockPatternUtils mLockPatternUtils;

    private RemoteCallbackList<ILiveLockScreenChangeListener> mChangeListeners;

    private WorkerHandler mHandler;
    private final HandlerThread mWorkerThread = new HandlerThread("worker",
            Process.THREAD_PRIORITY_BACKGROUND);

    /**
     * Lock held while accessing mCurrentLiveLockScreen
     */
    private final Object mLock = new Object();

    private class WorkerHandler extends Handler {
        private static final int MSG_UPDATE_CURRENT = 1000;

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CURRENT:
                    handleUpdateCurrentLiveLockScreenLocked();
                    break;
                default:
                    Slog.w(TAG, "Unknown message " + msg.what);
                    break;
            }
        }
    }

    private static final class LiveLockScreenRecord implements Comparable<LiveLockScreenRecord> {
        String pkg;
        int id;
        LiveLockScreenInfo lls;
        long creationTimeMs;
        long updateTimeMs;

        public LiveLockScreenRecord(String pkg, int id, LiveLockScreenInfo lls) {
            this.pkg = pkg;
            this.id = id;
            this.lls = lls;
            this.creationTimeMs = this.updateTimeMs = System.currentTimeMillis();
        }

        @Override
        public int compareTo(LiveLockScreenRecord another) {
            int priorityDelta = another.lls.priority - this.lls.priority;
            if (priorityDelta != 0) return priorityDelta;

            // priorities are equal so compare updateTimeMs
            return another.updateTimeMs < this.updateTimeMs ? -1 :
                    another.updateTimeMs > this.updateTimeMs ? 1 : 0;
        }

        @Override
        public boolean equals(Object o) {
            final LiveLockScreenRecord other = (LiveLockScreenRecord) o;
            return this.id == other.id && this.pkg.equals(other.pkg);
        }

        @Override
        public String toString() {
            return "LiveLockScreenRecord: pkg=" + pkg +
                    ", id=" + id +
                    ", creationTimeMs=" + creationTimeMs +
                    ", updateTimeMs=" + updateTimeMs +
                    ", lls=" + lls;
        }
    }

    public LiveLockScreenManagerService(Context context) {
        super(context);
        mContext = context;
        mLiveLockScreens = new TreeSet<>();
        mLockPatternUtils = new CmLockPatternUtils(context);
        mChangeListeners = new RemoteCallbackList<>();
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
        publishBinderService(CMContextConstants.CM_LIVE_LOCK_SCREEN_SERVICE, mService);
        mWorkerThread.start();
        mHandler = new WorkerHandler(mWorkerThread.getLooper());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_LOCK_SETTINGS_READY) {
            if (DEBUG) Slog.d(TAG, "Lock settings ready");
            synchronized (mLock) {
                mCurrentLiveLockScreen = getDefaultLiveLockScreenInternal();
            }
        }
    }

    private void enqueueInternalLocked(String pkg, int id, LiveLockScreenInfo lls) {
        // clamp priority
        lls.priority = clamp(lls.priority, LiveLockScreenInfo.PRIORITY_MIN,
                LiveLockScreenInfo.PRIORITY_MAX);
        if (DEBUG) Slog.d(TAG, "enqueueInternalLocked(" + pkg + ", " + id + ", " + lls + ")");
        synchronized (mLiveLockScreens) {
            LiveLockScreenRecord newRecord = null;
            for (LiveLockScreenRecord record : mLiveLockScreens) {
                if (record.id == id && record.pkg.equals(pkg)) {
                    newRecord = record;
                    // update the record
                    newRecord.updateTimeMs = System.currentTimeMillis();
                    newRecord.lls = lls;
                    break;
                }
            }

            if (newRecord == null) {
                newRecord = new LiveLockScreenRecord(pkg, id, lls);
            }
            mLiveLockScreens.add(newRecord);
        }
        updateCurrentLiveLockScreen();
    }

    private void cancelInternalLocked(String pkg, int id) {
        if (DEBUG) Slog.d(TAG, "cancelInternalLocked(" + pkg + ", " + id + ")");
        synchronized (mLiveLockScreens) {
            Iterator<LiveLockScreenRecord> iter = mLiveLockScreens.iterator();
            while (iter.hasNext()) {
                LiveLockScreenRecord record = iter.next();
                if (record.id == id && record.pkg.equals(pkg)) {
                    enforceSamePackageOrSystem(pkg, record.lls);
                    iter.remove();
                    break;
                }
            }
        }
        updateCurrentLiveLockScreen();
    }

    private void updateCurrentLiveLockScreen() {
        mHandler.removeMessages(WorkerHandler.MSG_UPDATE_CURRENT);
        mHandler.sendEmptyMessage(WorkerHandler.MSG_UPDATE_CURRENT);
    }

    private void handleUpdateCurrentLiveLockScreenLocked() {
        LiveLockScreenRecord current = null;
        synchronized (mLiveLockScreens) {
            if (mLiveLockScreens.size() > 0) {
                current = mLiveLockScreens.first();
            }
        }

        LiveLockScreenInfo currentLls = current != null
                ? current.lls
                : getDefaultLiveLockScreenInternal();
        synchronized (mLock) {
            if (mCurrentLiveLockScreen != currentLls) {
                mCurrentLiveLockScreen = currentLls;
                notifiyChangeListeners(currentLls);
            }
        }
    }

    private void notifiyChangeListeners(LiveLockScreenInfo llsInfo) {
        if (DEBUG) Slog.d(TAG, "notifiyChangeListeners(" + llsInfo + ")");
        int N = mChangeListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            ILiveLockScreenChangeListener listener = mChangeListeners.getBroadcastItem(i);
            try {
                listener.onLiveLockScreenChanged(llsInfo);
            } catch (RemoteException e) {
                Slog.w(TAG, "Unable to notifiy change listener", e);
            }
        }
        mChangeListeners.finishBroadcast();
    }

    private void setDefaultLiveLockScreenInternal(@Nullable LiveLockScreenInfo llsInfo) {
        long token = Binder.clearCallingIdentity();
        try {
            mLockPatternUtils.setThirdPartyKeyguard(llsInfo != null ? llsInfo.component : null);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Unable to set default live lock screen", e);
        }
        Binder.restoreCallingIdentity(token);
        updateCurrentLiveLockScreen();
    }

    private LiveLockScreenInfo getDefaultLiveLockScreenInternal() {
        long token = Binder.clearCallingIdentity();
        ComponentName cn = mLockPatternUtils.getThirdPartyKeyguardComponent();
        try {
            return cn != null ? new LiveLockScreenInfo(cn, LiveLockScreenInfo.PRIORITY_MIN) : null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(Math.min(value, max), min);
    }

    private boolean hasPrivatePermissions() {
        return mContext.checkCallingPermission(Manifest.permission
                .LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Enforces the {@link cyanogenmod.platform.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_ACCESS}
     * permission.
     */
    private void enforceAccessPermission() {
        if (hasPrivatePermissions()) return;

        mContext.enforceCallingPermission(Manifest.permission.LIVE_LOCK_SCREEN_MANAGER_ACCESS,
                null);
    }

    /**
     * Enforces the
     * {@link cyanogenmod.platform.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_ACCESS_PRIVATE}
     * permission.
     */
    private void enforcePrivateAccessPermission() {
        mContext.enforceCallingPermission(
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
    private void enforceSamePackageOrSystem(String pkg, LiveLockScreenInfo llsInfo) {
        // only apps with the private permission can show/cancel live lock screens from other
        // packages
        if (hasPrivatePermissions()) return;
        if (!Objects.equal(pkg, llsInfo.component.getPackageName())) {
            throw new SecurityException("Cannot access live lock screens from other packages");
        }
    }

    private IBinder mService = new ILiveLockScreenManager.Stub() {

        @Override
        public void enqueueLiveLockScreen(String pkg, int id, LiveLockScreenInfo llsInfo)
                throws RemoteException {
            enforceAccessPermission();
            enforceSamePackageOrSystem(pkg, llsInfo);
            enqueueInternalLocked(pkg, id, llsInfo);
        }

        @Override
        public void cancelLiveLockScreen(String pkg, int id) throws RemoteException {
            enforceAccessPermission();
            cancelInternalLocked(pkg, id);
        }

        @Override
        public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
            enforceAccessPermission();
            synchronized (mLock) {
                return mCurrentLiveLockScreen;
            }
        }

        @Override
        public LiveLockScreenInfo getDefaultLiveLockScreen() throws RemoteException {
            enforcePrivateAccessPermission();
            return getDefaultLiveLockScreenInternal();
        }

        @Override
        public void setDefaultLiveLockScreen(LiveLockScreenInfo llsInfo) throws RemoteException {
            enforcePrivateAccessPermission();
            setDefaultLiveLockScreenInternal(llsInfo);
        }

        @Override
        public void setLiveLockScreenEnabled(boolean enabled) throws RemoteException {
            enforcePrivateAccessPermission();
            CMSettings.Secure.putInt(mContext.getContentResolver(),
                    CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED, enabled ? 1 : 0);
        }

        @Override
        public boolean getLiveLockScreenEnabled() throws RemoteException {
            enforceAccessPermission();
            return CMSettings.Secure.getInt(mContext.getContentResolver(),
                    CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED, 0) == 1;
        }

        @Override
        public boolean registerChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            enforcePrivateAccessPermission();
            return mChangeListeners.register(listener);
        }

        @Override
        public boolean unregisterChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            enforcePrivateAccessPermission();
            return mChangeListeners.unregister(listener);
        }
    };
}
