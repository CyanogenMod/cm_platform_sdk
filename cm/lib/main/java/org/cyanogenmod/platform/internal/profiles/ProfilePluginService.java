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

package org.cyanogenmod.platform.internal.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.ProfileManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.profiles.Action;
import cyanogenmod.app.profiles.IProfilePluginService;
import cyanogenmod.app.profiles.Trigger;

import java.util.List;

public class ProfilePluginService extends SystemService {

    private Map<String, Trigger> mTriggers = Collections.synchronizedMap(new HashMap<String, Trigger>());
    private Map<String, Action> mActions = Collections.synchronizedMap(new HashMap<String, Action>());
    private PackageManager mPackageManager;
    private ProfileManager mProfileManager;

    public ProfilePluginService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PROFILE_PLUGIN_SERVICE, mService);
        mPackageManager = getContext().getPackageManager();
        mProfileManager = (ProfileManager) getContext().getSystemService(Context.PROFILE_SERVICE);
    }

    private String constructMapKey(String id) {
        return getPackageNameForCaller() + id;
    }

    private String getPackageNameForCaller() {
        return mPackageManager.getNameForUid(Binder.getCallingUid());
    }

    private final IBinder mService = new IProfilePluginService.Stub() {
        public void registerTrigger(Trigger trigger) {
            trigger.setPackage(getPackageNameForCaller());
            String triggerKey = constructMapKey(trigger.getTriggerId());
            mTriggers.put(triggerKey, trigger);
        }

        public void registerAction(Action action) {
            action.setPackage(getPackageNameForCaller());
            String actionKey = constructMapKey(action.getKey());
            mActions.put(actionKey, action);
        }

        @Override
        public List getRegisteredTriggers() throws RemoteException {
            return new ArrayList<Trigger>(mTriggers.values());
        }

        @Override
        public List getRegisteredActions() throws RemoteException {
            return new ArrayList<Action>(mActions.values());
        }

        @Override
        public void sendTrigger(String triggerId, int triggerState) {
            String triggerKey = constructMapKey(triggerId);
            Trigger trigger = mTriggers.get(triggerKey);
            if (trigger != null) {
                mProfileManager.sendTrigger(triggerId, triggerState);
            }
        }
    };
}
