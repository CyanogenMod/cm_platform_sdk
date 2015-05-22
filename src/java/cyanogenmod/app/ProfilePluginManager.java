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

import cyanogenmod.app.IProfilePluginService;

import java.util.List;

/**
 * The ProfilePluginManager allows you to publish and remove CustomTiles within the
 * Quick Settings Panel.
 *
 * <p>
 * Each of the publish methods takes an int id parameter and optionally a
 * {@link String} tag parameter, which may be {@code null}.  These parameters
 * are used to form a pair (tag, id), or ({@code null}, id) if tag is
 * unspecified.  This pair identifies this custom tile from your app to the
 * system, so that pair should be unique within your app.  If you call one
 * of the publish methods with a (tag, id) pair that is currently active and
 * a new set of custom tile parameters, it will be updated.  For example,
 * if you pass a new custom tile icon, the old icon in the panel will
 * be replaced with the new one.  This is also the same tag and id you pass
 * to the {@link #removeTile(int)} or {@link #removeTile(String, int)} method to clear
 * this custom tile.
 *
 * <p>
 * To get the instance of this class, utilize CMStatusBarManager#getInstance(Context context)
 *
 * @see cyanogenmod.app.CustomTile
 */
public class ProfilePluginManager {
    private static IProfilePluginService sService;
    private static ProfilePluginManager sProfilePluginManagerInstance;
    private Context mContext;

    public ProfilePluginManager(Context context) {
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

    public void registerTrigger(Trigger trigger) {
        IProfilePluginService service = getService();
        try {
            service.registerTrigger(trigger);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    public void registerAction(Action action) {
        IProfilePluginService service = getService();
        try {
            service.registerAction(action);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /** hide */
    public List<Action> getRegisteredActions() {
        IProfilePluginService service = getService();
        try {
            return service.getRegisteredActions();
        } catch (RemoteException e) {
            //Unable to get service, fail
            return null;
        }
    }

    /** hide */
    public List<Trigger> getRegisteredTriggers() {
        IProfilePluginService service = getService();
        try {
            return service.getRegisteredTriggers();
        } catch (RemoteException e) {
            //Unable to get service, fail
            return null;
        }
    }

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
