/**
 * Copyright (c) 2015-2016, The CyanogenMod Project
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
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;

public class KeyguardExternalProviderTest extends ThreadServiceTestCase<ViewProviderService> {
    private WindowManager mWindowManagerMock;
    private IExternalViewProviderFactory mProvider;
    private IKeyguardExternalViewProvider mView;

    public KeyguardExternalProviderTest() {
        super(ViewProviderService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        IBinder bind = bindService((ServiceRunnable) null);
        assertNotNull (bind);

        mProvider = IExternalViewProviderFactory.Stub.asInterface(bind);
        assertNotNull (mProvider);

        final Bundle bundle = new Bundle();
        IBinder bindView = mProvider.createExternalView(bundle);
        mView = IKeyguardExternalViewProvider.Stub.asInterface(bindView);
        assertNotNull (mView);

        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService(), Mockito.times(1))
                        .createExternalView(Mockito.eq(bundle));
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .onCreateView();

                // Ensure the bundle we constructed with is intact
                Bundle b = getService().getProvider().getOptionsImpl();
                assertEquals (b, bundle);
            }
        });
    }

    @Override
    protected void setupService() {
        super.setupService();

        // Update the service instance with our spy so we can track it
        try {
            Field f = ServiceTestCase.class.getDeclaredField("mService");
            f.setAccessible(true);
            ViewProviderService woot = ViewProviderService.class.newInstance();
            ViewProviderService spy = Mockito.spy(woot);
            f.set(this, spy);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
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

    public void testCallbacks() throws Exception {
        Mockito.reset(getService().getProvider().getTracker());
        mView.onScreenTurnedOff();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .onScreenTurnedOff();
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        mView.onKeyguardDismissed();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .onKeyguardDismissed();
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        mView.onBouncerShowing(true);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .onBouncerShowing(Mockito.eq(true));
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        mView.onKeyguardShowing(true);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .onKeyguardShowing(Mockito.eq(true));
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        mView.onLockscreenSlideOffsetChanged(1f);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .onLockscreenSlideOffsetChanged(Mockito.eq(1f));
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        mView.onAttach(null);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1)).onAttach();
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());

                ArgumentCaptor<WindowManager.LayoutParams> params = ArgumentCaptor
                        .forClass(WindowManager.LayoutParams.class);
                ArgumentCaptor<ViewGroup> viewGroup = ArgumentCaptor
                        .forClass(ViewGroup.class);
                Mockito.verify(mWindowManagerMock, Mockito.times(1))
                        .addView(viewGroup.capture(), params.capture());

                ViewGroup decorView = viewGroup.getAllValues().get(0);
                assertEquals (decorView.getChildCount(), 2);
                assertEquals (decorView.getChildAt(1), getService().getProvider().getView());

                WindowManager.LayoutParams param = params.getAllValues().get(0);
                assertEquals ((param.type & WindowManager.LayoutParams.TYPE_KEYGUARD_PANEL),
                        WindowManager.LayoutParams.TYPE_KEYGUARD_PANEL);

                int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN;
                assertEquals(param.flags & flags, flags);

                assertEquals ((param.gravity & Gravity.LEFT | Gravity.TOP),
                        Gravity.LEFT | Gravity.TOP);
                assertEquals ((param.format & PixelFormat.TRANSPARENT),
                        PixelFormat.TRANSPARENT);
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        mView.onDetach();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1)).onDetach();
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());

                ArgumentCaptor<ViewGroup> viewGroup = ArgumentCaptor
                        .forClass(ViewGroup.class);
                Mockito.verify(mWindowManagerMock, Mockito.times(1))
                        .removeView(viewGroup.capture());

                ViewGroup decorView = viewGroup.getAllValues().get(0);
                assertEquals (decorView.getChildCount(), 2);
                assertEquals (decorView.getChildAt(1), getService().getProvider().getView());
            }
        });
    }

    public void testCallbackRegistration() throws Exception {
        final IKeyguardExternalViewCallbacks.Stub callback = MockIBinderStubForInterface
                .getMockInterface(IKeyguardExternalViewCallbacks.Stub.class);
        mView.registerCallback(callback);
        getService().getProvider().requestDismissImpl();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Mockito.verify(callback, Mockito.times(1)).requestDismiss();
                    Mockito.verify(callback, Mockito.times(1)).asBinder();
                } catch (RemoteException e) {
                    Assert.fail(e.getMessage());
                }
                Mockito.verifyNoMoreInteractions(callback);
            }
        });

        Mockito.reset(callback);
        final Intent i = new Intent();
        getService().getProvider().requestDismissAndStartActivityImpl(i);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Mockito.verify(callback, Mockito.times(1)).requestDismissAndStartActivity(Mockito.eq(i));
                } catch (RemoteException e) {
                    Assert.fail(e.getMessage());
                }
                Mockito.verifyNoMoreInteractions(callback);
            }
        });

        Mockito.reset(callback);
        getService().getProvider().setInteractivityImpl(true);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Mockito.verify(callback, Mockito.times(1)).setInteractivity(Mockito.eq(true));
                } catch (RemoteException e) {
                    Assert.fail(e.getMessage());
                }
                Mockito.verifyNoMoreInteractions(callback);
            }
        });

        Mockito.reset(callback);
        getService().getProvider().slideLockscreenInImpl();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Mockito.verify(callback, Mockito.times(1)).slideLockscreenIn();
                } catch (RemoteException e) {
                    Assert.fail(e.getMessage());
                }
                Mockito.verifyNoMoreInteractions(callback);
            }
        });

        Mockito.reset(getService().getProvider().getTracker());
        getService().getProvider().collapseNotificationPanelImpl();
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verify(getService().getProvider().getTracker(), Mockito.times(1))
                        .collapseNotificationPanelImpl();
                Mockito.verifyNoMoreInteractions(getService().getProvider().getTracker());
            }
        });
    }

    public void testAlterWindow() throws Exception {
        // Test visible false
        Mockito.reset(mWindowManagerMock);
        final Rect rect = new Rect(0, 0, 100, 100);
        mView.alterWindow(0, 0, 100, 100, false, rect);
        runOnServiceThread(new Runnable() {
            @Override
            public void run() {
                Mockito.verifyNoMoreInteractions(mWindowManagerMock);
            }
        });

        // Test visible true
        Mockito.reset(mWindowManagerMock);
        mView.alterWindow(10, 20, 30, 40, true, rect);
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
                // First view is actionbar
                View child = decorView.getChildAt(1);
                assertEquals (decorView.getChildCount(), 2);
                assertEquals (child, getService().getProvider().getView());
                assertEquals (decorView.getVisibility(), View.VISIBLE);
                assertEquals (decorView.getClipBounds(), rect);

                WindowManager.LayoutParams param = params.getAllValues().get(0);
                assertEquals (param.x, 10);
                assertEquals (param.y, 20);
                assertEquals (param.width, 30);
                assertEquals (param.height, 40);
                Mockito.verifyNoMoreInteractions(mWindowManagerMock);
            }
        });
    }
}
