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

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ILiveLockScreenManager;
import cyanogenmod.app.LiveLockScreenInfo;
import cyanogenmod.providers.CMSettings;
import org.cyanogenmod.internal.util.CmLockPatternUtils;

import java.util.Iterator;
import java.util.TreeSet;

/** {@hide} */
public class LiveLockScreenManagerService extends SystemService{
    private static final String TAG = LiveLockScreenInfo.class.getSimpleName();

    private Context mContext;
    private LiveLockScreenInfo mCurrentLiveLockScreen;

    private TreeSet<LiveLockScreenRecord> mLiveLockScreens;

    private CmLockPatternUtils mLockPatternUtils;

    public LiveLockScreenManagerService(Context context) {
        super(context);
        mContext = context;
        mLiveLockScreens = new TreeSet<>();
        mLockPatternUtils = new CmLockPatternUtils(context);
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_LIVE_LOCK_SCREEN_SERVICE, mService);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_LOCK_SETTINGS_READY) {
            mCurrentLiveLockScreen = getDefaultLiveLockScreen();
        }
    }

    private void showInternalLocked(String pkg, int id, LiveLockScreenInfo lls) {
        synchronized (mLiveLockScreens) {
            LiveLockScreenRecord newRecord = null;
            for (LiveLockScreenRecord record : mLiveLockScreens) {
                if (record.id == id && record.pkg.equals(pkg)) {
                    newRecord = record;
                    // update the time
                    newRecord.updateTimeMs = System.currentTimeMillis();
                    break;
                }
            }

            if (newRecord == null) {
                newRecord = new LiveLockScreenRecord(pkg, id, lls);
            }
            mLiveLockScreens.add(newRecord);
        }
        updateCurrentLiveLockScreenLocked();
    }

    private void cancelInternalLocked(String pkg, int id) {
        synchronized (mLiveLockScreens) {
            Iterator<LiveLockScreenRecord> iter = mLiveLockScreens.iterator();
            while (iter.hasNext()) {
                LiveLockScreenRecord record = iter.next();
                if (record.id == id && record.pkg.equals(pkg)) {
                    iter.remove();
                    break;
                }
            }
        }
        updateCurrentLiveLockScreenLocked();
    }

    private void updateCurrentLiveLockScreenLocked() {
        LiveLockScreenRecord current = null;
        synchronized (mLiveLockScreens) {
            current = mLiveLockScreens.first();
        }

        LiveLockScreenInfo currentLls = current != null ? current.lls : getDefaultLiveLockScreen();
        if (mCurrentLiveLockScreen != currentLls) {
            mCurrentLiveLockScreen = currentLls;
            // TODO: notify clients
        }
    }

    private LiveLockScreenInfo getDefaultLiveLockScreen() {
        ComponentName cn = mLockPatternUtils.getThirdPartyKeyguardComponent();
        return cn != null ? new LiveLockScreenInfo(cn, LiveLockScreenInfo.PRIORITY_MIN) : null;
    }

    private IBinder mService = new ILiveLockScreenManager.Stub() {

        @Override
        public void show(String pkg, int id, LiveLockScreenInfo lls)
                throws RemoteException {
            showInternalLocked(pkg, id, lls);
        }

        @Override
        public void cancel(String pkg, int id) throws RemoteException {
            cancelInternalLocked(pkg, id);
        }

        @Override
        public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
            return mCurrentLiveLockScreen;
        }

        @Override
        public void setLiveLockScreenEnabled(boolean enabled) throws RemoteException {
            CMSettings.Secure.putInt(mContext.getContentResolver(),
                    CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED, enabled ? 1 : 0);
        }

        @Override
        public boolean getLiveLockScreenEnabled() throws RemoteException {
            return CMSettings.Secure.getInt(mContext.getContentResolver(),
                    CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED, 0) == 1;
        }
    };

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
}
