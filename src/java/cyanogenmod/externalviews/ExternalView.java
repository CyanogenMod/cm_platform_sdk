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
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.LinkedList;

/**
 * TODO: unhide once documented and finalized
 * @hide
 */
public class ExternalView extends View implements Application.ActivityLifecycleCallbacks,
        ViewTreeObserver.OnPreDrawListener {

    private Context mContext;
    private LinkedList<Runnable> mQueue = new LinkedList<Runnable>();
    private volatile IExternalViewProvider mExternalViewProvider;
    private final ExternalViewProperties mExternalViewProperties;

    public ExternalView(Context context, AttributeSet attrs) {
        this(context, attrs, null);
    }

    public ExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public ExternalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs);
    }

    public ExternalView(Context context, AttributeSet attributeSet, ComponentName componentName) {
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
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mExternalViewProvider = IExternalViewProvider.Stub.asInterface(
                        IExternalViewProviderFactory.Stub.asInterface(service).createExternalView(null));
                executeQueue();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mExternalViewProvider = null;
        }
    };

    private void executeQueue() {
        while (!mQueue.isEmpty()) {
            Runnable r = mQueue.pop();
            r.run();
        }
    }

    private void performAction(Runnable r) {
        if (mExternalViewProvider != null) {
            r.run();
        } else {
            mQueue.add(r);
        }
    }

    // view overrides, for positioning

    @Override
    public boolean onPreDraw() {
        long cur = System.currentTimeMillis();
        if (!mExternalViewProperties.hasChanged()) {
            return true;
        }
        final int x = mExternalViewProperties.getX();
        final int y = mExternalViewProperties.getY();
        final int width = mExternalViewProperties.getWidth();
        final int height = mExternalViewProperties.getHeight();
        final boolean visible = mExternalViewProperties.isVisible();
        final Rect clipRect = mExternalViewProperties.getHitRect();
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
                getViewTreeObserver().addOnPreDrawListener(ExternalView.this);
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
                getViewTreeObserver().removeOnPreDrawListener(ExternalView.this);
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
}
