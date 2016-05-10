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
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ILiveLockScreenChangeListener;
import cyanogenmod.app.ILiveLockScreenManager;
import cyanogenmod.app.ILiveLockScreenManagerProvider;
import cyanogenmod.app.LiveLockScreenInfo;
import cyanogenmod.app.LiveLockScreenManager;
import cyanogenmod.platform.Manifest;
import cyanogenmod.providers.CMSettings;

import java.util.List;

/**
 * Live lock screen service broker for connecting clients to a backing Live lock screen manager
 * service.
 *
 * @hide
 */
public class LiveLockScreenServiceBroker extends SystemService {
    private static final String TAG = LiveLockScreenServiceBroker.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int MSG_TRY_CONNECTING = 1;

    private static final long SERVICE_CONNECTION_WAIT_TIME_MS = 4 * 1000L; // 4 seconds

    private static final String DEPRECATED_THIRD_PARTY_KEYGUARD_PERMISSION =
            "android.permission.THIRD_PARTY_KEYGUARD";

    private Context mContext;
    // The actual LLS service to invoke
    private ILiveLockScreenManagerProvider mService;

    // Cached change listeners
    private final RemoteCallbackList<ILiveLockScreenChangeListener> mChangeListeners =
            new RemoteCallbackList<>();

    private LiveLockScreenInfo mDefaultLlsInfo;

    private final Handler mConnectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TRY_CONNECTING:
                    tryConnecting();
                    break;
                default:
                    Slog.e(TAG, "Unknown message");
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.i(TAG, "LiveLockScreenManagerService connected");
            synchronized (LiveLockScreenServiceBroker.this) {
                mService = ILiveLockScreenManagerProvider.Stub.asInterface(service);
                LiveLockScreenServiceBroker.this.notifyAll();
                // If any change listeners are cached, register them with the newly connected
                // service.
                try {
                    int N = mChangeListeners.beginBroadcast();
                    if (mService != null && N > 0) {
                        for (int i = 0; i < N; i++) {
                            mService.registerChangeListener(mChangeListeners.getBroadcastItem(i));
                        }
                    }
                } catch (RemoteException e) {
                    /* ignore */
                } finally {
                    mChangeListeners.finishBroadcast();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Slog.i(TAG, "LiveLockScreenManagerService unexpectedly disconnected");
            synchronized (LiveLockScreenServiceBroker.this) {
                mService = null;
                LiveLockScreenServiceBroker.this.notifyAll();
            }
        }
    };

    /**
     * ILiveLockScreenManager implementation to use when no backing service can be found.
     */
    private final ILiveLockScreenManagerProvider mServiceStubForFailure =
            new ILiveLockScreenManagerProvider() {
        @Override
        public void enqueueLiveLockScreen(String pkg, int id, LiveLockScreenInfo lls,
                int[] idReceived, int userid) throws RemoteException {
        }

        @Override
        public void cancelLiveLockScreen(String pkg, int id, int userId) throws RemoteException {
        }

        @Override
        public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
            return null;
        }

        @Override
        public void updateDefaultLiveLockScreen(LiveLockScreenInfo llsInfo) throws RemoteException {

        }

        @Override
        public boolean getLiveLockScreenEnabled() throws RemoteException {
            return false;
        }

        @Override
        public boolean registerChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            return false;
        }

        @Override
        public boolean unregisterChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            return false;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };

    private final class BinderService extends ILiveLockScreenManager.Stub {

        @Override
        public void enqueueLiveLockScreen(String pkg, int id,
                LiveLockScreenInfo lls, int[] idReceived, int userId) throws RemoteException {
            getServiceGuarded().enqueueLiveLockScreen(pkg, id, lls, idReceived, userId);
        }

        @Override
        public void cancelLiveLockScreen(String pkg, int id, int userId) throws RemoteException {
            getServiceGuarded().cancelLiveLockScreen(pkg, id, userId);
        }

        @Override
        public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
            return getServiceGuarded().getCurrentLiveLockScreen();
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
            setLiveLockScreenEnabledInternal(enabled);
        }

        @Override
        public boolean getLiveLockScreenEnabled() throws RemoteException {
            return getServiceGuarded().getLiveLockScreenEnabled();
        }

        @Override
        public boolean registerChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            boolean registered = getServiceGuarded().registerChangeListener(listener);
            if (registered) {
                mChangeListeners.register(listener);
            }
            return getServiceGuarded().registerChangeListener(listener);
        }

        @Override
        public boolean unregisterChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            boolean unregistered = getServiceGuarded().unregisterChangeListener(listener);
            if (unregistered) {
                mChangeListeners.unregister(listener);
            }
            return unregistered;
        }
    }

    public LiveLockScreenServiceBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
        if (mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.LIVE_LOCK_SCREEN)) {
            publishBinderService(CMContextConstants.CM_LIVE_LOCK_SCREEN_SERVICE,
                    new BinderService());
        } else {
            Slog.wtf(TAG, "CM live lock screen service started by system server but feature xml " +
                    "not declared. Not publishing binder service!");
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_THIRD_PARTY_APPS_CAN_START) {
            if (DEBUG) Slog.d(TAG, "Third party apps ready");

            // Initialize the default LLS component
            String defComponent = CMSettings.Secure.getString(mContext.getContentResolver(),
                    CMSettings.Secure.DEFAULT_LIVE_LOCK_SCREEN_COMPONENT);
            if (!TextUtils.isEmpty(defComponent)) {
                mDefaultLlsInfo = new LiveLockScreenInfo.Builder()
                        .setComponent(ComponentName.unflattenFromString(defComponent))
                        .build();
            }
            // Now that 3rd party apps are ready, try connecting to the backing service
            tryConnecting();
        }
    }

    /**
     * Binds to the backing service if one is found
     */
    private void tryConnecting() {
        Slog.i(TAG, "Connecting to LiveLockScreenManagerService");
        synchronized (this) {
            if (mService != null) {
                Slog.d(TAG, "Already connected");
                return;
            }
            final Intent intent = new Intent();
            final ComponentName cn = getLiveLockScreenServiceComponent();
            if (cn == null) {
                Slog.e(TAG, "No live lock screen manager service found");
                return;
            }
            intent.setComponent(getLiveLockScreenServiceComponent());
            try {
                if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                    Slog.e(TAG, "Failed to bind to LiveLockScreenManagerService");
                }
            } catch (SecurityException e) {
                Slog.e(TAG, "Forbidden to bind to LiveLockScreenManagerService", e);
            }
        }
    }

    /**
     * Queries package manager for the component which handles the
     * {@link LiveLockScreenManager#SERVICE_INTERFACE} and has been granted the
     * {@link org.cyanogenmod.platform.internal.Manifest.permission#LIVE_LOCK_SCREEN_MANAGER_PROVIDER}
     * permission.
     *
     * @return A valid component that supports {@link LiveLockScreenManager#SERVICE_INTERFACE} or
     *         null if no component can be found.
     */
    @Nullable private ComponentName getLiveLockScreenServiceComponent() {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(LiveLockScreenManager.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent, 0);
        for (ResolveInfo info : resolveInfos) {
            if (info != null) {
                if (pm.checkPermission(Manifest.permission.LIVE_LOCK_SCREEN_MANAGER_PROVIDER,
                        info.serviceInfo.packageName) == PackageManager.PERMISSION_GRANTED &&
                        info.serviceInfo.isEnabled()) {
                    return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
                }
            }
        }

        return null;
    }

    private ILiveLockScreenManagerProvider getOrConnectService() {
        synchronized (this) {
            if (mService != null) {
                return mService;
            }
            // Service is not connected. Try blocking connecting.
            Slog.w(TAG, "LiveLockScreenManagerService not connected. Try connecting...");
            mConnectionHandler.sendMessage(
                    mConnectionHandler.obtainMessage(MSG_TRY_CONNECTING));
            final long shouldEnd =
                    SystemClock.elapsedRealtime() + SERVICE_CONNECTION_WAIT_TIME_MS;
            long waitTime = SERVICE_CONNECTION_WAIT_TIME_MS;
            while (waitTime > 0) {
                try {
                    // TODO: consider using Java concurrent construct instead of raw object wait
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    Slog.w(TAG, "Connection wait interrupted", e);
                }
                if (mService != null) {
                    // Success
                    return mService;
                }
                // Calculate remaining waiting time to make sure we wait the full timeout period
                waitTime = shouldEnd - SystemClock.elapsedRealtime();
            }
            // Timed out. Something's really wrong.
            Slog.e(TAG, "Can not connect to LiveLockScreenManagerService (timed out)");
            return null;
        }
    }

    /**
     * Make sure to return a non-empty service instance. Return the connected LiveLockScreenManager
     * instance, if not connected, try connecting. If fail to connect, return a fake service
     * instance which returns failure to service caller.
     *
     * @return a non-empty service instance, real or fake
     */
    private ILiveLockScreenManagerProvider getServiceGuarded() {
        final ILiveLockScreenManagerProvider service = getOrConnectService();
        if (service != null) {
            return service;
        }
        return mServiceStubForFailure;
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

    private LiveLockScreenInfo getDefaultLiveLockScreenInternal() {
        return mDefaultLlsInfo;
    }

    private void setDefaultLiveLockScreenInternal(LiveLockScreenInfo llsInfo) {
        if (llsInfo != null && llsInfo.component != null) {
            // Check that the package this component belongs to has the third party keyguard perm
            final PackageManager pm = mContext.getPackageManager();
            final boolean hasThirdPartyKeyguardPermission = pm.checkPermission(
                    Manifest.permission.THIRD_PARTY_KEYGUARD,
                    llsInfo.component.getPackageName()) == PackageManager.PERMISSION_GRANTED
                    || pm.checkPermission(DEPRECATED_THIRD_PARTY_KEYGUARD_PERMISSION,
                    llsInfo.component.getPackageName()) == PackageManager.PERMISSION_GRANTED;
            if (!hasThirdPartyKeyguardPermission) {
                Slog.e(TAG, "Package " + llsInfo.component.getPackageName() +
                        " does not have " + Manifest.permission.THIRD_PARTY_KEYGUARD);
                return;
            }
        }

        long token = Binder.clearCallingIdentity();
        try {
            CMSettings.Secure.putString(mContext.getContentResolver(),
                    CMSettings.Secure.DEFAULT_LIVE_LOCK_SCREEN_COMPONENT,
                    (llsInfo != null && llsInfo.component != null)
                            ? llsInfo.component.flattenToString()
                            : "");
        } finally {
            Binder.restoreCallingIdentity(token);
        }

        mDefaultLlsInfo = llsInfo;
        try {
            getServiceGuarded().updateDefaultLiveLockScreen(llsInfo);
        } catch (RemoteException e) {
            /* ignore */
        }
    }

    private void setLiveLockScreenEnabledInternal(boolean enabled) {
        long token = Binder.clearCallingIdentity();
        CMSettings.Secure.putInt(mContext.getContentResolver(),
                CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED, enabled ? 1 : 0);
        Binder.restoreCallingIdentity(token);
    }
}
