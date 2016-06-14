/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.cyanogenmod.tests.livelockscreen.unit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.test.AndroidTestCase;
import android.test.mock.MockPackageManager;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ILiveLockScreenManager;
import cyanogenmod.app.LiveLockScreenInfo;
import cyanogenmod.app.LiveLockScreenManager;
import org.cyanogenmod.tests.common.MockIBinderStubForInterface;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;

public class LiveLockScreenManagerTest extends AndroidTestCase {
    private LiveLockScreenManager mManager;
    private ILiveLockScreenManager mManagerInterface;
    private Context mMockContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PackageManager packageManager = Mockito.mock(MockPackageManager.class);
        Mockito.doReturn(true).when(packageManager).hasSystemFeature(
                CMContextConstants.Features.LIVE_LOCK_SCREEN);

        // Something else is initializing the manager
        Field f = LiveLockScreenManager.class.getDeclaredField("sInstance");
        f.setAccessible(true);
        f.set(null, null);

        mMockContext = Mockito.mock(Context.class);
        Mockito.doReturn(packageManager).when(mMockContext).getPackageManager();
        mManager = LiveLockScreenManager.getInstance(mMockContext);
        f = LiveLockScreenManager.class.getDeclaredField("sService");
        f.setAccessible(true);

        mManagerInterface = MockIBinderStubForInterface
                .getMockInterface(ILiveLockScreenManager.Stub.class);
        f.set(mManager, mManagerInterface);

        Mockito.verify(mMockContext, Mockito.times(1)).getPackageManager();
        Mockito.verify(packageManager, Mockito.times(1)).hasSystemFeature(
                Mockito.eq(CMContextConstants.Features.LIVE_LOCK_SCREEN));
        assertNotNull (mManager);
    }

    public void testGetDefaultLiveLockScreen() throws RemoteException {
        mManager.getDefaultLiveLockScreen();

        Mockito.verify(mManagerInterface, Mockito.times(1))
                .getDefaultLiveLockScreen();
        Mockito.verifyNoMoreInteractions(mManagerInterface);
    }

    public void testSetDefaultLiveLockScreen() throws RemoteException {
        LiveLockScreenInfo liveLockScreenInfo = Mockito.mock(LiveLockScreenInfo.class);
        mManager.setDefaultLiveLockScreen(liveLockScreenInfo);

        Mockito.verify(mManagerInterface, Mockito.times(1))
                .setDefaultLiveLockScreen(Mockito.eq(liveLockScreenInfo));
        Mockito.verifyNoMoreInteractions(mManagerInterface);
    }

    private boolean testEnqueue(boolean success) throws Exception {
        String testPackage = "com.testpackage";
        Mockito.doReturn(testPackage).when(mMockContext).getPackageName();

        if (!success) {
            Mockito.doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    int[] idOut = (int[]) invocation.getArguments()[3];
                    idOut[0] = 1;
                    return invocation;
                }
            }).when(mManagerInterface).enqueueLiveLockScreen(Mockito.anyString(),
                    Mockito.anyInt(), Mockito.any(LiveLockScreenInfo.class),
                    Mockito.any(int[].class), Mockito.anyInt());
        }

        LiveLockScreenInfo liveLockScreenInfo = Mockito.mock(LiveLockScreenInfo.class);
        boolean result = mManager.show(0, liveLockScreenInfo);

        Mockito.verify(mManagerInterface, Mockito.times(1)).enqueueLiveLockScreen(Mockito.eq(testPackage),
                Mockito.eq(0), Mockito.eq(liveLockScreenInfo), Mockito.any(int[].class),
                Mockito.eq(UserHandle.myUserId()));
        Mockito.verifyNoMoreInteractions(mManagerInterface);

        return result;
    }

    public void testShowLockscreen() throws Exception {
        assertEquals(testEnqueue(true), true);
        assertEquals(testEnqueue(false), false);
    }

    public void testCancel() throws Exception {
        String testPackage = "com.testpackage";
        Mockito.doReturn(testPackage).when(mMockContext).getPackageName();

        LiveLockScreenInfo liveLockScreenInfo = Mockito.mock(LiveLockScreenInfo.class);
        mManager.cancel(0);

        Mockito.verify(mManagerInterface, Mockito.times(1)).cancelLiveLockScreen(
                Mockito.eq(testPackage), Mockito.eq(0), Mockito.eq(UserHandle.myUserId()));
        Mockito.verifyNoMoreInteractions(mManagerInterface);
    }
}
