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

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;

import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CustomTile;
import cyanogenmod.app.CustomTileListenerService;
import cyanogenmod.app.StatusBarPanelCustomTile;
import cyanogenmod.app.ICustomTileListener;
import cyanogenmod.app.ICMStatusBarManager;

import org.cyanogenmod.internal.statusbar.ExternalQuickSettingsRecord;
import org.cyanogenmod.internal.statusbar.IStatusBarCustomTileHolder;

import java.util.ArrayList;

import org.cyanogenmod.platform.internal.R;

/**
 * Internal service which manages interactions with system ui elements
 * @hide
 */
public class CMStatusBarManagerService extends CMSystemService {
    private static final String TAG = "CMStatusBarManagerService";

    private Context mContext;
    private Handler mHandler = new Handler();
    private CustomTileListeners mCustomTileListeners;

    static final int MAX_PACKAGE_TILES = 4;

    private static final int REASON_PACKAGE_CHANGED = 1;

    private final ManagedServices.UserProfiles mUserProfiles = new ManagedServices.UserProfiles();

    final ArrayList<ExternalQuickSettingsRecord> mQSTileList =
            new ArrayList<ExternalQuickSettingsRecord>();
    final ArrayMap<String, ExternalQuickSettingsRecord> mCustomTileByKey =
            new ArrayMap<String, ExternalQuickSettingsRecord>();

    public CMStatusBarManagerService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.STATUSBAR;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "registerCMStatusBar cmstatusbar: " + this);
        mCustomTileListeners = new CustomTileListeners();
        publishBinderService(CMContextConstants.CM_STATUS_BAR_SERVICE, mService);

        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        pkgFilter.addAction(Intent.ACTION_QUERY_PACKAGE_RESTART);
        pkgFilter.addDataScheme("package");
        getContext().registerReceiverAsUser(mPackageIntentReceiver, UserHandle.ALL, pkgFilter, null,
                null);

        IntentFilter sdFilter = new IntentFilter(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        getContext().registerReceiverAsUser(mPackageIntentReceiver, UserHandle.ALL, sdFilter, null,
                null);
    }

    private final BroadcastReceiver mPackageIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            boolean queryRestart = false;
            boolean queryRemove = false;
            boolean packageChanged = false;
            boolean removeTiles = true;

            if (action.equals(Intent.ACTION_PACKAGE_ADDED)
                    || (queryRemove=action.equals(Intent.ACTION_PACKAGE_REMOVED))
                    || action.equals(Intent.ACTION_PACKAGE_RESTARTED)
                    || (packageChanged=action.equals(Intent.ACTION_PACKAGE_CHANGED))
                    || (queryRestart=action.equals(Intent.ACTION_QUERY_PACKAGE_RESTART))
                    || action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)) {
                int changeUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_ALL);
                String pkgList[] = null;
                boolean queryReplace = queryRemove &&
                        intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                if (action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)) {
                    pkgList = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                } else if (queryRestart) {
                    pkgList = intent.getStringArrayExtra(Intent.EXTRA_PACKAGES);
                } else {
                    Uri uri = intent.getData();
                    if (uri == null) {
                        return;
                    }
                    String pkgName = uri.getSchemeSpecificPart();
                    if (pkgName == null) {
                        return;
                    }
                    if (packageChanged) {
                        // We remove tiles for packages which have just been disabled
                        try {
                            final IPackageManager pm = AppGlobals.getPackageManager();
                            final int enabled = pm.getApplicationEnabledSetting(pkgName,
                                    changeUserId != UserHandle.USER_ALL ? changeUserId :
                                            UserHandle.USER_OWNER);
                            if (enabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    || enabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                                removeTiles = false;
                            }
                        } catch (IllegalArgumentException e) {
                            // Package doesn't exist; probably racing with uninstall.
                            // removeTiles is already true, so nothing to do here.
                            Slog.i(TAG, "Exception trying to look up app enabled setting", e);
                        } catch (RemoteException e) {
                            // Failed to talk to PackageManagerService Should never happen!
                        }
                    }
                    pkgList = new String[]{pkgName};
                }

                if (pkgList != null && (pkgList.length > 0)) {
                    for (String pkgName : pkgList) {
                        if (removeTiles) {
                            removeAllCustomTilesInt(pkgName, !queryRestart,
                                    changeUserId, REASON_PACKAGE_CHANGED, null);
                        }
                    }
                }
                mCustomTileListeners.onPackagesChanged(queryReplace, pkgList);
            }
        }
    };

    private final IBinder mService = new ICMStatusBarManager.Stub() {
        /**
         * @hide
         */
        @Override
        public void createCustomTileWithTag(String pkg, String opPkg, String tag, int id,
               CustomTile customTile, int[] idOut, int userId) throws RemoteException {
            enforceCustomTilePublish();
            createCustomTileWithTagInternal(pkg, opPkg, Binder.getCallingUid(),
                    Binder.getCallingPid(), tag, id, customTile, idOut, userId);
        }

        /**
         * @hide
         */
        @Override
        public void removeCustomTileWithTag(String pkg, String tag, int id, int userId) {
            checkCallerIsSystemOrSameApp(pkg);
            userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(),
                    Binder.getCallingUid(), userId, true, false, "cancelCustomTileWithTag", pkg);
            removeCustomTileWithTagInternal(Binder.getCallingUid(),
                    Binder.getCallingPid(), pkg, tag, id, userId, null);
        }

        /**
         * Register a listener binder directly with the status bar manager.
         *
         * Only works with system callers. Apps should extend
         * {@link cyanogenmod.app.CustomTileListenerService}.
         * @hide
         */
        @Override
        public void registerListener(final ICustomTileListener listener,
                                     final ComponentName component, final int userid) {
            enforceBindCustomTileListener();
            mCustomTileListeners.registerService(listener, component, userid);

            // Notify registered tiles
            try {
                for (ExternalQuickSettingsRecord qsTile : mQSTileList) {
                    if (userid == UserHandle.USER_ALL || qsTile.getUserId() == userid) {
                        listener.onCustomTilePosted(new StatusBarCustomTileHolder(qsTile.sbTile));
                    }
                }
            } catch (RemoteException re) {
                // Ignore
            }
        }

        /**
         * Remove a listener binder directly
         * @hide
         */
        @Override
        public void unregisterListener(ICustomTileListener listener, int userid) {
            enforceBindCustomTileListener();
            mCustomTileListeners.unregisterService(listener, userid);
        }

        /**
         * Allow an ICustomTileListener to simulate clearing (dismissing) a single customTile.
         *
         * @param token The binder for the listener, to check that the caller is allowed
         */
        @Override
        public void removeCustomTileFromListener(ICustomTileListener token, String pkg,
               String tag, int id) {
            final int callingUid = Binder.getCallingUid();
            final int callingPid = Binder.getCallingPid();
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (mQSTileList) {
                    final ManagedServices.ManagedServiceInfo info
                            = mCustomTileListeners.checkServiceTokenLocked(token);
                    removeCustomTileFromListenerLocked(info, callingUid, callingPid,
                            pkg, tag, id, info.userid);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    };

    void createCustomTileWithTagInternal(final String pkg, final String opPkg, final int callingUid,
            final int callingPid, final String tag, final int id, final CustomTile customTile,
            final int[] idOut, final int incomingUserId) {

        if (pkg == null || customTile == null) {
            throw new IllegalArgumentException("null not allowed: pkg=" + pkg
                    + " id=" + id + " customTile=" + customTile);
        }

        final int userId = ActivityManager.handleIncomingUser(callingPid,
                callingUid, incomingUserId, true, false, "createCustomTileWithTag", pkg);
        final UserHandle user = new UserHandle(userId);

        // remove custom tile call ends up in not removing the custom tile.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Obtain the resources package name
                String resPkg = customTile.getResourcesPackageName();
                if (TextUtils.isEmpty(resPkg)) {
                    resPkg = pkg;
                }

                final StatusBarPanelCustomTile sbc = new StatusBarPanelCustomTile(
                        pkg, resPkg, opPkg, id, tag, callingUid, callingPid, customTile, user);
                ExternalQuickSettingsRecord r = new ExternalQuickSettingsRecord(sbc);
                ExternalQuickSettingsRecord old = mCustomTileByKey.get(sbc.getKey());

                int index = indexOfQsTileLocked(sbc.getKey());
                if (index < 0) {
                    // If this tile unknown to us, check DOS protection
                    if (checkDosProtection(pkg, callingUid, userId)) return;
                    mQSTileList.add(r);
                } else {
                    old = mQSTileList.get(index);
                    mQSTileList.set(index, r);
                    r.isUpdate = true;
                }

                mCustomTileByKey.put(sbc.getKey(), r);

                if (customTile.icon != 0 || customTile.remoteIcon != null) {
                    StatusBarPanelCustomTile oldSbn = (old != null) ? old.sbTile : null;
                    mCustomTileListeners.notifyPostedLocked(sbc, oldSbn);
                } else {
                    Slog.e(TAG, "Not posting custom tile with no icon set: " + customTile);
                    if (old != null && !old.isCanceled) {
                        mCustomTileListeners.notifyRemovedLocked(sbc);
                    }
                }
            }
        });
        idOut[0] = id;
    }

    private boolean checkDosProtection(String pkg, int callingUid, int userId) {
        final boolean isSystemTile = isUidSystem(callingUid) || ("android".equals(pkg));
        // Limit the number of Custom tiles that any given package except the android
        // package or a registered listener can enqueue.  Prevents DOS attacks and deals with leaks.
        if (!isSystemTile) {
            synchronized (mQSTileList) {
                int count = 0;
                final int N = mQSTileList.size();

                for (int i = 0; i < N; i++) {
                    final ExternalQuickSettingsRecord r = mQSTileList.get(i);
                    if (r.sbTile.getPackage().equals(pkg) && r.sbTile.getUserId() == userId) {
                        count++;
                        if (count >= MAX_PACKAGE_TILES) {
                            Slog.e(TAG, "Package has already posted " + count
                                    + " custom tiles.  Not showing more.  package=" + pkg);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // lock on mQSTileList
    int indexOfQsTileLocked(String key) {
        final int N = mQSTileList.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(mQSTileList.get(i).getKey())) {
                return i;
            }
        }
        return -1;
    }

    // lock on mQSTileList
    int indexOfQsTileLocked(String pkg, String tag, int id, int userId) {
        ArrayList<ExternalQuickSettingsRecord> list = mQSTileList;
        final int len = list.size();
        for (int i = 0; i < len; i++) {
            ExternalQuickSettingsRecord r = list.get(i);
            if (!customTileMatchesUserId(r, userId) || r.sbTile.getId() != id) {
                continue;
            }
            if (tag == null) {
                if (r.sbTile.getTag() != null) {
                    continue;
                }
            } else {
                if (!tag.equals(r.sbTile.getTag())) {
                    continue;
                }
            }
            if (r.sbTile.getPackage().equals(pkg)) {
                return i;
            }
        }
        return -1;
    }

    private static void checkCallerIsSystemOrSameApp(String pkg) {
        if (isCallerSystem()) {
            return;
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

    private static boolean isUidSystem(int uid) {
        final int appid = UserHandle.getAppId(uid);
        return (appid == android.os.Process.SYSTEM_UID
                || appid == android.os.Process.PHONE_UID || uid == 0);
    }

    private static boolean isCallerSystem() {
        return isUidSystem(Binder.getCallingUid());
    }

    /**
     * Determine whether the userId applies to the custom tile in question, either because
     * they match exactly, or one of them is USER_ALL (which is treated as a wildcard).
     */
    private boolean customTileMatchesUserId(ExternalQuickSettingsRecord r, int userId) {
        return
                // looking for USER_ALL custom tile? match everything
                userId == UserHandle.USER_ALL
                        // a custom tile sent to USER_ALL matches any query
                        || r.getUserId() == UserHandle.USER_ALL
                        // an exact user match
                        || r.getUserId() == userId;
    }

    private void removeCustomTileFromListenerLocked(ManagedServices.ManagedServiceInfo info,
            int callingUid, int callingPid, String pkg, String tag, int id, int userId) {
        removeCustomTileWithTagInternal(callingUid, callingPid, pkg, tag, id, userId, info);
    }

    void removeCustomTileWithTagInternal(final int callingUid, final int callingPid,
            final String pkg, final String tag, final int id, final int userId,
            final ManagedServices.ManagedServiceInfo listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mQSTileList) {
                    int index = indexOfQsTileLocked(pkg, tag, id, userId);
                    if (index >= 0) {
                        ExternalQuickSettingsRecord r = mQSTileList.get(index);
                        mQSTileList.remove(index);
                        // status bar
                        r.isCanceled = true;
                        mCustomTileListeners.notifyRemovedLocked(r.sbTile);
                        mCustomTileByKey.remove(r.sbTile.getKey());
                        if (r.getCustomTile().deleteIntent != null) {
                            try {
                                r.getCustomTile().deleteIntent.send();
                            } catch (PendingIntent.CanceledException ex) {
                                // do nothing - there's no relevant way to recover, and
                                //     no reason to let this propagate
                                Slog.w(TAG, "canceled PendingIntent for "
                                        + r.sbTile.getPackage(), ex);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Removes all custom tiles from a given package that have all of the
     * {@code mustHaveFlags}.
     */
    boolean removeAllCustomTilesInt(String pkg, boolean doit, int userId, int reason,
            ManagedServices.ManagedServiceInfo listener) {
        synchronized (mQSTileList) {
            final int N = mQSTileList.size();
            ArrayList<ExternalQuickSettingsRecord> removedTiles = null;
            for (int i = N-1; i >= 0; --i) {
                ExternalQuickSettingsRecord r = mQSTileList.get(i);
                if (!customTileMatchesUserId(r, userId)) {
                    continue;
                }
                // Don't remove custom tiles to all, if there's no package name specified
                if (r.getUserId() == UserHandle.USER_ALL && pkg == null) {
                    continue;
                }
                if (pkg != null && !r.sbTile.getPackage().equals(pkg)) {
                    continue;
                }
                if (removedTiles == null) {
                    removedTiles = new ArrayList<>();
                }
                removedTiles.add(r);
                if (!doit) {
                    return true;
                }
                mQSTileList.remove(i);
                removeCustomTileLocked(r, false, reason);
            }
            return removedTiles != null;
        }
    }

    private void removeCustomTileLocked(ExternalQuickSettingsRecord r,
            boolean sendDelete, int reason) {
        // tell the app
        if (sendDelete) {
            if (r.getCustomTile().deleteIntent != null) {
                try {
                    r.getCustomTile().deleteIntent.send();
                } catch (PendingIntent.CanceledException ex) {
                    // do nothing - there's no relevant way to recover, and
                    //     no reason to let this propagate
                    Slog.w(TAG, "canceled PendingIntent for " + r.sbTile.getPackage(), ex);
                }
            }
        }

        // status bar
        if (r.getCustomTile().icon != 0 || r.getCustomTile().remoteIcon != null) {
            r.isCanceled = true;
            mCustomTileListeners.notifyRemovedLocked(r.sbTile);
        }

        mCustomTileByKey.remove(r.sbTile.getKey());
    }

    private void enforceSystemOrSystemUI(String message) {
        if (isCallerSystem()) return;
        mContext.enforceCallingPermission(android.Manifest.permission.STATUS_BAR_SERVICE,
                message);
    }

    private void enforceCustomTilePublish() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.PUBLISH_CUSTOM_TILE,
                "StatusBarManagerService");
    }

    private void enforceBindCustomTileListener() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.BIND_CUSTOM_TILE_LISTENER_SERVICE,
                "StatusBarManagerService");
    }

    private boolean isVisibleToListener(StatusBarPanelCustomTile sbc,
                                        ManagedServices.ManagedServiceInfo listener) {
        return listener.enabledAndUserMatches(sbc.getUserId());
    }

    public class CustomTileListeners extends ManagedServices {

        public CustomTileListeners() {
            super(CMStatusBarManagerService.this.mContext, mHandler, mQSTileList, mUserProfiles);
        }

        @Override
        protected Config getConfig() {
            Config c = new Config();
            c.caption = "custom tile listener";
            c.serviceInterface = CustomTileListenerService.SERVICE_INTERFACE;
            //TODO: Implement this in the future
            //c.secureSettingName = Settings.Secure.ENABLED_CUSTOM_TILE_LISTENERS;
            c.bindPermission =
                    cyanogenmod.platform.Manifest.permission.BIND_CUSTOM_TILE_LISTENER_SERVICE;
            //TODO: Implement this in the future
            //c.settingsAction = Settings.ACTION_CUSTOM_TILE_LISTENER_SETTINGS;
            c.clientLabel = R.string.custom_tile_listener_binding_label;
            return c;
        }

        @Override
        protected IInterface asInterface(IBinder binder) {
            return ICustomTileListener.Stub.asInterface(binder);
        }

        @Override
        public void onServiceAdded(ManagedServiceInfo info) {
            final ICustomTileListener listener = (ICustomTileListener) info.service;
            try {
                listener.onListenerConnected();
            } catch (RemoteException e) {
                // we tried
            }
        }

        @Override
        protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
        }


        /**
         * asynchronously notify all listeners about a new custom tile
         *
         * <p>
         * Also takes care of removing a custom tile that has been visible to a listener before,
         * but isn't anymore.
         */
        public void notifyPostedLocked(StatusBarPanelCustomTile sbc,
               StatusBarPanelCustomTile oldSbc) {
            // Lazily initialized snapshots of the custom tile.
            StatusBarPanelCustomTile sbcClone = null;

            for (final ManagedServiceInfo info : mServices) {
                boolean sbnVisible = isVisibleToListener(sbc, info);
                boolean oldSbnVisible = oldSbc != null ? isVisibleToListener(oldSbc, info) : false;
                // This custom tile hasn't been and still isn't visible -> ignore.
                if (!oldSbnVisible && !sbnVisible) {
                    continue;
                }

                // This custom tile became invisible -> remove the old one.
                if (oldSbnVisible && !sbnVisible) {
                    final StatusBarPanelCustomTile oldSbcClone = oldSbc.clone();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyRemoved(info, oldSbcClone);
                        }
                    });
                    continue;
                }
                sbcClone = sbc.clone();

                final StatusBarPanelCustomTile sbcToPost = sbcClone;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyPosted(info, sbcToPost);
                    }
                });
            }
        }

        /**
         * asynchronously notify all listeners about a removed custom tile
         */
        public void notifyRemovedLocked(StatusBarPanelCustomTile sbc) {
            // make a copy in case changes are made to the underlying CustomTile object
            final StatusBarPanelCustomTile sbcClone = sbc.clone();
            for (final ManagedServiceInfo info : mServices) {
                if (!isVisibleToListener(sbcClone, info)) {
                    continue;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyRemoved(info, sbcClone);
                    }
                });
            }
        }

        private void notifyPosted(final ManagedServiceInfo info,
                                  final StatusBarPanelCustomTile sbc) {
            final ICustomTileListener listener = (ICustomTileListener)info.service;
            StatusBarCustomTileHolder sbcHolder = new StatusBarCustomTileHolder(sbc);
            try {
                listener.onCustomTilePosted(sbcHolder);
            } catch (RemoteException ex) {
                Log.e(TAG, "unable to notify listener (posted): " + listener, ex);
            }
        }

        private void notifyRemoved(ManagedServiceInfo info, StatusBarPanelCustomTile sbc) {
            if (!info.enabledAndUserMatches(sbc.getUserId())) {
                return;
            }
            final ICustomTileListener listener = (ICustomTileListener) info.service;
            StatusBarCustomTileHolder sbcHolder = new StatusBarCustomTileHolder(sbc);
            try {
                listener.onCustomTileRemoved(sbcHolder);
            } catch (RemoteException ex) {
                Log.e(TAG, "unable to notify listener (removed): " + listener, ex);
            }
        }
    }

    /**
     * Wrapper for a StatusBarPanelCustomTile object that allows transfer across a oneway
     * binder without sending large amounts of data over a oneway transaction.
     */
    private static final class StatusBarCustomTileHolder
            extends IStatusBarCustomTileHolder.Stub {
        private StatusBarPanelCustomTile mValue;

        public StatusBarCustomTileHolder(StatusBarPanelCustomTile value) {
            mValue = value;
        }

        /** Get the held value and clear it. This function should only be called once per holder */
        @Override
        public StatusBarPanelCustomTile get() {
            StatusBarPanelCustomTile value = mValue;
            mValue = null;
            return value;
        }
    }
}
