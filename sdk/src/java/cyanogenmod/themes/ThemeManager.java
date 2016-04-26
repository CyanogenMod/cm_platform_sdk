/*
 * Copyright (C) 2014-2016 The CyanogenMod Project
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

package cyanogenmod.themes;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.themes.ThemeChangeRequest.RequestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages changing and applying of themes.
 * <p>Get an instance of this class by calling blah blah blah</p>
 */
public class ThemeManager {
    private static final String TAG = ThemeManager.class.getName();
    private static IThemeService sService;
    private static ThemeManager sInstance;
    private static Handler mHandler;

    private Set<ThemeChangeListener> mChangeListeners = new ArraySet<>();

    private Set<ThemeProcessingListener> mProcessingListeners = new ArraySet<>();

    private ThemeManager(Context context) {
        sService = getService();
        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.THEMES) && sService == null) {
            Log.wtf(TAG, "Unable to get ThemeManagerService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static ThemeManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThemeManager(context);
        }

        return sInstance;
    }

    /** @hide */
    public static IThemeService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_THEME_SERVICE);
        if (b != null) {
            sService = IThemeService.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    private final IThemeChangeListener mThemeChangeListener = new IThemeChangeListener.Stub() {
        @Override
        public void onProgress(final int progress) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mChangeListeners) {
                        List<ThemeChangeListener> listenersToRemove = new ArrayList<>();
                        for (ThemeChangeListener listener : mChangeListeners) {
                            try {
                                listener.onProgress(progress);
                            } catch (Throwable e) {
                                Log.w(TAG, "Unable to update theme change progress", e);
                                listenersToRemove.add(listener);
                            }
                        }
                        if (listenersToRemove.size() > 0) {
                            for (ThemeChangeListener listener : listenersToRemove) {
                                mChangeListeners.remove(listener);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onFinish(final boolean isSuccess) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mChangeListeners) {
                        List<ThemeChangeListener> listenersToRemove = new ArrayList<>();
                        for (ThemeChangeListener listener : mChangeListeners) {
                            try {
                                listener.onFinish(isSuccess);
                            } catch (Throwable e) {
                                Log.w(TAG, "Unable to update theme change listener", e);
                                listenersToRemove.add(listener);
                            }
                        }
                        if (listenersToRemove.size() > 0) {
                            for (ThemeChangeListener listener : listenersToRemove) {
                                mChangeListeners.remove(listener);
                            }
                        }
                    }
                }
            });
        }
    };

    private final IThemeProcessingListener mThemeProcessingListener =
            new IThemeProcessingListener.Stub() {
        @Override
        public void onFinishedProcessing(final String pkgName) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mProcessingListeners) {
                        List<ThemeProcessingListener> listenersToRemove = new ArrayList<>();
                        for (ThemeProcessingListener listener : mProcessingListeners) {
                            try {
                                listener.onFinishedProcessing(pkgName);
                            } catch (Throwable e) {
                                Log.w(TAG, "Unable to update theme change progress", e);
                                listenersToRemove.add(listener);
                            }
                        }
                        if (listenersToRemove.size() > 0) {
                            for (ThemeProcessingListener listener : listenersToRemove) {
                                mProcessingListeners.remove(listener);
                            }
                        }
                    }
                }
            });
        }
    };


    /**
     * @deprecated Use {@link ThemeManager#registerThemeChangeListener(ThemeChangeListener)} instead
     */
    public void addClient(ThemeChangeListener listener) {
        registerThemeChangeListener(listener);
    }

    /**
     * @deprecated Use {@link ThemeManager#unregisterThemeChangeListener(ThemeChangeListener)}
     * instead
     */
    public void removeClient(ThemeChangeListener listener) {
        unregisterThemeChangeListener(listener);
    }

    /**
     * @deprecated Use {@link ThemeManager#unregisterThemeChangeListener(ThemeChangeListener)}
     * instead
     */
    public void onClientPaused(ThemeChangeListener listener) {
        unregisterThemeChangeListener(listener);
    }

    /**
     * @deprecated Use {@link ThemeManager#registerThemeChangeListener(ThemeChangeListener)} instead
     */
    public void onClientResumed(ThemeChangeListener listener) {
        registerThemeChangeListener(listener);
    }

    /**
     * @deprecated Use {@link ThemeManager#unregisterThemeChangeListener(ThemeChangeListener)}
     * instead
     */
    public void onClientDestroyed(ThemeChangeListener listener) {
        unregisterThemeChangeListener(listener);
    }

    /**
     * Register a {@link ThemeChangeListener} to be notified when a theme is done being processed.
     * @param listener {@link ThemeChangeListener} to register
     */
    public void registerThemeChangeListener(ThemeChangeListener listener) {
        synchronized (mChangeListeners) {
            if (mChangeListeners.contains(listener)) {
                throw new IllegalArgumentException("Listener already registered");
            }
            if (mChangeListeners.size() == 0) {
                try {
                    sService.requestThemeChangeUpdates(mThemeChangeListener);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to register listener", e);
                }
            }
            mChangeListeners.add(listener);
        }
    }

    /**
     * Unregister a {@link ThemeChangeListener}
     * @param listener {@link ThemeChangeListener} to unregister
     */
    public void unregisterThemeChangeListener(ThemeChangeListener listener) {
        synchronized (mChangeListeners) {
            mChangeListeners.remove(listener);
            if (mChangeListeners.size() == 0) {
                try {
                    sService.removeUpdates(mThemeChangeListener);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to unregister listener", e);
                }
            }
        }
    }

    /**
     * Register a {@link ThemeProcessingListener} to be notified when a theme is done being
     * processed.
     * @param listener {@link ThemeProcessingListener} to register
     */
    public void registerProcessingListener(ThemeProcessingListener listener) {
        synchronized (mProcessingListeners) {
            if (mProcessingListeners.contains(listener)) {
                throw new IllegalArgumentException("Listener already registered");
            }
            if (mProcessingListeners.size() == 0) {
                try {
                    sService.registerThemeProcessingListener(mThemeProcessingListener);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to register listener", e);
                }
            }
            mProcessingListeners.add(listener);
        }
    }

    /**
     * Unregister a {@link ThemeProcessingListener}.
     * @param listener {@link ThemeProcessingListener} to unregister
     */
    public void unregisterProcessingListener(ThemeProcessingListener listener) {
        synchronized (mProcessingListeners) {
            mProcessingListeners.remove(listener);
            if (mProcessingListeners.size() == 0) {
                try {
                    sService.unregisterThemeProcessingListener(mThemeProcessingListener);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to unregister listener", e);
                }
            }
        }
    }

    public void requestThemeChange(String pkgName, List<String> components) {
        requestThemeChange(pkgName, components, true);
    }

    public void requestThemeChange(String pkgName, List<String> components,
            boolean removePerAppThemes) {
        Map<String, String> componentMap = new HashMap<>(components.size());
        for (String component : components) {
            componentMap.put(component, pkgName);
        }
        requestThemeChange(componentMap, removePerAppThemes);
    }

    public void requestThemeChange(Map<String, String> componentMap) {
        requestThemeChange(componentMap, true);
    }

    public void requestThemeChange(Map<String, String> componentMap, boolean removePerAppThemes) {
        ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
        for (String component : componentMap.keySet()) {
            builder.setComponent(component, componentMap.get(component));
        }

        requestThemeChange(builder.build(), removePerAppThemes);
    }

    public void requestThemeChange(ThemeChangeRequest request, boolean removePerAppThemes) {
        try {
            sService.requestThemeChange(request, removePerAppThemes);
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }
    }

    public void applyDefaultTheme() {
        try {
            sService.applyDefaultTheme();
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }
    }

    public boolean isThemeApplying() {
        try {
            return sService.isThemeApplying();
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }

        return false;
    }

    public boolean isThemeBeingProcessed(String themePkgName) {
        try {
            return sService.isThemeBeingProcessed(themePkgName);
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }
        return false;
    }

    public int getProgress() {
        try {
            return sService.getProgress();
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }
        return -1;
    }

    public boolean processThemeResources(String themePkgName) {
        try {
            return sService.processThemeResources(themePkgName);
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }
        return false;
    }

    public long getLastThemeChangeTime() {
        try {
            return sService.getLastThemeChangeTime();
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }
        return 0;
    }

    public ThemeChangeRequest.RequestType getLastThemeChangeRequestType() {
        try {
            int type = sService.getLastThemeChangeRequestType();
            return (type >= 0 && type < RequestType.values().length)
                    ? RequestType.values()[type]
                    : null;
        } catch (RemoteException e) {
            logThemeServiceException(e);
        }

        return null;
    }

    private void logThemeServiceException(Exception e) {
        Log.w(TAG, "Unable to access ThemeService", e);
    }

    public interface ThemeChangeListener {
        void onProgress(int progress);
        void onFinish(boolean isSuccess);
    }

    public interface ThemeProcessingListener {
        void onFinishedProcessing(String pkgName);
    }
}

