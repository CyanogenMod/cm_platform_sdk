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

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.LinkedList;

/**
 * This class provides a placeholder view for hosting an external view, provided by a
 * {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}, within the lock screen.
 *
 * <p>This class is intended to only be used within the SystemUi process.</p>
 * @hide
 */
public class KeyguardExternalView extends View implements ViewTreeObserver.OnPreDrawListener,
        IBinder.DeathRecipient {
    private static final String TAG = KeyguardExternalView.class.getSimpleName();

    /**
     * An extra passed via an intent that provides a list of permissions that should be requested
     * from the user.
     */
    public static final String EXTRA_PERMISSION_LIST = "permissions_list";

    /**
     * Category defining an activity to call to request permissions that a
     * {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService} will need.  Apps that
     * provide a {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService} should
     * check that they have the required permission before making any method calls that would
     * require a dangerous permission to be granted.
     */
    public static final String CATEGORY_KEYGUARD_GRANT_PERMISSION
            = "org.cyanogenmod.intent.category.KEYGUARD_GRANT_PERMISSION";

    private LinkedList<Runnable> mQueue = new LinkedList<Runnable>();

    private Context mContext;
    private final ExternalViewProperties mExternalViewProperties;
    private volatile IKeyguardExternalViewProvider mExternalViewProvider;
    private IBinder mService;
    private final Point mDisplaySize;
    private boolean mIsInteractive;

    private KeyguardExternalViewCallbacks mCallback;

    private OnWindowAttachmentChangedListener mWindowAttachmentListener;

    public KeyguardExternalView(Context context, AttributeSet attrs) {
        this(context, attrs, null);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs);
    }

    /**
     * @param context
     * @param attributeSet
     * @param componentName The component name for the
     *                      {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     *                      that will be bound to create the external view.
     */
    public KeyguardExternalView(Context context, AttributeSet attributeSet, ComponentName componentName) {
        super(context, attributeSet);
        mContext = getContext();
        mExternalViewProperties = new ExternalViewProperties(this, mContext);
        if (componentName != null) {
            mContext.bindService(new Intent().setComponent(componentName),
                    mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        mDisplaySize = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealSize(mDisplaySize);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                IExternalViewProviderFactory factory = IExternalViewProviderFactory.Stub.asInterface(service);
                if (factory != null) {
                    mExternalViewProvider = IKeyguardExternalViewProvider.Stub.asInterface(
                            factory.createExternalView(null));
                    if (mExternalViewProvider != null) {
                        mExternalViewProvider.registerCallback(
                                KeyguardExternalView.this.mKeyguardExternalViewCallbacks);
                        mService = service;
                        mService.linkToDeath(KeyguardExternalView.this, 0);
                        executeQueue();
                    } else {
                        Log.e(TAG, "Unable to get external view provider");
                    }
                } else {
                    Log.e(TAG, "Unable to get external view provider factory");
                }
            } catch (RemoteException | SecurityException e) {
                Log.e(TAG, "Unable to connect to service", e);
            }
            // We should unbind the service if we failed to connect to the provider
            if (mService != service && service != null) {
                mContext.unbindService(mServiceConnection);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (mExternalViewProvider != null) {
                try {
                    mExternalViewProvider.unregisterCallback(
                            KeyguardExternalView.this.mKeyguardExternalViewCallbacks);
                } catch (RemoteException e) {
                }
                mExternalViewProvider = null;
            }
            if (mService != null) {
                mService.unlinkToDeath(KeyguardExternalView.this, 0);
                mService = null;
            }
        }
    };

    private final IKeyguardExternalViewCallbacks mKeyguardExternalViewCallbacks =
            new IKeyguardExternalViewCallbacks.Stub() {
        @Override
        public boolean requestDismiss() throws RemoteException {
            if (mCallback != null) {
                return mCallback.requestDismiss();
            }

            return false;
        }

        @Override
        public boolean requestDismissAndStartActivity(Intent intent) throws RemoteException {
            if (mCallback != null) {
                return mCallback.requestDismissAndStartActivity(intent);
            }

            return false;
        }

        @Override
        public void collapseNotificationPanel() throws RemoteException {
            /* collapseNotificationPanel is deprecated so do nothing */
        }

        @Override
        public void setInteractivity(boolean isInteractive) {
            mIsInteractive = isInteractive;
        }

        @Override
        public void onAttachedToWindow() {
            if (mWindowAttachmentListener != null) {
                mWindowAttachmentListener.onAttachedToWindow();
            }
        }

        @Override
        public void onDetachedFromWindow() {
            if (mWindowAttachmentListener != null) {
                mWindowAttachmentListener.onDetachedFromWindow();
            }
        }

        @Override
        public void slideLockscreenIn() {
            if (mCallback != null) {
                mCallback.slideLockscreenIn();
            }
        }
    };

    private void executeQueue() {
        while (!mQueue.isEmpty()) {
            Runnable r = mQueue.pop();
            r.run();
        }
    }

    protected void performAction(Runnable r) {
        if (mExternalViewProvider != null) {
            r.run();
        } else {
            mQueue.add(r);
        }
    }

    // view overrides, for positioning

    @Override
    public boolean onPreDraw() {
        if (!mExternalViewProperties.hasChanged()) {
            return true;
        }
        // keyguard views always take up the full screen when visible
        final int x = mExternalViewProperties.getX();
        final int y = mExternalViewProperties.getY();
        final int width = mDisplaySize.x - x;
        final int height = mDisplaySize.y - y;
        final boolean visible = mExternalViewProperties.isVisible();
        final Rect clipRect = new Rect(x, y, width + x, height + y);
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.alterWindow(x, y, width, height, visible,
                            clipRect);
                } catch (RemoteException e) {
                }
            }
        });
        return true;
    }

    // Placeholder callbacks

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onDetach();
                } catch (RemoteException e) {
                }
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onAttach(null);
                } catch (RemoteException e) {
                }
            }
        });
    }

    @Override
    public void binderDied() {
        if (mCallback != null) {
            mCallback.providerDied();
        }
    }

    /**
     * Sets the component of the {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     * to be used for this ExternalView.  If a provider is already connected to this view, it is
     * first unbound before binding to the new provider.
     * @param componentName The {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     *                      to bind to.
     */
    public void setProviderComponent(ComponentName componentName) {
        // unbind any existing external view provider
        if (mExternalViewProvider != null) {
            mContext.unbindService(mServiceConnection);
        }
        if (componentName != null) {
            mContext.bindService(new Intent().setComponent(componentName),
                    mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Called from the host when the keyguard is being shown to the user.
     * @param screenOn  True if the screen is currently on.
     */
    public void onKeyguardShowing(final boolean screenOn) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onKeyguardShowing(screenOn);
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * Called from the host when the user has unlocked the device.  Once this is called the lock
     * lock screen should no longer displayed.
     */
    public void onKeyguardDismissed() {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onKeyguardDismissed();
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * Called from the host when the keyguard is displaying the security screen for the user to
     * enter their pin, password, or pattern.
     * @param showing True if the bouncer is being show or false when it is dismissed without the
     *                device being unlocked.
     */
    public void onBouncerShowing(final boolean showing) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onBouncerShowing(showing);
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * Called from the host when the screen is turned on.
     */
    public void onScreenTurnedOn() {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onScreenTurnedOn();
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * Called from the host when the screen is turned off.
     */
    public void onScreenTurnedOff() {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onScreenTurnedOff();
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * Called from the host when the user is swiping the lockscreen
     * to transition into the live lock screen
     *
     * @param swipeProgress [0-1] represents the progress of the swipe
     */
    public void onLockscreenSlideOffsetChanged(final float swipeProgress) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onLockscreenSlideOffsetChanged(swipeProgress);
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * External views provided by a
     * {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService} can be either
     * interactive or non-interactive.
     *
     * <p>A non-interactive component does not receive any input events and functions similar to a
     * live wallpaper.</p>
     *
     * <p>An interactive component can receive input events and allows the user to interact with it
     * when the notification panel is not being displayed on top of the external view.</p>
     *
     * @return True if the current external view is interactive.
     */
    public boolean isInteractive() {
        return mIsInteractive;
    }

    /**
     * Registers a {@link cyanogenmod.externalviews.KeyguardExternalView.KeyguardExternalViewCallbacks}
     * for receiving events from the
     * {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     * @param callback The callback to register
     */
    public void registerKeyguardExternalViewCallback(KeyguardExternalViewCallbacks callback) {
        mCallback = callback;
    }

    /**
     * Unregister a previously registered
     * {@link cyanogenmod.externalviews.KeyguardExternalView.KeyguardExternalViewCallbacks}
     * @param callback The callback to unregister
     */
    public void unregisterKeyguardExternalViewCallback(KeyguardExternalViewCallbacks callback) {
        if (mCallback != callback) {
            throw new IllegalArgumentException("Callback not registered");
        }
        mCallback = null;
    }

    /**
     * Registers a {@link cyanogenmod.externalviews.KeyguardExternalView.OnWindowAttachmentChangedListener}
     * for receiving events from the
     * {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     * @param listener The callback to register
     *
     * @hide
     */
    public void registerOnWindowAttachmentChangedListener(
            OnWindowAttachmentChangedListener listener) {
        mWindowAttachmentListener = listener;
    }

    /**
     * Unregister a previously registered
     * {@link cyanogenmod.externalviews.KeyguardExternalView.OnWindowAttachmentChangedListener}
     * @param listener The callback to unregister
     *
     * @hide
     */
    public void unregisterOnWindowAttachmentChangedListener(
            OnWindowAttachmentChangedListener listener) {
        if (mWindowAttachmentListener != listener) {
            throw new IllegalArgumentException("Callback not registered");
        }
        mWindowAttachmentListener = null;
    }

    /**
     * Callback interface for a {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}
     * to send events to the host's registered
     * {@link cyanogenmod.externalviews.KeyguardExternalView.KeyguardExternalViewCallbacks}
     */
    public interface KeyguardExternalViewCallbacks {
        boolean requestDismiss();
        boolean requestDismissAndStartActivity(Intent intent);
        void providerDied();
        void slideLockscreenIn();
    }

    /**
     * Callback interface for changes to the containing window being attached and detached from the
     * window manager.
     * @hide
     */
    public interface OnWindowAttachmentChangedListener {
        void onAttachedToWindow();
        void onDetachedFromWindow();
    }
}
