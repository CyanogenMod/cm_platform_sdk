/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package cyanogenmod.externalviews;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.policy.PhoneWindow;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * TODO: unhide once documented and finalized
 * @hide
 */
public abstract class ExternalViewProviderService extends Service {

    private static final String TAG = "ExternalViewProvider";
    private static final boolean DEBUG = false;

    private WindowManager mWindowManager;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IExternalViewProviderFactory.Stub() {
            @Override public IBinder createExternalView(final Bundle options) {
                FutureTask<IBinder> c = new FutureTask<IBinder>(new Callable<IBinder>() {
                    @Override
                    public IBinder call() throws Exception {
                        return ExternalViewProviderService.this.createExternalView(options).mImpl;
                    }
                });
                mHandler.post(c);
                try {
                    return c.get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "error: ", e);
                    return null;
                }
            }
        };
    }

    protected abstract Provider createExternalView(Bundle options);

    protected abstract class Provider {
        private final class ProviderImpl extends IExternalViewProvider.Stub {
            private final Window mWindow;
            private final WindowManager.LayoutParams mParams;

            private boolean mShouldShow = true;
            private boolean mAskedShow = false;

            public ProviderImpl() {
                mWindow = new PhoneWindow(ExternalViewProviderService.this);
                ((ViewGroup) mWindow.getDecorView()).addView(onCreateView());

                mParams = new WindowManager.LayoutParams();
                mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                mParams.gravity = Gravity.LEFT | Gravity.TOP;
                mParams.format = PixelFormat.TRANSPARENT;
            }

            @Override
            public void onAttach(IBinder windowToken) throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWindowManager.addView(mWindow.getDecorView(), mParams);
                        Provider.this.onAttach();
                    }
                });
            }

            @Override
            public void onStart() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onStart();
                    }
                });
            }

            @Override
            public void onResume() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mShouldShow = true;
                        updateVisibility();
                        Provider.this.onResume();
                    }
                });
            }

            @Override
            public void onPause() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mShouldShow = false;
                        updateVisibility();
                        Provider.this.onPause();
                    }
                });
            }

            @Override
            public void onStop() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onStop();
                    }
                });
            }

            @Override
            public void onDetach() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWindowManager.removeView(mWindow.getDecorView());
                        Provider.this.onDetach();
                    }
                });
            }

            @Override
            public void alterWindow(final int x, final int y, final int width, final int height,
                                    final boolean visible, final Rect clipRect) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mParams.x = x;
                        mParams.y = y;
                        mParams.width = width;
                        mParams.height = height;

                        if (DEBUG) Log.d(TAG, mParams.toString());

                        mAskedShow = visible;

                        updateVisibility();

                        View decorView = mWindow.getDecorView();
                        if (decorView.getVisibility() == View.VISIBLE) {
                            decorView.setClipBounds(clipRect);
                        }

                        if (mWindow.getDecorView().getVisibility() != View.GONE)
                            mWindowManager.updateViewLayout(mWindow.getDecorView(), mParams);
                    }
                });
            }

            private void updateVisibility() {
                if (DEBUG) Log.d(TAG, "shouldShow = " + mShouldShow + " askedShow = " + mAskedShow);
                mWindow.getDecorView().setVisibility(mShouldShow && mAskedShow ?
                        View.VISIBLE : View.GONE);
            }
        }

        private final ProviderImpl mImpl = new ProviderImpl();
        private final Bundle mOptions;

        protected Provider(Bundle options) {
            mOptions = options;
        }

        protected Bundle getOptions() {
            return mOptions;
        }

        protected void onAttach() {}
        protected abstract View onCreateView();
        protected void onStart() {}
        protected void onResume() {}
        protected void onPause() {}
        protected void onStop() {}
        protected void onDetach() {}
    }
}
