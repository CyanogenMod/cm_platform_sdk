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

package org.cyanogenmod.tests.common;

import android.os.IBinder;
import android.os.IInterface;
import org.mockito.Mockito;

/**
 * Helper class to mock stubs for IInterfaces
 * Ensures that when querying the local interface
 * we return the instance itself as to preserve the mock instance
 */
public final class MockIBinderStubForInterface {
    private MockIBinderStubForInterface() {}

    private static <T extends IBinder> String getStubDescriptor(Class<T> stubClass) {
        try {
            T instance = stubClass.newInstance();
            return instance.getInterfaceDescriptor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T extends IBinder> T getMockInterface(Class<T> stub) {
        String descriptor = getStubDescriptor(stub);
        T mockInterface = Mockito.mock(stub);
        Mockito.doReturn(mockInterface)
                .when(mockInterface)
                .queryLocalInterface(descriptor == null ?
                        Mockito.anyString() : Mockito.eq(descriptor));
        return mockInterface;
    }
}
