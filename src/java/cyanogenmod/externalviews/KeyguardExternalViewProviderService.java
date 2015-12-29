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
import android.os.RemoteCallbackList;
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
 * A class for providing a view that can be displayed within the lock screen.  Applications that
 * wish to provide a view to be displayed within the lock screen should extend this service.
 *
 * <p>Applications extending this class should include the
 * {@link cyanogenmod.platform.Manifest.permission.THIRD_PARTY_KEYGUARD} permission in their
 * manifest</p>
 */
public abstract class KeyguardExternalViewProviderService extends Service {

    private static final String TAG = KeyguardExternalViewProviderService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private WindowManager mWindowManager;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IExternalViewProviderFactory.Stub() {
            @Override public IBinder createExternalView(final Bundle options) {
                FutureTask<IBinder> c = new FutureTask<IBinder>(new Callable<IBinder>() {
                    @Override
                    public IBinder call() throws Exception {
                        return KeyguardExternalViewProviderService.this
                                .createExternalView(options).mImpl;
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

    /**
     * Called when the host has bound to this service.
     * @param options Optional bundle.  This param is currently not used.
     * @return  The newly created provider.
     */
    protected abstract Provider createExternalView(Bundle options);

    /**
     * This class provides an interface for the host and service to communicate to each other.
     *
     * <p>Applications extending {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     * must also extend this class within their service.</p>
     */
    protected abstract class Provider {
        private final class ProviderImpl extends IKeyguardExternalViewProvider.Stub {
            private final Window mWindow;
            private final WindowManager.LayoutParams mParams;

            private boolean mShouldShow = true;
            private boolean mAskedShow = false;

            private final RemoteCallbackList<IKeyguardExternalViewCallbacks> mCallbacks =
                    new RemoteCallbackList<IKeyguardExternalViewCallbacks>();

            public ProviderImpl(Provider provider) {
                mWindow = new PhoneWindow(KeyguardExternalViewProviderService.this);
                ((ViewGroup) mWindow.getDecorView()).addView(onCreateView());

                mParams = new WindowManager.LayoutParams();
                mParams.type = provider.getWindowType();
                mParams.flags = provider.getWindowFlags();
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
            public void onKeyguardShowing(final boolean screenOn) throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onKeyguardShowing(screenOn);
                    }
                });
            }

            @Override
            public void onKeyguardDismissed() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onKeyguardDismissed();
                    }
                });
            }

            @Override
            public void onBouncerShowing(final boolean showing) throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onBouncerShowing(showing);
                    }
                });
            }

            @Override
            public void onScreenTurnedOn() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onScreenTurnedOn();
                    }
                });
            }

            @Override
            public void onScreenTurnedOff() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onScreenTurnedOff();
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

            @Override
            public void registerCallback(IKeyguardExternalViewCallbacks callback) {
                mCallbacks.register(callback);
            }

            @Override
            public void unregisterCallback(IKeyguardExternalViewCallbacks callback) {
                mCallbacks.unregister(callback);
            }

            private void updateVisibility() {
                if (DEBUG) Log.d(TAG, "shouldShow = " + mShouldShow + " askedShow = " + mAskedShow);
                mWindow.getDecorView().setVisibility(mShouldShow && mAskedShow ?
                        View.VISIBLE : View.GONE);
            }

            // callbacks from provider to host
            protected final void dismiss() {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.dismiss();
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

            protected final void dismissAndStartActivity(final Intent intent) {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.dismissAndStartActivity(intent);
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

            protected final void collapseNotificationPanel() {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.collapseNotificationPanel();
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

            protected final void setInteractivity(final boolean isInteractive) {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.setInteractivity(isInteractive);
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

        }

        private final ProviderImpl mImpl = new ProviderImpl(this);
        private final Bundle mOptions;

        protected Provider(Bundle options) {
            mOptions = options;
        }

        protected Bundle getOptions() {
            return mOptions;
        }

        /**
         * Called when the host view is attached to a window.
         */
        protected void onAttach() {}

        /**
         * Callback used for getting the view to be displayed within the host's content.
         * @return The view to be displayed within the host's content.
         */
        protected abstract View onCreateView();

        /**
         * Called when the host's activity or application is started.
         */
        protected void onStart() {}

        /**
         * Called when the host's activity or application is resumed.
         */
        protected void onResume() {}

        /**
         * Called when the host's activity or application is paused.
         */
        protected void onPause() {}

        /**
         * Called when the host's activity or application is stopped.
         */
        protected void onStop() {}

        /**
         * Called when the host view is detached from a window.
         */
        protected void onDetach() {}

        // keyguard events

        /**
         * Called from the host when the keyguard is being shown to the user.
         * @param screenOn  True if the screen is currently on.
         */
        protected abstract void onKeyguardShowing(boolean screenOn);

        /**
         * Called from the host when the use has unlocked the device.  Once this is called the lock
         * lock screen should no longer displayed.
         *
         * <p>The view component should enter a paused state when this is called, and save any state
         * information that may be needed once the lock screen is displayed again.  For example, a
         * non-interactive component that provides animated visuals should pause playback of those
         * animations and save the state, if necessary, of that animation.</p>
         */
        protected abstract void onKeyguardDismissed();

        /**
         * Called from the host when the keyguard is displaying the security screen for the user to
         * enter their pin, password, or pattern.
         *
         * <p>Interactive components will no longer have focus when the bouncer is displayed and
         * should enter a paused or idle state while the bouncer is being shown.</p>
         * @param showing True if the bouncer is being show or false when it is dismissed without the
         *                device being unlocked.
         */
        protected abstract void onBouncerShowing(boolean showing);

        /**
         * Called from the host when the screen is turned on.
         *
         * <p>The provided view should return to a running state when this is called.  For example,
         * a non-interactive component that provides animated visuals should resume playback of
         * those animations.</p>
         */
        protected abstract void onScreenTurnedOn();

        /**
         * Called from the host when the screen is turned off.
         *
         * <p>The provided view should provided view should pause its activity, if not currently
         * in a paused state, and do any work necessary to be ready when the screen is turned
         * back on.  This will allow for a seamless user experience once the screen is turned on.
         * </p>
         */
        protected abstract void onScreenTurnedOff();

        // callbacks from provider to host

        /**
         * This method should be called whenever an action has occurred within your view that would
         * require it to be dismissed and either unlock the device or show the bouncer so the user
         * can enter their security pin, password, or pattern.
         *
         * <p>If the user has a secure lock screen and dismisses the bouncer without entering their
         * secure code, onBouncerShowing should be called with onShowing being set to false.</p>
         */
        protected final void dismiss() {
            mImpl.dismiss();
        }

        /**
         * Similar to dismiss() with the added action of launching the provided intent once the lock
         * screen is unlocked.
         * @param intent An intent specifying an activity to launch.
         */
        protected final void dismissAndStartActivity(final Intent intent) {
            mImpl.dismissAndStartActivity(intent);
        }

        /**
         * Call this method when you would like to take focus and hide the notification panel.
         *
         * <p>You should call this method if your component requires focus and the users's
         * attention.  This has no effect for non-interactive components.</p>
         */
        protected final void collapseNotificationPanel() {
            mImpl.collapseNotificationPanel();
        }

        /**
         * This method should be called when the provided view needs to change from interactive to
         * non-interactive and vice versa.
         *
         * <p>Interactive components can receive input focus and receive user interaction while
         * non-interactive components never receive focus and are purely visual.</p>
         * @param isInteractive
         */
        protected final void setInteractivity(final boolean isInteractive) {
            mImpl.setInteractivity(isInteractive);
        }

        /*package*/ final int getWindowType() {
            return WindowManager.LayoutParams.TYPE_KEYGUARD_PANEL;
        }

        /*package*/ final int getWindowFlags() {
            return WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
    }
}
