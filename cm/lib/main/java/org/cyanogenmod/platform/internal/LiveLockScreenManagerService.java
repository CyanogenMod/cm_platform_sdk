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

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ILiveLockScreenManager;
import cyanogenmod.app.LiveLockScreen;

import java.util.PriorityQueue;

/** {@hide} */
public class LiveLockScreenManagerService extends SystemService{
    private static final String TAG = LiveLockScreen.class.getSimpleName();

    private Context mContext;
    private Handler mHandler;

    private PriorityQueue<LiveLockScreenRecord> mLiveLockScreenQueue;

    public LiveLockScreenManagerService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_LIVE_LOCK_SCREEN_SERVICE, mService);
    }

    private IBinder mService = new ILiveLockScreenManager.Stub() {

        @Override
        public void show(String tag, int id, LiveLockScreen lls) throws RemoteException {

        }

        @Override
        public void cancel(String tag, int id) throws RemoteException {

        }

        @Override
        public LiveLockScreen getCurrentLiveLockScreen() throws RemoteException {
            return null;
        }

        @Override
        public void setLiveLockScreenEnabled() throws RemoteException {

        }

        @Override
        public boolean getLiveLockScreenEnabled() throws RemoteException {
            return false;
        }
    };

    private static final class LiveLockScreenRecord implements Comparable<LiveLockScreenRecord> {
        LiveLockScreen lls;
        String pkg;
        long creationTimeMs;
        long updateTimeMs;

        public LiveLockScreenRecord(String pkg, LiveLockScreen lls) {
            this.pkg = pkg;
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
        public String toString() {
            return "LiveLockScreenRecord: pkg=" + pkg +
                    " creationTimeMs=" + creationTimeMs +
                    " updateTimeMs=" + updateTimeMs +
                    " lls=" + lls;
        }
    }
}
