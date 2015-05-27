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
import android.content.pm.PackageManager;
import android.hardware.ITorchCallback;
import android.hardware.ITorchService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TorchManager {

    public static final String TAG = TorchManager.class.getSimpleName();

    private static final int DISPATCH_TORCH_STATE_CHANGE = 1;
    private static final int DISPATCH_TORCH_ERROR = 2;
    private static final int DISPATCH_TORCH_AVAILABILITY_CHANGE = 3;

    private Context mContext;
    private ITorchService mService;
    private TorchHandler mHandler;

    private static TorchManager sInstance;

    private final List<TorchCallback> mCallbacks = new ArrayList<>();

    public static TorchManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TorchManager(context);
        }
        return sInstance;
    }

    private TorchManager(Context context) {
        mContext = context;
        mHandler = new TorchHandler(Looper.getMainLooper());
        IBinder b = ServiceManager.getService(CMContextConstants.CM_TORCH_SERVICE);
        mService = ITorchService.Stub.asInterface(b);
    }

    private class TorchHandler extends Handler {
        public TorchHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISPATCH_TORCH_STATE_CHANGE:
                    synchronized (mCallbacks) {
                        List<TorchCallback> listenersToRemove = new ArrayList<>();
                        for (TorchCallback listener : mCallbacks) {
                            try {
                                listener.onTorchStateChanged(msg.arg1 != 0);
                            } catch (Throwable e) {
                                Log.w(TAG, "Unable to update torch state", e);
                                listenersToRemove.add(listener);
                            }
                        }
                        if (listenersToRemove.size() > 0) {
                            for (TorchCallback listener : listenersToRemove) {
                                mCallbacks.remove(listener);
                            }
                        }
                    }
                    break;

                case DISPATCH_TORCH_ERROR:
                    synchronized (mCallbacks) {
                        List<TorchCallback> listenersToRemove = new ArrayList<>();
                        for (TorchCallback listener : mCallbacks) {
                            try {
                                listener.onTorchError();
                            } catch (Throwable e) {
                                Log.w(TAG, "Unable to update torch error", e);
                                listenersToRemove.add(listener);
                            }
                        }
                        if (listenersToRemove.size() > 0) {
                            for (TorchCallback listener : listenersToRemove) {
                                mCallbacks.remove(listener);
                            }
                        }
                    }
                    break;

                case DISPATCH_TORCH_AVAILABILITY_CHANGE:
                    synchronized (mCallbacks) {
                        List<TorchCallback> listenersToRemove = new ArrayList<>();
                        for (TorchCallback listener : mCallbacks) {
                            try {
                                listener.onTorchAvailabilityChanged(msg.arg1 == 1);
                            } catch (Throwable e) {
                                Log.w(TAG, "Unable to update torch availability change", e);
                                listenersToRemove.add(listener);
                            }
                        }
                        if (listenersToRemove.size() > 0) {
                            for (TorchCallback listener : listenersToRemove) {
                                mCallbacks.remove(listener);
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void addListener(TorchCallback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) {
                throw new IllegalArgumentException("Torch client was already added");
            }
            if (mCallbacks.size() == 0) {
                try {
                    mService.addListener(mTorchChangeListener);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to register torch listener");
                }
            }
            mCallbacks.add(callback);
        }
    }

    public void removeListener(TorchCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
            if (mCallbacks.size() == 0) {
                try {
                    mService.removeListener(mTorchChangeListener);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to remove torch listener");
                }
            }
        }
    }

    private final ITorchCallback mTorchChangeListener = new ITorchCallback.Stub() {
        @Override
        public void onTorchStateChanged(boolean on) throws RemoteException {
            mHandler.sendMessage(Message.obtain(mHandler, DISPATCH_TORCH_STATE_CHANGE,
                    on ? 1 : 0, 0));
        }

        @Override
        public void onTorchError() throws RemoteException {
            mHandler.sendEmptyMessage(DISPATCH_TORCH_ERROR);
        }

        @Override
        public void onTorchAvailabilityChanged(final boolean available) throws RemoteException {
            mHandler.sendMessage(Message.obtain(mHandler, DISPATCH_TORCH_AVAILABILITY_CHANGE,
                    available ? 1 : 0, 0));
        }
    };

    public void toggleTorch() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mService.toggleTorch();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setTorchEnabled(final boolean newState) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mService.setTorchEnabled(newState);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isTorchOn() {
        try {
            return mService.isTorchOn();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isTorchSupported() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public boolean isAvailable() {
        try {
            return mService.isAvailable();
        } catch (RemoteException e) {
            return false;
        }
    }

    public interface TorchCallback {
        /**
         * Called when the torch state changes
         */
        public void onTorchStateChanged(boolean on);
        /**
         * Called when there is an error that turns the torch off.
         */
        public void onTorchError();

        /**
         * Called when there is a change in availability of the torch functionality
         * @param available true if the torch is currently available.
         */
        public void onTorchAvailabilityChanged(boolean available);
    }


}
