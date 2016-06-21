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

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;

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

    @Override
    public void onStart() {
        // empty
    }

    protected abstract T getIBinderAsIInterface(IBinder service);

    protected abstract T getDefaultImplementation();

    @Nullable
    protected abstract ComponentName getServiceComponent();

    public void setBrokeredServiceConnection(BrokeredServiceConnection brokeredServiceCn) {
        Slog.e(TAG, "Setting brokered service connection " + brokeredServiceCn.toString());
        mBrokeredServiceConnection = brokeredServiceCn;
    }

    public T getImplementingServiceGuarded() {
        final T service = getOrConnectService();
        if (service != null) {
            return service;
        }
        return getDefaultImplementation();
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

    public void tryConnecting() {
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
            intent.setComponent(getServiceComponent());
            try {
                if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                    Slog.e(TAG, "Failed to bind to implementation");
                }
            } catch (SecurityException e) {
                Slog.e(TAG, "Forbidden to bind to implementation", e);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.i(TAG, "Implementation service connected");
            synchronized (BrokerableCMSystemService.this) {
                mImplementingBinderInterface = getIBinderAsIInterface(service);
                System.out.println(TAG + " Implementing " + mImplementingBinderInterface);
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

    public interface BrokeredServiceConnection {
        void onBrokeredServiceConnected();
        void onBrokeredServiceDisconnected();
    }

}
