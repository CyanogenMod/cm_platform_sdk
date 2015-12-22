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
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.LinkedList;

/**
 * TODO: unhide once documented and finalized
 * @hide
 */
public class KeyguardExternalView extends View implements Application.ActivityLifecycleCallbacks,
        ViewTreeObserver.OnPreDrawListener, IBinder.DeathRecipient {

    public static final String EXTRA_PERMISSION_LIST = "permissions_list";
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

    public KeyguardExternalView(Context context, AttributeSet attrs) {
        this(context, attrs, null);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attributeSet, ComponentName componentName) {
        super(context, attributeSet);
        mContext = getContext();
        mExternalViewProperties = new ExternalViewProperties(this, mContext);
        Application app = (mContext instanceof Activity) ? ((Activity) mContext).getApplication()
                : (Application) mContext;
        app.registerActivityLifecycleCallbacks(this);
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
                mExternalViewProvider = IKeyguardExternalViewProvider.Stub.asInterface(
                        IExternalViewProviderFactory.Stub.asInterface(service).
                                createExternalView(null));
                mExternalViewProvider.registerCallback(
                        KeyguardExternalView.this.mKeyguardExternalViewCallbacks);
                mService = service;
                mService.linkToDeath(KeyguardExternalView.this, 0);
                executeQueue();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                mExternalViewProvider.unregisterCallback(
                        KeyguardExternalView.this.mKeyguardExternalViewCallbacks);
            } catch (RemoteException e) {
            }
            mExternalViewProvider = null;
            mService.unlinkToDeath(KeyguardExternalView.this, 0);
            mService = null;
        }
    };

    private final IKeyguardExternalViewCallbacks mKeyguardExternalViewCallbacks =
            new IKeyguardExternalViewCallbacks.Stub() {
        @Override
        public void dismiss() throws RemoteException {
            if (mCallback != null) {
                mCallback.dismiss();
            }
        }

        @Override
        public void dismissAndStartActivity(Intent intent) throws RemoteException {
            if (mCallback != null) {
                mCallback.dismissAndStartActivity(intent);
            }
        }

        @Override
        public void collapseNotificationPanel() throws RemoteException {
            if (mCallback != null) {
                mCallback.collapseNotificationPanel();
            }
        }

        @Override
        public void setInteractivity(boolean isInteractive) {
            mIsInteractive = isInteractive;
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

    // Activity lifecycle callbacks

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onStart();
                } catch (RemoteException e) {
                }
            }
        });
    }

    @Override
    public void onActivityResumed(Activity activity) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onResume();
                } catch (RemoteException e) {
                }
                getViewTreeObserver().addOnPreDrawListener(KeyguardExternalView.this);
            }
        });
    }

    @Override
    public void onActivityPaused(Activity activity) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onPause();
                } catch (RemoteException e) {
                }
                getViewTreeObserver().removeOnPreDrawListener(KeyguardExternalView.this);
            }
        });
    }

    @Override
    public void onActivityStopped(Activity activity) {
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.onStop();
                } catch (RemoteException e) {
                }
            }
        });
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mExternalViewProvider = null;
        mContext.unbindService(mServiceConnection);
    }

    // Placeholder callbacks

    @Override
    public void onDetachedFromWindow() {
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
     * Sets the component of the ExternalViewProviderService to be used for this ExternalView.
     * If a provider is already connected to this view, it is first unbound before binding to the
     * new provider.
     * @param componentName
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

    public boolean isInteractive() {
        return mIsInteractive;
    }

    public void registerKeyguardExternalViewCallback(KeyguardExternalViewCallbacks callback) {
        mCallback = callback;
    }

    public void unregisterKeyguardExternalViewCallback(KeyguardExternalViewCallbacks callback) {
        if (mCallback != callback) {
            throw new IllegalArgumentException("Callback not registered");
        }
        mCallback = null;
    }

    public interface KeyguardExternalViewCallbacks {
        public void dismiss();
        public void dismissAndStartActivity(Intent intent);
        public void collapseNotificationPanel();
        public void providerDied();
    }
}
