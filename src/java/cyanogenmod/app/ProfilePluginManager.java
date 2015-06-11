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

package cyanogenmod.app;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import android.os.UserHandle;

import cyanogenmod.app.IProfilePluginService;

import java.util.List;

/**
 * The ProfilePluginManager allows you to register {@link Action}s and {@link Trigger}s
 * with the Profile plugin service.
 *
 * <p>
 *     TODO: Details!
 * <p>
 * To get the instance of this class, utilize ProfilePluginManager#getInstance(Context context)
 *
 * @see {@link Trigger} and {@link Action}
 */
public class ProfilePluginManager {
    private static IProfilePluginService sService;
    private static ProfilePluginManager sProfilePluginManagerInstance;
    private Context mContext;

    private ProfilePluginManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link ProfilePluginManager}
     * @param context
     * @return {@link ProfilePluginManager}
     */
    public static ProfilePluginManager getInstance(Context context) {
        if (sProfilePluginManagerInstance == null) {
            sProfilePluginManagerInstance = new ProfilePluginManager(context);
        }
        return sProfilePluginManagerInstance;
    }

    /**
     * Register a {@link Trigger} with the Profile plugin service.
     * @param trigger
     */
    public void registerTrigger(Trigger trigger) {
        IProfilePluginService service = getService();
        try {
            String pkg = mContext.getPackageName();
            service.registerTrigger(pkg, mContext.getOpPackageName(), trigger,
                    UserHandle.myUserId());
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /**
     * Register an {@link Action} with the Profile plugin service.
     * @param action
     */
    public void registerAction(Action action) {
        IProfilePluginService service = getService();
        try {
            String pkg = mContext.getPackageName();
            service.registerAction(pkg, mContext.getOpPackageName(), action, UserHandle.myUserId());
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /** @hide */
    public List<ProfileServiceAction> getRegisteredActions() {
        IProfilePluginService service = getService();
        try {
            return service.getRegisteredActions();
        } catch (RemoteException e) {
            //Unable to get service, fail
            return null;
        }
    }

    /** @hide */
    public List<ProfileServiceTrigger> getRegisteredTriggers() {
        IProfilePluginService service = getService();
        try {
            return service.getRegisteredTriggers();
        } catch (RemoteException e) {
            //Unable to get service, fail
            return null;
        }
    }

    /** @hide **/
    public void sendTrigger(String triggerId, String state) {
        IProfilePluginService service = getService();
        try {
            service.sendTrigger(triggerId, state);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /** @hide */
    public void fireAction(String actionId, String actionState) {
        IProfilePluginService service = getService();
        try {
            service.fireAction(actionId, actionState);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /** @hide */
    public IProfilePluginService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_PROFILE_PLUGIN_SERVICE);
        sService = IProfilePluginService.Stub.asInterface(b);
        return sService;
    }
}
