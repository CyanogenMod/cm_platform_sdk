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
import android.os.IInterface;
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

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

import java.util.List;

/**
 * Live lock screen service broker for connecting clients to a backing Live lock screen manager
 * service.
 *
 * @hide
 */
public class LiveLockScreenServiceBroker extends
        BrokerableCMSystemService<ILiveLockScreenManagerProvider> {
    private static final String TAG = LiveLockScreenServiceBroker.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String DEPRECATED_THIRD_PARTY_KEYGUARD_PERMISSION =
            "android.permission.THIRD_PARTY_KEYGUARD";

    private Context mContext;

    // Cached change listeners
    private final RemoteCallbackList<ILiveLockScreenChangeListener> mChangeListeners =
            new RemoteCallbackList<>();

    private LiveLockScreenInfo mDefaultLlsInfo;

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
            getBrokeredService().enqueueLiveLockScreen(pkg, id, lls, idReceived, userId);
        }

        @Override
        public void cancelLiveLockScreen(String pkg, int id, int userId) throws RemoteException {
            getBrokeredService().cancelLiveLockScreen(pkg, id, userId);
        }

        @Override
        public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
            return getBrokeredService().getCurrentLiveLockScreen();
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
            return getBrokeredService().getLiveLockScreenEnabled();
        }

        @Override
        public boolean registerChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            boolean registered = getBrokeredService().registerChangeListener(listener);
            if (registered) {
                mChangeListeners.register(listener);
            }
            return registered;
        }

        @Override
        public boolean unregisterChangeListener(
                ILiveLockScreenChangeListener listener) throws RemoteException {
            boolean unregistered = getBrokeredService().unregisterChangeListener(listener);
            if (unregistered) {
                mChangeListeners.unregister(listener);
            }
            return unregistered;
        }
    }

    public LiveLockScreenServiceBroker(Context context) {
        super(context);
        mContext = context;
        setBrokeredServiceConnection(mServiceConnection);
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.LIVE_LOCK_SCREEN;
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
        publishBinderService(CMContextConstants.CM_LIVE_LOCK_SCREEN_SERVICE, new BinderService());
    }

    @Override
    protected ILiveLockScreenManagerProvider getIBinderAsIInterface(IBinder service) {
        return ILiveLockScreenManagerProvider.Stub.asInterface(service);
    }

    @Override
    public ILiveLockScreenManagerProvider getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    private BrokeredServiceConnection mServiceConnection = new BrokeredServiceConnection() {
        @Override
        public void onBrokeredServiceConnected() {
            // If any change listeners are cached, register them with the newly connected
            // service.
            try {
                int N = mChangeListeners.beginBroadcast();
                ILiveLockScreenManagerProvider iLiveLockScreenManagerProvider =
                        getBrokeredService();
                if (iLiveLockScreenManagerProvider != null && N > 0) {
                    for (int i = 0; i < N; i++) {
                        iLiveLockScreenManagerProvider
                                .registerChangeListener(mChangeListeners.getBroadcastItem(i));
                    }
                }
            } catch (RemoteException e) {
                    /* ignore */
            } finally {
                mChangeListeners.finishBroadcast();
            }
        }

        @Override
        public void onBrokeredServiceDisconnected() {

        }
    };

    @Override
    protected String getComponentFilteringPermission() {
        // Live lock screen service has its own vertical providing permission
        return Manifest.permission.LIVE_LOCK_SCREEN_MANAGER_PROVIDER;
    }

    @Override
    protected ComponentName getServiceComponent() {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(LiveLockScreenManager.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent, 0);
        for (ResolveInfo info : resolveInfos) {
            if (info != null) {
                if (info.serviceInfo.isEnabled()) {
                    return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
                }
            }
        }
        return null;
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
        }
        super.onBootPhase(phase);
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
            getBrokeredService().updateDefaultLiveLockScreen(llsInfo);
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
