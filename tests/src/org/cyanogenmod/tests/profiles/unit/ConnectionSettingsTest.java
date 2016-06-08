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
package org.cyanogenmod.tests.profiles.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.profiles.ConnectionSettings;

public class ConnectionSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testConstructManually() {
        ConnectionSettings connectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_GPS);
        assertEquals(ConnectionSettings.PROFILE_CONNECTION_GPS,
                connectionSettings.getConnectionId());
        assertNotNull(connectionSettings);
    }

    @SmallTest
    public void testConstructWholly() {
        ConnectionSettings connectionSettings =
                new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_GPS,
                        ConnectionSettings.BooleanState.STATE_DISALED, true);
        assertEquals(true, connectionSettings.isOverride());
        assertEquals(ConnectionSettings.BooleanState.STATE_DISALED,
                connectionSettings.getValue());
        assertEquals(ConnectionSettings.PROFILE_CONNECTION_GPS,
                connectionSettings.getConnectionId());
        assertNotNull(connectionSettings);
    }

    @SmallTest
    public void testVerifyOverride() {
        ConnectionSettings connectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_GPS);
        connectionSettings.setOverride(true);
        assertEquals(true, connectionSettings.isOverride());
    }

    @SmallTest
    public void testVerifySubId() {
        int expectedSubId = 2;
        ConnectionSettings connectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_2G3G4G);
        connectionSettings.setSubId(expectedSubId);
        assertEquals(expectedSubId, connectionSettings.getSubId());
    }

    @SmallTest
    public void testVerifyValue() {
        int expectedValue = ConnectionSettings.BooleanState.STATE_DISALED;
        ConnectionSettings connectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_2G3G4G);
        connectionSettings.setValue(expectedValue);
        assertEquals(expectedValue, connectionSettings.getValue());
    }
}
