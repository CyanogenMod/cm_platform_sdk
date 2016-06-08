package org.cyanogenmod.tests.externalviews.keyguardexternalviews;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.*;
import android.test.ServiceTestCase;
import android.view.*;
import android.widget.Space;
import android.widget.TextView;
import cyanogenmod.externalviews.IExternalViewProviderFactory;
import cyanogenmod.externalviews.IKeyguardExternalViewCallbacks;
import cyanogenmod.externalviews.IKeyguardExternalViewProvider;
import cyanogenmod.externalviews.KeyguardExternalViewProviderService;
import org.cyanogenmod.tests.common.MockIBinderStubForInterface;
import org.cyanogenmod.tests.common.ThreadServiceTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;

public class KeyguardExternalProviderTest extends ThreadServiceTestCase<KeyguardExternalProviderTest.ViewProviderService> {
    private WindowManager mWindowManagerMock;

    public KeyguardExternalProviderTest() {
        super(ViewProviderService.class);
    }

    public static class ViewProviderService extends KeyguardExternalViewProviderService {
        private ViewProvider mProvider;

        public ViewProviderService() {}

        @Override
        public KeyguardExternalViewProviderService.Provider createExternalView(Bundle options) {
            if (mProvider == null) {
                mProvider = Mockito.spy(new ViewProvider(options));
            }
            return mProvider;
        }

        @Override
        protected void attachBaseContext(Context base) {
            super.attachBaseContext(base);
        }

        public class ViewProvider extends KeyguardExternalViewProviderService.Provider {
            private ViewProvider mTracker;
            private View mView;

            public ViewProvider(Bundle options) {
                super(options);
            }
            @Override
            public View onCreateView() {
                if (mTracker == null) {
                    mTracker = Mockito.mock(ViewProvider.class);
                }
                // Will be null for mTracker, due to mImpl initialization flow
                    mTracker.onCreateView();
                if (mView == null) {
                    mView = new Space(getBaseContext());
                }
                return mView;
            }
            @Override
            public void onKeyguardShowing(boolean screenOn) {
                mTracker.onKeyguardShowing(screenOn);
            }
            @Override
            public void onKeyguardDismissed() {
                mTracker.onKeyguardDismissed();
            }
            @Override
            public void onBouncerShowing(boolean showing) {
                mTracker.onBouncerShowing(showing);
            }
            @Override
            public void onScreenTurnedOn() {
                mTracker.onScreenTurnedOn();
            }
            @Override
            public void onScreenTurnedOff() {
                mTracker.onScreenTurnedOff();
            }

            @Override
            protected void onAttach() {
                mTracker.onAttach();
            }

            @Override
            protected void onDetach() {
                mTracker.onDetach();
            }

            @Override
            protected void onLockscreenSlideOffsetChanged(float swipeProgress) {
                mTracker.onLockscreenSlideOffsetChanged(swipeProgress);
            }

            public boolean requestDismissImpl() {
                mTracker.requestDismiss();
                return requestDismiss();
            }

            public boolean requestDismissAndStartActivityImpl(Intent intent) {
                mTracker.requestDismissAndStartActivity(intent);
                return requestDismissAndStartActivity(intent);
            }

            public void setInteractivityImpl(boolean interactive) {
                mTracker.setInteractivity(interactive);
                setInteractivity(interactive);
            }

            public void slideLockscreenInImpl() {
                mTracker.slideLockscreenIn();
                slideLockscreenIn();
            }

            public Bundle getOptionsImpl() {
                mTracker.getOptions();
                return getOptions();
            }

            public void collapseNotificationPanelImpl() {
                mTracker.collapseNotificationPanel();
                collapseNotificationPanel();
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void setupService() {
        super.setupService();

        // Update the service instance with our spy so we can track it
        Field f = null;
        try {
            f = ServiceTestCase.class.getDeclaredField("mService");
            f.setAccessible(true);
            ViewProviderService woot = ViewProviderService.class.newInstance();
            ViewProviderService spy = Mockito.spy(woot);
            f.set(this, spy);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        // Setup mock context
        Context context = Mockito.mock(Context.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(getContext().getApplicationInfo()).when(context).getApplicationInfo();
        Mockito.doReturn(getContext().getResources()).when(context).getResources();
        Mockito.doReturn(getContext().getTheme()).when(context).getTheme();
        Mockito.doReturn(getContext().getPackageManager()).when(context).getPackageManager();

        Mockito.doReturn(1).when(context).checkCallingOrSelfPermission(Mockito.anyString());
        Mockito.doReturn(getContext().getMainLooper()).when(context).getMainLooper();
        // Setup mock window manager
        mWindowManagerMock = Mockito.mock(WindowManager.class);
        WindowManager actualWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Mockito.doReturn(mWindowManagerMock).when(context).getSystemService(Mockito.eq(Context.WINDOW_SERVICE));
        Mockito.doReturn(actualWindowManager.getDefaultDisplay()).when(mWindowManagerMock).getDefaultDisplay();
        Mockito.doReturn(getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .when(context).getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Attach our mock context to service
        getService().attach(
                context,
                null,               // ActivityThread not actually used in Service
                ViewProviderService.class.getName(),
                null,               // token not needed when not talking with the activity manager
                getApplication(),
                null                // mocked services don't talk with the activity manager
        );
    }

    public void testCreateViewOnBind() throws RemoteException, IllegalAccessException, InstantiationException {
        final ViewProviderService providerService = startService(true, null);
        assert (providerService != null);

        IBinder bind = providerService.onBind(new Intent());
        IExternalViewProviderFactory provider = IExternalViewProviderFactory.Stub.asInterface(bind);
        assert (provider != null);

        // Ensure on bind we were asked to create an external view
        final Bundle bundle = new Bundle();
        IBinder bindView = provider.createExternalView(bundle);
        IKeyguardExternalViewProvider view = IKeyguardExternalViewProvider.Stub.asInterface(bindView);
        assert (view != null);

        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService, Mockito.times(1)).createExternalView(Mockito.eq(bundle));
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onCreateView();
            }
        });
    }

    public void testCallbacks() throws RemoteException, IllegalAccessException, InstantiationException {
        final ViewProviderService providerService = startService(true, null);
        assert (providerService != null);

        IBinder bind = providerService.onBind(new Intent());
        final IExternalViewProviderFactory provider = IExternalViewProviderFactory.Stub.asInterface(bind);
        assert (provider != null);

        // Ensure on bind we were asked to create an external view
        final Bundle bundle = new Bundle();
        IBinder bindView = provider.createExternalView(bundle);
        final IKeyguardExternalViewProvider view = IKeyguardExternalViewProvider.Stub.asInterface(bindView);
        assert (view != null);

//        ViewProviderService.ViewProvider blah = providerService.new ViewProvider(bundle);
//        Bundle b = blah.getOptionsImpl();
//        assert (b == bundle);

        Mockito.reset(providerService.mProvider.mTracker);
        view.onScreenTurnedOff();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onScreenTurnedOff();
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);
            }
        });

        Mockito.reset(providerService.mProvider.mTracker);
        view.onKeyguardDismissed();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onKeyguardDismissed();
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);
            }
        });

        Mockito.reset(providerService.mProvider.mTracker);
        view.onBouncerShowing(true);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onBouncerShowing(Mockito.eq(true));
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);
            }
        });

        Mockito.reset(providerService.mProvider.mTracker);
        view.onKeyguardShowing(true);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onKeyguardShowing(Mockito.eq(true));
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);
            }
        });

        Mockito.reset(providerService.mProvider.mTracker);
        view.onLockscreenSlideOffsetChanged(1f);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onLockscreenSlideOffsetChanged(Mockito.eq(1f));
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);
            }
        });

        Mockito.reset(providerService.mProvider.mTracker);
        view.onAttach(null);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onAttach();
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);

                ArgumentCaptor<WindowManager.LayoutParams> params = ArgumentCaptor
                        .forClass(WindowManager.LayoutParams.class);
                ArgumentCaptor<ViewGroup> viewGroup = ArgumentCaptor
                        .forClass(ViewGroup.class);
                Mockito.verify(mWindowManagerMock, Mockito.times(1))
                        .addView(viewGroup.capture(), params.capture());

                ViewGroup decorView = viewGroup.getAllValues().get(0);
                assert (decorView.getChildCount() == 1);
                assert (decorView.getChildAt(0) == providerService.mProvider.mView);

                WindowManager.LayoutParams param = params.getAllValues().get(0);
                assert ((param.type & WindowManager.LayoutParams.TYPE_KEYGUARD_PANEL) != 0);

                int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN;
                assert ((param.flags & flags) != 0);

                assert ((param.gravity & Gravity.LEFT | Gravity.TOP) != 0);
                assert ((param.format & PixelFormat.TRANSPARENT) != 0);
            }
        });

        Mockito.reset(providerService.mProvider.mTracker);
        view.onDetach();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(providerService.mProvider.mTracker, Mockito.times(1)).onDetach();
                Mockito.verifyNoMoreInteractions(providerService.mProvider.mTracker);

                ArgumentCaptor<ViewGroup> viewGroup = ArgumentCaptor
                        .forClass(ViewGroup.class);
                Mockito.verify(mWindowManagerMock, Mockito.times(1))
                        .removeView(viewGroup.capture());

                ViewGroup decorView = viewGroup.getAllValues().get(0);
                assert (decorView.getChildCount() == 1);
                assert (decorView.getChildAt(0) == providerService.mProvider.mView);
            }
        });
    }

    public void testCallbackRegistration() throws RemoteException {
        final ViewProviderService providerService = startService(true, null);
        assert (providerService != null);

        IBinder bind = providerService.onBind(new Intent());
        final IExternalViewProviderFactory provider = IExternalViewProviderFactory.Stub.asInterface(bind);
        assert (provider != null);

        // Ensure on bind we were asked to create an external view
        final Bundle bundle = new Bundle();
        IBinder bindView = provider.createExternalView(bundle);
        final IKeyguardExternalViewProvider view = IKeyguardExternalViewProvider.Stub.asInterface(bindView);
        assert (view != null);

//        final IKeyguardExternalViewCallbacks.Stub callback = MockIBinderStubForInterface.getMockInterface(IKeyguardExternalViewCallbacks.Stub.class);
//        view.registerCallback(callback);
//        providerService.mProvider.requestDismissImpl();
//        runOnServiceThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Mockito.verify(callback, Mockito.times(1)).requestDismiss();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//                Mockito.verifyNoMoreInteractions(callback);
//            }
//        });
//
//        Mockito.reset(callback);
//        final Intent i = new Intent();
//        providerService.mProvider.requestDismissAndStartActivityImpl(i);
//        runOnServiceThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Mockito.verify(callback, Mockito.times(1)).requestDismissAndStartActivity(Mockito.eq(i));
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//                Mockito.verifyNoMoreInteractions(callback);
//            }
//        });
//
//        Mockito.reset(callback);
//        providerService.mProvider.setInteractivityImpl(true);
//        runOnServiceThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Mockito.verify(callback, Mockito.times(1)).setInteractivity(Mockito.eq(true));
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//                Mockito.verifyNoMoreInteractions(callback);
//            }
//        });
//
//        Mockito.reset(callback);
//        providerService.mProvider.slideLockscreenInImpl();
//        runOnServiceThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Mockito.verify(callback, Mockito.times(1)).slideLockscreenIn();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//                Mockito.verifyNoMoreInteractions(callback);
//            }
//        });
//
//        Mockito.reset(callback);
//        providerService.mProvider.collapseNotificationPanelImpl();
//        runOnServiceThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Mockito.verify(callback, Mockito.times(1)).collapseNotificationPanel();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//                Mockito.verifyNoMoreInteractions(callback);
//            }
//        });
    }

    public void testAlterWindow() throws RemoteException {
        final ViewProviderService providerService = startService(true, null);
        assert (providerService != null);

        IBinder bind = providerService.onBind(new Intent());
        final IExternalViewProviderFactory provider = IExternalViewProviderFactory.Stub.asInterface(bind);
        assert (provider != null);

        // Ensure on bind we were asked to create an external view
        final Bundle bundle = new Bundle();
        IBinder bindView = provider.createExternalView(bundle);
        final IKeyguardExternalViewProvider view = IKeyguardExternalViewProvider.Stub.asInterface(bindView);
        assert (view != null);

        // Test visible false
        Mockito.reset(mWindowManagerMock);
        final Rect rect = new Rect(0, 0, 100, 100);
        view.alterWindow(0, 0, 100, 100, false, rect);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verifyNoMoreInteractions(mWindowManagerMock);
            }
        });

        // Test visible true
        Mockito.reset(mWindowManagerMock);
        view.alterWindow(10, 20, 30, 40, true, rect);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<WindowManager.LayoutParams> params = ArgumentCaptor
                        .forClass(WindowManager.LayoutParams.class);
                ArgumentCaptor<ViewGroup> viewGroup = ArgumentCaptor
                        .forClass(ViewGroup.class);
                Mockito.verify(mWindowManagerMock, Mockito.times(1))
                        .updateViewLayout(viewGroup.capture(), params.capture());

                ViewGroup decorView = viewGroup.getAllValues().get(0);
                View child = decorView.getChildAt(0);
                assert (decorView.getChildCount() == 1);
                assert (child == providerService.mProvider.mView);
                assert (child.getVisibility() == View.VISIBLE);
                assert (child.getClipBounds().equals(rect));

                WindowManager.LayoutParams param = params.getAllValues().get(0);
                assert (param.x == 10);
                assert (param.y == 20);
                assert (param.width == 30);
                assert (param.height == 40);
                Mockito.verifyNoMoreInteractions(mWindowManagerMock);
            }
        });
    }
}
