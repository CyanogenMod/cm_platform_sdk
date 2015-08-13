package cyanogenmod.externalviews;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import cyanogenmod.platform.R;
import java.util.LinkedList;

public class ExternalView extends View implements ViewTreeObserver.OnScrollChangedListener,
        Application.ActivityLifecycleCallbacks {

    private static final String sAttributeNameSpace =
            "http://schemas.android.com/apk/lib/cyanogenmod.platform";

    private Activity mActivity;
    private final ComponentName mExtensionComponent;
    private boolean mBound;
    private LinkedList<Runnable> mQueue = new LinkedList<Runnable>();
    private IExternalViewProvider mExternalViewProvider;

    private static ComponentName getComponentFromAttribute(Context context, AttributeSet attrs) {
        String componentString = attrs.getAttributeValue(sAttributeNameSpace, "componentName");
        return ComponentName.unflattenFromString(componentString);
    }

    public ExternalView(Context context, AttributeSet attrs) {
        this(context, getComponentFromAttribute(context, attrs));
    }

    public ExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public ExternalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs);
    }

    public ExternalView(Context context, ComponentName componentName) {
        super(context);
        mActivity = (Activity) getContext();
        mExtensionComponent = componentName;
        mActivity.getApplication().registerActivityLifecycleCallbacks(this);
        getViewTreeObserver().addOnScrollChangedListener(this);
        bind();
    }

    private void bind() {
        if (mBound) {
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(mExtensionComponent);
        mActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            try {
                mExternalViewProvider = IExternalViewProvider.Stub.asInterface(
                        IExternalViewProviderFactory.Stub.asInterface(service).createExternalView(null));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            executeQueue();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mExternalViewProvider = null;
        }
    };

    private void executeQueue() {
        while (!mQueue.isEmpty()) {
            Runnable r = mQueue.pop();
            r.run();
        }
    }

    // Activity lifecycle callbacks

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    private void performAction(Runnable r) {
        if (mBound) {
            r.run();
        } else {
            mQueue.add(r);
        }
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
        unbindExtension();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int[] screenCords = new int[2];
        getLocationOnScreen(screenCords);
        final Rect hitRect = new Rect();
        mActivity.getWindow().getDecorView().getHitRect(hitRect);
        final int x = screenCords[0];
        final int y = screenCords[1];
        final int width = getWidth();
        final int height = getHeight();
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.alterWindow(x, y, width, height,
                            getLocalVisibleRect(hitRect));
                } catch (RemoteException e) {
                }
            }
        });
    }

    @Override
    public void onScrollChanged() {
        int[] screenCords = new int[2];
        getLocationOnScreen(screenCords);
        final Rect hitRect = new Rect();
        mActivity.getWindow().getDecorView().getHitRect(hitRect);
        final int x = screenCords[0];
        final int y = screenCords[1];
        final int width = getWidth();
        final int height = getHeight();
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.alterWindow(x, y, width, height,
                            getLocalVisibleRect(hitRect));
                } catch (RemoteException e) {
                }
            }
        });
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
        unbindExtension();
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

    private void unbindExtension() {
        if (mBound) {
            mBound = false;
            mActivity.unbindService(mServiceConnection);
        }
    }
}
