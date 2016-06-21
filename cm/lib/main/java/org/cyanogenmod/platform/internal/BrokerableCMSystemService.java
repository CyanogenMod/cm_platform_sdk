/**
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
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;

import cyanogenmod.platform.Manifest;

import com.android.internal.util.Preconditions;

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

public abstract class BrokerableCMSystemService<T extends IInterface> extends CMSystemService {
    private static final String TAG = BrokerableCMSystemService.class.getSimpleName();

    private static final int MSG_TRY_CONNECTING = 1;
    private static final long SERVICE_CONNECTION_WAIT_TIME_MS = 4 * 1000L; // 4 seconds
    private Context mContext;

    private BrokeredServiceConnection mBrokeredServiceConnection;
    private T mImplementingBinderInterface;

    public BrokerableCMSystemService(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Called when the {@link IInterface} contract for the given {@link IBinder} is to
     * be assigned for the implementing service {@link T}
     * @param service
     * @return {@link T}
     */
    protected abstract T getIBinderAsIInterface(@NonNull IBinder service);

    /**
     * Called when necessary as the default implementation. (usually a failure implementation)
     * For when an implementing service is not found, is updating, or is failing.
     * @return {@link T}
     */
    protected abstract T getDefaultImplementation();

    /**
     * Called when attempting to connect to the a given {@link T} implementation. Defines
     * the {@link ComponentName} to be used for the binding operation.
     *
     * By default, the calling component MUST gate its implementation by the
     * {@link Manifest.permission.BIND_CORE_SERVICE} permission as well as using,
     * the permission granted to its own package.
     *
     * This permission can be overridden via {@link #getComponentFilteringPermission()}
     *
     * @return {@link ComponentName}
     */
    @Nullable
    protected abstract ComponentName getServiceComponent();

    /**
     * Override this method if your broker will provide its own permission for guarding a vertical
     * api defintion. Otherwise, the component from {@link #getServiceComponent()}
     * will be gated via the {@link Manifest.permission.BIND_CORE_SERVICE} permission.
     *
     * @return boolean
     */
    @NonNull
    protected String getComponentFilteringPermission() {
        return Manifest.permission.BIND_CORE_SERVICE;
    }

    /**
     * Set a {@link BrokeredServiceConnection} to receive callbacks when an implementation is
     * connected or disconnected.
     * @param brokeredServiceComponent
     */
    public final void setBrokeredServiceConnection(
            @NonNull BrokeredServiceConnection brokeredServiceComponent) {
        Preconditions.checkNotNull(brokeredServiceComponent);
        Slog.e(TAG, "Setting brokered service connection "
                + brokeredServiceComponent.toString());
        mBrokeredServiceConnection = brokeredServiceComponent;
    }

    /**
     * Get the implementing service for the given binder invocation. Usually called from a binder
     * thread in a subclassed service.
     * @return {@link T} that represents the implementing service
     */
    public final T getBrokeredService() {
        final T service = getOrConnectService();
        if (service != null) {
            return service;
        }
        return getDefaultImplementation();
    }

    @Override
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == PHASE_THIRD_PARTY_APPS_CAN_START) {
            Slog.d(TAG, "Third party apps ready");
            tryConnecting();
        }
    }

    private T getOrConnectService() {
        synchronized (this) {
            if (mImplementingBinderInterface != null) {
                return mImplementingBinderInterface;
            }
            // Service is not connected. Try blocking connecting.
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
                if (mImplementingBinderInterface != null) {
                    // Success
                    return mImplementingBinderInterface;
                }
                // Calculate remaining waiting time to make sure we wait the full timeout period
                waitTime = shouldEnd - SystemClock.elapsedRealtime();
            }
            return null;
        }
    }

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

    /**
     * Attempt to connect to the component which is going to serve {@link T}
     * interface contract implementation.
     */
    public final void tryConnecting() {
        Slog.i(TAG, "Connecting to implementation");
        synchronized (this) {
            if (mImplementingBinderInterface != null) {
                Slog.d(TAG, "Already connected");
                return;
            }
            final Intent intent = new Intent();
            final ComponentName cn = getServiceComponent();
            if (cn == null) {
                Slog.e(TAG, "No implementation service found");
                return;
            }
            intent.setComponent(cn);
            try {
                if (mContext.getPackageManager().checkPermission(
                        getComponentFilteringPermission(),
                        cn.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                    Slog.e(TAG, "Target component lacks " + getComponentFilteringPermission()
                            + " service permission, failing " + cn);
                    return;
                }
                if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                    Slog.e(TAG, "Failed to bind to implementation " + cn);
                }
            } catch (SecurityException e) {
                Slog.e(TAG, "Forbidden to bind to implementation " + cn, e);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.i(TAG, "Implementation service connected");
            synchronized (BrokerableCMSystemService.this) {
                mImplementingBinderInterface = getIBinderAsIInterface(service);
                BrokerableCMSystemService.this.notifyAll();
                if (mBrokeredServiceConnection != null) {
                    Slog.i(TAG, "Notifying service connected");
                    mBrokeredServiceConnection.onBrokeredServiceConnected();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Slog.i(TAG, "Implementation service unexpectedly disconnected");
            synchronized (BrokerableCMSystemService.this) {
                mImplementingBinderInterface = null;
                BrokerableCMSystemService.this.notifyAll();
                if (mBrokeredServiceConnection != null) {
                    mBrokeredServiceConnection.onBrokeredServiceDisconnected();
                }
            }
        }
    };
}
