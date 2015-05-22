/**
 * Copyright (c) 2015, The CyanogenMod Project
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.ProfileManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import android.os.UserHandle;
import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.Action;
import cyanogenmod.app.IProfilePluginService;
import cyanogenmod.app.ProfileServiceAction;
import cyanogenmod.app.ProfileServiceTrigger;
import cyanogenmod.app.Trigger;

import java.util.List;

public class ProfilePluginService extends SystemService {

    private Map<String, ProfileServiceTrigger> mTriggers =
            Collections.synchronizedMap(new HashMap<String, ProfileServiceTrigger>());
    private Map<String, ProfileServiceAction> mActions =
            Collections.synchronizedMap(new HashMap<String, ProfileServiceAction>());
    private PackageManager mPackageManager;

    public ProfilePluginService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PROFILE_PLUGIN_SERVICE, mService);
        mPackageManager = getContext().getPackageManager();
    }

    private String getPackageNameForCaller() {
        return mPackageManager.getNameForUid(Binder.getCallingUid());
    }

    private final IBinder mService = new IProfilePluginService.Stub() {

        @Override
        public void registerTrigger(String pkg, String opPkg, Trigger trigger, int userId)
                throws RemoteException {
            registerTriggerInternal(pkg, opPkg, Binder.getCallingUid(),
                    Binder.getCallingPid(), trigger, userId);
        }

        @Override
        public void registerAction(String pkg, String opPkg, Action action, int userId)
                throws RemoteException {
            registerActionInternal(pkg, opPkg, Binder.getCallingUid(),
                    Binder.getCallingPid(), action, userId);
        }

        @Override
        public List getRegisteredTriggers() throws RemoteException {
            return new ArrayList<ProfileServiceTrigger>(mTriggers.values());
        }

        @Override
        public List getRegisteredActions() throws RemoteException {
            return new ArrayList<ProfileServiceAction>(mActions.values());
        }

        @Override
        public void sendTrigger(String triggerId, String state) throws RemoteException {
            ProfileServiceTrigger profileServiceTrigger = mTriggers.get(triggerId);
            if (profileServiceTrigger != null) {
                ProfileManager profileManager = (ProfileManager)
                        getContext().getSystemService(Context.PROFILE_SERVICE);
                profileManager.sendTrigger(profileServiceTrigger.getTrigger().getTriggerId(),
                        state);
            }
        }

        @Override
        public void fireAction(String actionId, String state) throws RemoteException {
            ProfileServiceAction profileServiceAction = mActions.get(actionId);
            if (profileServiceAction != null) {
                try {
                    profileServiceAction.getAction().getAction().send();
                } catch (PendingIntent.CanceledException e) {
                    // Ignore cancelled
                }
            }
        }
    };

    private void registerTriggerInternal(String pkg, String opPkg, int callingUid, int callingPid,
            Trigger trigger, int incomingUserId) {
        final int userId = ActivityManager.handleIncomingUser(callingPid,
                callingUid, incomingUserId, true, false, "registerTrigger", pkg);
        final UserHandle user = new UserHandle(userId);
        final ProfileServiceTrigger profileServiceTrigger =
                new ProfileServiceTrigger(pkg, opPkg, userId, callingPid, trigger, user);
        mTriggers.put(profileServiceTrigger.getKey(), profileServiceTrigger);
    }

    private void registerActionInternal(String pkg, String opPkg, int callingUid, int callingPid,
            Action action, int incomingUserId) {
        final int userId = ActivityManager.handleIncomingUser(callingPid,
                callingUid, incomingUserId, true, false, "registerAction", pkg);
        final UserHandle user = new UserHandle(userId);
        final ProfileServiceAction profileServiceAction =
                new ProfileServiceAction(pkg, opPkg, userId, callingPid, action, user);
        mActions.put(profileServiceAction.getKey(), profileServiceAction);
    }
}
