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
import android.util.Log;
import android.util.Slog;

import cyanogenmod.app.ICMStatusBarManager;

/**
 * A class that wraps the internal StatusBarManagerService to expose functionality
 * for new API's specific to the CyanogenMod platform.
 */
public class CMStatusBarManager {
    private static final String TAG = "CMStatusBarManager";
    private static boolean localLOGV = false;

    private Context mContext;

    private static ICMStatusBarManager sService;
    private boolean mConnected;

    private static CMStatusBarManager sCMStatusBarManagerInstance;
    private CMStatusBarManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.CMStatusBarManager}
     * @param context
     * @return {@link cyanogenmod.app.CMStatusBarManager}
     */
    public static CMStatusBarManager getInstance(Context context) {
        if (sCMStatusBarManagerInstance == null) {
            sCMStatusBarManagerInstance = new CMStatusBarManager(context);
        }
        return sCMStatusBarManagerInstance;
    }

    /**
     * Post a custom tile to be shown in the status bar panel. If a custom tile with
     * the same id has already been posted by your application and has not yet been removed, it
     * will be replaced by the updated information.
     *
     * @param id An identifier for this customTile unique within your
     *        application.
     * @param customTile A {@link CustomTile} object describing what to show the user.
     *                   Must not be null.
     */
    public void publishTile(int id, CustomTile customTile) {
        publishTile(null, id, customTile);
    }

    /**
     * Post a custom tile to be shown in the status bar panel. If a custom tile with
     * the same tag and id has already been posted by your application and has not yet been
     * removed, it will be replaced by the updated information.
     *
     * @param tag A string identifier for this custom tile.  May be {@code null}.
     * @param id An identifier for this custom tile.  The pair (tag, id) must be unique
     *        within your application.
     * @param customTile A {@link cyanogenmod.app.CustomTile} object describing what to
     *        show the user. Must not be null.
     */
    public void publishTile(String tag, int id, CustomTile customTile) {
        if (!mConnected || sService == null) {
            Log.w(TAG, "not connected to CMStatusBarManagerService");
            return;
        }

        int[] idOut = new int[1];
        String pkg = mContext.getPackageName();
        if (localLOGV) Log.v(TAG, pkg + ": create(" + id + ", " + customTile + ")");
        try {
            sService.createCustomTileWithTag(pkg, mContext.getOpPackageName(), tag, id,
                    customTile, idOut, UserHandle.myUserId());
            if (id != idOut[0]) {
                Log.w(TAG, "notify: id corrupted: sent " + id + ", got back " + idOut[0]);
            }
        } catch (RemoteException e) {
            Slog.w("CMStatusBarManager", "warning: no cm status bar service");
        }
    }

    /**
     * Similar to {@link cyanogenmod.app.CMStatusBarManager#publishTile(int id, cyanogenmod.app.CustomTile)},
     * however lets you specify a {@link android.os.UserHandle}
     * @param tag A string identifier for this custom tile.  May be {@code null}.
     * @param id An identifier for this custom tile.  The pair (tag, id) must be unique
     *        within your application.
     * @param customTile A {@link cyanogenmod.app.CustomTile} object describing what to
     *        show the user. Must not be null.
     * @param user A user handle to publish the tile as.
     */
    public void publishTileAsUser(String tag, int id, CustomTile customTile, UserHandle user) {
        if (!mConnected || sService == null) {
            Log.w(TAG, "not connected to CMStatusBarManagerService");
            return;
        }

        int[] idOut = new int[1];
        String pkg = mContext.getPackageName();
        if (localLOGV) Log.v(TAG, pkg + ": create(" + id + ", " + customTile + ")");
        try {
            sService.createCustomTileWithTag(pkg, mContext.getOpPackageName(), tag, id,
                    customTile, idOut, user.getIdentifier());
            if (id != idOut[0]) {
                Log.w(TAG, "notify: id corrupted: sent " + id + ", got back " + idOut[0]);
            }
        } catch (RemoteException e) {
            Slog.w("CMStatusBarManager", "warning: no cm status bar service");
        }
    }

    /**
     * Remove a custom tile that's currently published to the StatusBarPanel.
     * @param id The identifier for the custom tile to be removed.
     */
    public void removeTile(int id) {
        removeTile(null, id);
    }

    /**
     * Remove a custom tile that's currently published to the StatusBarPanel.
     * @param tag The string identifier for the custom tile to be removed.
     * @param id The identifier for the custom tile to be removed.
     */
    public void removeTile(String tag, int id) {
        if (!mConnected || sService == null) {
            Log.w(TAG, "not connected to CMStatusBarManagerService");
            return;
        }

        String pkg = mContext.getPackageName();
        if (localLOGV) Log.v(TAG, pkg + ": remove(" + id + ")");
        try {
            sService.removeCustomTileWithTag(pkg, tag, id, UserHandle.myUserId());
        } catch (RemoteException e) {
            Slog.w("CMStatusBarManager", "warning: no cm status bar service");
        }
    }

    /**
     * Similar to {@link cyanogenmod.app.CMStatusBarManager#removeTile(String tag, int id)} however lets you
     * specific a {@link android.os.UserHandle}
     * @param tag The string identifier for the custom tile to be removed.
     * @param id The identifier for the custom tile to be removed.
     * @param user The user handle to remove the tile from.
     */
    public void removeTileAsUser(String tag, int id, UserHandle user) {
        if (!mConnected || sService == null) {
            Log.w(TAG, "not connected to CMStatusBarManagerService");
            return;
        }

        String pkg = mContext.getPackageName();
        if (localLOGV) Log.v(TAG, pkg + ": remove(" + id + ")");
        try {
            sService.removeCustomTileWithTag(pkg, tag, id, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    /** @hide */
    public ICMStatusBarManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_STATUS_BAR_SERVICE);
        sService = ICMStatusBarManager.Stub.asInterface(b);
        return sService;
    }
}
