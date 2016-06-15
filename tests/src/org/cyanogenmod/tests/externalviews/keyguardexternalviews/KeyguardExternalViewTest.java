/**
 * Copyright (c) 2016, The CyanogenMod Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.AndroidTestCase;
import android.view.WindowManager;
import cyanogenmod.externalviews.IExternalViewProviderFactory;
import cyanogenmod.externalviews.IKeyguardExternalViewCallbacks;
import cyanogenmod.externalviews.IKeyguardExternalViewProvider;
import cyanogenmod.externalviews.KeyguardExternalView;
import org.cyanogenmod.tests.common.MockIBinderStubForInterface;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class KeyguardExternalViewTest extends AndroidTestCase {
    private IKeyguardExternalViewProvider.Stub mIKeyguardExternalViewProvider;
    private IExternalViewProviderFactory.Stub mExternalViewProviderFactory;
    private WindowManager mWindowManagerMock;
    private Context mContextMock;
    private ServiceConnection mServiceConnection;
    private IKeyguardExternalViewCallbacks mKeyguardCallback;
    private KeyguardExternalView mExternalView;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        WindowManager windowManager = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        // Ensure we mock context but invoke non intercepted calls to impl
        mContextMock = Mockito.mock(Context.class, Mockito.CALLS_REAL_METHODS);

        // Needed since ExternalView's base class instantiates things off this.
        // We can't use a spy here since ContextImpl is hidden (PowerMock ?)
        // For now just redirect these to the test context
        Mockito.doReturn(getContext().getApplicationInfo()).when(mContextMock).getApplicationInfo();
        Mockito.doReturn(getContext().getResources()).when(mContextMock).getResources();
        Mockito.doReturn(getContext().getTheme()).when(mContextMock).getTheme();

        // Mock window manager to ensure we don't try to add the windows
        mWindowManagerMock = Mockito.mock(WindowManager.class);
        Mockito.doReturn(mWindowManagerMock).when(mContextMock).getSystemService(Context.WINDOW_SERVICE);
        Mockito.doReturn(windowManager.getDefaultDisplay()).when(mWindowManagerMock).getDefaultDisplay();

        // Mock the viewProvider/KeyguardView to keep track of callback invocations
        mIKeyguardExternalViewProvider = MockIBinderStubForInterface
                .getMockInterface(IKeyguardExternalViewProvider.Stub.class);
        mExternalViewProviderFactory = MockIBinderStubForInterface
                .getMockInterface(IExternalViewProviderFactory.Stub.class);

        // Ensure we return our view provider when the factory is asked to create external view
        Mockito.doReturn(mIKeyguardExternalViewProvider)
                .when(mExternalViewProviderFactory)
                .createExternalView(Mockito.any(Bundle.class));

        // Store the callback object registered by the view
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                mKeyguardCallback = (IKeyguardExternalViewCallbacks) invocation.getArguments()[0];
                return null;
            }
        }).when(mIKeyguardExternalViewProvider)
                .registerCallback(Mockito.notNull(IKeyguardExternalViewCallbacks.class));

        // Simulate bound service connection when bindService is invoked
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceConnection connection = (ServiceConnection) invocation.getArguments()[1];
                connection.onServiceConnected(null, mExternalViewProviderFactory);
                mServiceConnection = connection;
                return true;
            }
        }).when(mContextMock).bindService(Mockito.any(Intent.class),
                Mockito.any(ServiceConnection.class), Mockito.anyInt());
    }

    public void testValidServiceBind() {
        mExternalView = new KeyguardExternalView(mContextMock, null, new ComponentName("", ""));

        // Ensure we attempted to bind to the service
        Mockito.verify(mContextMock, Mockito.times(1)).bindService(Mockito.any(Intent.class),
                Mockito.any(ServiceConnection.class), Mockito.anyInt());
    }

    public void testInvalidServiceBind() {
        mExternalView = new KeyguardExternalView(mContextMock, null, null);
        // Ensure we did not attempt to bind to the service
        Mockito.verify(mContextMock, Mockito.never()).bindService(Mockito.any(Intent.class),
                Mockito.any(ServiceConnection.class), Mockito.anyInt());
    }

    public void testServiceAndCallbacksRegistered() throws RemoteException {
        testValidServiceBind();

        // Ensure a view was asked to be created
        Mockito.verify(mExternalViewProviderFactory, Mockito.times(1))
                .createExternalView(Mockito.any(Bundle.class));

        // Ensure callbacks were registered
        Mockito.verify(mIKeyguardExternalViewProvider, Mockito.times(1))
                .registerCallback(Mockito.notNull(IKeyguardExternalViewCallbacks.class));

        assertNotNull(mKeyguardCallback);
    }

    public void testServiceUnbindAndCallbacksUnRegistered() throws RemoteException {
        testServiceAndCallbacksRegistered();

        assertNotNull(mServiceConnection);
        mServiceConnection.onServiceDisconnected(null);

        // Ensure callbacks were registered
        Mockito.verify(mIKeyguardExternalViewProvider, Mockito.times(1))
                .unregisterCallback(Mockito.notNull(IKeyguardExternalViewCallbacks.class));
    }

    // Ensure provider is alerted view callbacks
    public void testViewProviderCallbacks() throws RemoteException {
        testServiceAndCallbacksRegistered();

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onKeyguardShowing(true);
        Mockito.verify(mIKeyguardExternalViewProvider,
                Mockito.times(1)).onKeyguardShowing(Mockito.anyBoolean());
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onAttachedToWindow();
        Mockito.verify(mIKeyguardExternalViewProvider,
                Mockito.times(1)).onAttach(Mockito.any(IBinder.class));
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onDetachedFromWindow();
        Mockito.verify(mIKeyguardExternalViewProvider, Mockito.times(1)).onDetach();
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onBouncerShowing(true);
        Mockito.verify(mIKeyguardExternalViewProvider,
                Mockito.times(1)).onBouncerShowing(Mockito.anyBoolean());
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onKeyguardDismissed();
        Mockito.verify(mIKeyguardExternalViewProvider,
                Mockito.times(1)).onKeyguardDismissed();
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onLockscreenSlideOffsetChanged(1f);
        Mockito.verify(mIKeyguardExternalViewProvider,
                Mockito.times(1)).onLockscreenSlideOffsetChanged(Mockito.eq(1f));
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onScreenTurnedOff();
        Mockito.verify(mIKeyguardExternalViewProvider, Mockito.times(1)).onScreenTurnedOff();
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);

        Mockito.reset(mIKeyguardExternalViewProvider);
        mExternalView.onScreenTurnedOn();
        Mockito.verify(mIKeyguardExternalViewProvider, Mockito.times(1)).onScreenTurnedOn();
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);
    }

    public void testWindowMovement() throws RemoteException {
        testServiceAndCallbacksRegistered();
        Mockito.reset(mIKeyguardExternalViewProvider);

        mExternalView.setLeft(0);
        mExternalView.setTop(0);
        mExternalView.setRight(100);
        mExternalView.setBottom(100);

        mExternalView.onPreDraw();
        Mockito.verify(mIKeyguardExternalViewProvider, Mockito.times(1))
                .alterWindow(Mockito.eq(0), Mockito.eq(0), Mockito.anyInt(),
                        Mockito.anyInt(), Mockito.eq(true), Mockito.any(Rect.class));
        Mockito.verifyNoMoreInteractions(mIKeyguardExternalViewProvider);
    }

    public void testWindowAttachmentCallbacks() throws RemoteException {
        testServiceAndCallbacksRegistered();

        KeyguardExternalView.OnWindowAttachmentChangedListener callback =
                Mockito.mock(KeyguardExternalView.OnWindowAttachmentChangedListener.class);
        mExternalView.registerOnWindowAttachmentChangedListener(callback);

        mKeyguardCallback.onAttachedToWindow();
        Mockito.verify(callback, Mockito.times(1)).onAttachedToWindow();
        Mockito.verifyNoMoreInteractions(callback);

        mKeyguardCallback.onDetachedFromWindow();
        Mockito.verify(callback, Mockito.times(1)).onDetachedFromWindow();
        Mockito.verifyNoMoreInteractions(callback);
    }

    public void testKeyguardViewCallbacks() throws RemoteException {
        testServiceAndCallbacksRegistered();

        KeyguardExternalView.KeyguardExternalViewCallbacks callback = Mockito.mock(
                KeyguardExternalView.KeyguardExternalViewCallbacks.class);
        mExternalView.registerKeyguardExternalViewCallback(callback);

        mKeyguardCallback.requestDismiss();
        Mockito.verify(callback, Mockito.times(1)).requestDismiss();
        Mockito.verifyNoMoreInteractions(callback);

        Intent i = new Intent();
        mKeyguardCallback.requestDismissAndStartActivity(i);
        Mockito.verify(callback, Mockito.times(1))
                .requestDismissAndStartActivity(Mockito.eq(i));
        Mockito.verifyNoMoreInteractions(callback);

        mKeyguardCallback.setInteractivity(true);
        assertEquals(mExternalView.isInteractive(), true);
        Mockito.verifyNoMoreInteractions(callback);

        mKeyguardCallback.slideLockscreenIn();
        Mockito.verify(callback, Mockito.times(1)).slideLockscreenIn();
        Mockito.verifyNoMoreInteractions(callback);

        mExternalView.binderDied();
        Mockito.verify(callback, Mockito.times(1)).providerDied();
        Mockito.verifyNoMoreInteractions(callback);
    }
}
