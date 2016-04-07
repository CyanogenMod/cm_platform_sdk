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
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.policy.PhoneWindow;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * A class for providing a view that can be displayed within the lock screen.  Applications that
 * wish to provide a view to be displayed within the lock screen should extend this service.
 *
 * <p>Applications extending this class should include the
 * {@link cyanogenmod.platform.Manifest.permission#THIRD_PARTY_KEYGUARD} permission in their
 * manifest</p>

 * <p>Applications extending this class should also extend
 * {@link KeyguardExternalViewProviderService.Provider} and return a new instance of
 * {@link KeyguardExternalViewProviderService.Provider} in
 * {@link KeyguardExternalViewProviderService#createExternalView(Bundle)}.</p>
 */
public abstract class KeyguardExternalViewProviderService extends Service {

    private static final String TAG = KeyguardExternalViewProviderService.class.getSimpleName();
    private static final boolean DEBUG = false;

    /**
     * The action that must be declared as handled by this service.
     *
     * <p>{@code
     *  <intent-filter>
     *      <action android:name="cyanogenmod.externalviews.KeyguardExternalViewProviderService"/>
     *  </intent-filter>
     *}</p>
     */
    public static final String SERVICE_INTERFACE =
            "cyanogenmod.externalviews.KeyguardExternalViewProviderService";

    /**
     * Name under which an external keyguard view publishes information about itself.
     * This meta-data must reference an XML resource containing
     * a <code>&lt;lockscreen&gt;</code>
     * tag.
     */
    public static final String META_DATA = "cyanogenmod.externalviews.keyguard";

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
     */
    protected abstract class Provider {
        private final class ProviderImpl extends IKeyguardExternalViewProvider.Stub
                implements Window.Callback {
            private final Window mWindow;
            private final WindowManager.LayoutParams mParams;

            private boolean mShouldShow = true;
            private boolean mAskedShow = false;

            private final RemoteCallbackList<IKeyguardExternalViewCallbacks> mCallbacks =
                    new RemoteCallbackList<IKeyguardExternalViewCallbacks>();

            public ProviderImpl(Provider provider) {
                mWindow = new PhoneWindow(KeyguardExternalViewProviderService.this);
                mWindow.setCallback(this);
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
            public void onLockscreenSlideOffsetChanged(final float swipeProgress)
                    throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Provider.this.onLockscreenSlideOffsetChanged(swipeProgress);
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
            protected final boolean requestDismiss() {
                boolean ret = true;
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        ret &= callback.requestDismiss();
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
                return ret;
            }

            protected final boolean requestDismissAndStartActivity(final Intent intent) {
                boolean ret = true;
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        ret &= callback.requestDismissAndStartActivity(intent);
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
                return ret;
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

            public void slideLockscreenIn() {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.slideLockscreenIn();
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

            // region Window callbacks
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                return false;
            }

            @Override
            public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                return false;
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                return false;
            }

            @Override
            public boolean dispatchTrackballEvent(MotionEvent event) {
                return false;
            }

            @Override
            public boolean dispatchGenericMotionEvent(MotionEvent event) {
                return false;
            }

            @Override
            public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                return false;
            }

            @Override
            public View onCreatePanelView(int featureId) {
                return null;
            }

            @Override
            public boolean onCreatePanelMenu(int featureId, Menu menu) {
                return false;
            }

            @Override
            public boolean onPreparePanel(int featureId, View view, Menu menu) {
                return false;
            }

            @Override
            public boolean onMenuOpened(int featureId, Menu menu) {
                return false;
            }

            @Override
            public boolean onMenuItemSelected(int featureId, MenuItem item) {
                return false;
            }

            @Override
            public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {}

            @Override
            public void onContentChanged() {}

            @Override
            public void onWindowFocusChanged(boolean hasFocus) {}

            @Override
            public void onAttachedToWindow() {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.onAttachedToWindow();
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

            @Override
            public void onDetachedFromWindow() {
                int N = mCallbacks.beginBroadcast();
                for(int i=0; i < N; i++) {
                    IKeyguardExternalViewCallbacks callback = mCallbacks.getBroadcastItem(0);
                    try {
                        callback.onDetachedFromWindow();
                    } catch(RemoteException e) {
                    }
                }
                mCallbacks.finishBroadcast();
            }

            @Override
            public void onPanelClosed(int featureId, Menu menu) {}

            @Override
            public boolean onSearchRequested() {
                return false;
            }

            @Override
            public boolean onSearchRequested(SearchEvent searchEvent) {
                return false;
            }

            @Override
            public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
                return null;
            }

            @Override
            public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
                return null;
            }

            @Override
            public void onActionModeStarted(ActionMode mode) {}

            @Override
            public void onActionModeFinished(ActionMode mode) {}
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
         * Called when the host view is detached from a window.
         */
        protected void onDetach() {}

        /**
         * Callback used for getting the view to be displayed within the host's content.
         * @return The view to be displayed within the host's content.  If null is returned no
         *         content will be displayed.
         */
        protected abstract View onCreateView();

        // keyguard events

        /**
         * Called from the host when the keyguard is being shown to the user.
         * @param screenOn  True if the screen is currently on.
         */
        protected abstract void onKeyguardShowing(boolean screenOn);

        /**
         * Called from the host when the user has unlocked the device.  Once this is called the lock
         * lock screen is no longer being displayed.
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

        /**
         * Called from the host when the user is swiping the lockscreen
         * to transition into the live lock screen
         *
         * @param swipeProgress [0-1] represents the progress of the swipe
         */
        protected void onLockscreenSlideOffsetChanged(float swipeProgress) {}

        // callbacks from provider to host

        /**
         * Request that the keyguard be dismissed.   Calling this method will dismiss the lock
         * screen, if it is a not secure, or present the user with the security screen for the user
         * to enter their security code to finish dismissing the lock screen.
         *
         * <p>If the user has a secure lock screen and dismisses the bouncer without entering their
         * secure code, the lock screen will not be dismissed and
         * {@link KeyguardExternalViewProviderService.Provider#onBouncerShowing(boolean)} will be
         * called with {@code onShowing} being set to false, indicating that the lock screen was not
         * dismissed as requested.</p>
         * @return True if the call succeeded.
         */
        protected final boolean requestDismiss() {
            return mImpl.requestDismiss();
        }

        /**
         * Request that the keyguard be dismissed and the activity provided by the given intent be
         * started once the keyguard is dismissed.   If a secure lock screen is being used the user
         * will need to enter their correct security code to finish dismissing the lock screen.
         *
         * <p>If the user has a secure lock screen and dismisses the bouncer without entering their
         * secure code, the lock screen will not be dismissed and
         * {@link KeyguardExternalViewProviderService.Provider#onBouncerShowing(boolean)} will be
         * called with onShowing being set to false, indicating that the lock screen was not
         * dismissed as requested.</p>
         * @param intent An intent specifying an activity to launch.
         * @return True if the call succeeded.
         */
        protected final boolean requestDismissAndStartActivity(final Intent intent) {
            return mImpl.requestDismissAndStartActivity(intent);
        }

        /**
         * Call this method when you would like to take focus and hide the notification panel.
         *
         * <p>You should call this method if your component requires focus and the users's
         * attention.  The user will still be able to bring the notifications back into view by
         * sliding down from the status bar.
         * Calling this method has no effect for non-interactive components.</p>
         * @deprecated As of SDK version {@link cyanogenmod.os.Build.CM_VERSION_CODES#ELDERBERRY}
         * this does nothing.
         */
        @Deprecated
        protected final void collapseNotificationPanel() {
            /* do nothing */
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

        /**
         * Call this method when you like to slide in the lockscreen on top of
         * your live lockscreen. Only relevant if you use
         * {@link KeyguardExternalViewProviderService.Provider#setInteractivity(boolean)}
         */
        protected final void slideLockscreenIn() {
            mImpl.slideLockscreenIn();
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
