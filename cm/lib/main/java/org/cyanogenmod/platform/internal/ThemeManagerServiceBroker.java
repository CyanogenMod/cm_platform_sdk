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

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.platform.Manifest;
import cyanogenmod.themes.IThemeChangeListener;
import cyanogenmod.themes.IThemeProcessingListener;
import cyanogenmod.themes.IThemeService;
import cyanogenmod.themes.ThemeChangeRequest;

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

/**
 * Theme service broker for connecting clients to a backing theme manager service.
 *
 * @hide
 */
public class ThemeManagerServiceBroker extends BrokerableCMSystemService<IThemeService> {
    private static final String TAG = ThemeManagerServiceBroker.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final ComponentName SERVICE_COMPONENT =
            new ComponentName("org.cyanogenmod.themeservice",
                    "org.cyanogenmod.themeservice.ThemeManagerService");

    // Cached change listeners
    private final RemoteCallbackList<IThemeChangeListener> mChangeListeners =
            new RemoteCallbackList<>();
    private final RemoteCallbackList<IThemeProcessingListener> mProcessingListeners =
            new RemoteCallbackList<>();

    private final IThemeService mServiceStubForFailure = new IThemeService() {
        @Override
        public void requestThemeChangeUpdates(IThemeChangeListener listener) throws RemoteException {
        }

        @Override
        public void removeUpdates(IThemeChangeListener listener) throws RemoteException {
        }

        @Override
        public void requestThemeChange(ThemeChangeRequest request,
                boolean removePerAppThemes) throws RemoteException {
        }

        @Override
        public void applyDefaultTheme() throws RemoteException {
        }

        @Override
        public boolean isThemeApplying() throws RemoteException {
            return false;
        }

        @Override
        public int getProgress() throws RemoteException {
            return 0;
        }

        @Override
        public boolean processThemeResources(String themePkgName) throws RemoteException {
            return false;
        }

        @Override
        public boolean isThemeBeingProcessed(String themePkgName) throws RemoteException {
            return false;
        }

        @Override
        public void registerThemeProcessingListener(
                IThemeProcessingListener listener) throws RemoteException {
        }

        @Override
        public void unregisterThemeProcessingListener(
                IThemeProcessingListener listener) throws RemoteException {
        }

        @Override
        public void rebuildResourceCache() throws RemoteException {
        }

        @Override
        public long getLastThemeChangeTime() throws RemoteException {
            return 0;
        }

        @Override
        public int getLastThemeChangeRequestType() throws RemoteException {
            return 0;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };

    private final class BinderService extends IThemeService.Stub {

        @Override
        public void requestThemeChangeUpdates(IThemeChangeListener listener)
                throws RemoteException {
            getBrokeredService().requestThemeChangeUpdates(listener);
            mChangeListeners.register(listener);
        }

        @Override
        public void removeUpdates(IThemeChangeListener listener) throws RemoteException {
            getBrokeredService().removeUpdates(listener);
            mChangeListeners.unregister(listener);
        }

        @Override
        public void requestThemeChange(ThemeChangeRequest request,
                boolean removePerAppThemes) throws RemoteException {
            getBrokeredService().requestThemeChange(request, removePerAppThemes);
        }

        @Override
        public void applyDefaultTheme() throws RemoteException {
            getBrokeredService().applyDefaultTheme();
        }

        @Override
        public boolean isThemeApplying() throws RemoteException {
            return getBrokeredService().isThemeApplying();
        }

        @Override
        public int getProgress() throws RemoteException {
            return getBrokeredService().getProgress();
        }

        @Override
        public boolean processThemeResources(String themePkgName) throws RemoteException {
            return getBrokeredService().processThemeResources(themePkgName);
        }

        @Override
        public boolean isThemeBeingProcessed(String themePkgName) throws RemoteException {
            return getBrokeredService().isThemeBeingProcessed(themePkgName);
        }

        @Override
        public void registerThemeProcessingListener(
                IThemeProcessingListener listener) throws RemoteException {
            getBrokeredService().registerThemeProcessingListener(listener);
            mProcessingListeners.register(listener);
        }

        @Override
        public void unregisterThemeProcessingListener(
                IThemeProcessingListener listener) throws RemoteException {
            getBrokeredService().unregisterThemeProcessingListener(listener);
            mProcessingListeners.unregister(listener);
        }

        @Override
        public void rebuildResourceCache() throws RemoteException {
            getBrokeredService().rebuildResourceCache();
        }

        @Override
        public long getLastThemeChangeTime() throws RemoteException {
            return getBrokeredService().getLastThemeChangeTime();
        }

        @Override
        public int getLastThemeChangeRequestType() throws RemoteException {
            return getBrokeredService().getLastThemeChangeRequestType();
        }
    }

    public ThemeManagerServiceBroker(Context context) {
        super(context);
        setBrokeredServiceConnection(mServiceConnection);
    }

    @Override
    protected IThemeService getIBinderAsIInterface(@NonNull IBinder service) {
        return IThemeService.Stub.asInterface(service);
    }

    @Override
    protected IThemeService getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return SERVICE_COMPONENT;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.CM_THEME_SERVICE;
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
        publishBinderService(CMContextConstants.CM_THEME_SERVICE, new BinderService());
    }

    @Override
    protected String getComponentFilteringPermission() {
        return Manifest.permission.ACCESS_THEME_MANAGER;
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            tryConnecting();
        }
        super.onBootPhase(phase);
    }

    private BrokeredServiceConnection mServiceConnection = new BrokeredServiceConnection() {
        @Override
        public void onBrokeredServiceConnected() {
            // If any change listeners are cached, register them with the newly connected
            // service.
            IThemeService themeService =
                    getBrokeredService();
            try {
                int N = mChangeListeners.beginBroadcast();
                if (themeService != null && N > 0) {
                    for (int i = 0; i < N; i++) {
                        themeService.requestThemeChangeUpdates(
                                mChangeListeners.getBroadcastItem(i));
                    }
                }
            } catch (RemoteException e) {
                    /* ignore */
            } finally {
                mChangeListeners.finishBroadcast();
            }

            try {
                int N = mProcessingListeners.beginBroadcast();
                if (themeService != null && N > 0) {
                    for (int i = 0; i < N; i++) {
                        themeService.registerThemeProcessingListener(
                                mProcessingListeners.getBroadcastItem(i));
                    }
                }
            } catch (RemoteException e) {
                    /* ignore */
            } finally {
                mProcessingListeners.finishBroadcast();
            }
        }

        @Override
        public void onBrokeredServiceDisconnected() {
        }
    };
}
