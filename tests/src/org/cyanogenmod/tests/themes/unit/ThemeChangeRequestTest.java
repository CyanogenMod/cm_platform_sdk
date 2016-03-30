/**
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
package org.cyanogenmod.tests.themes.unit;

import android.os.Parcel;
import android.test.AndroidTestCase;

import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.themes.ThemeChangeRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Excercise the parceling of the ThemeChangeRequest object and builder.
 */
public class ThemeChangeRequestTest extends AndroidTestCase {

    @SmallTest
    public void testGetAlarmThemePackageNameUnravelFromParcel() {
        String expectedAlarmPackage = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setAlarm(expectedAlarmPackage)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedAlarmPackage, fromParcel.getAlarmThemePackageName());
    }

    @SmallTest
    public void testGetBootanimationThemePackageNameUnravelFromParcel() {
        String expectedBootAnimationThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setBootanimation(expectedBootAnimationThemePackageName)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedBootAnimationThemePackageName,
                fromParcel.getBootanimationThemePackageName());
    }

    @SmallTest
    public void testGetFontThemePackageNameUnravelFromParcel() {
        String expectedFontThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setFont(expectedFontThemePackageName)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedFontThemePackageName,
                fromParcel.getFontThemePackageName());
    }

    @SmallTest
    public void testGetIconsThemePackageNameUnravelFromParcel() {
        String expectedIconThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setIcons(expectedIconThemePackageName)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedIconThemePackageName,
                fromParcel.getIconsThemePackageName());
    }

    @SmallTest
    public void testGetLiveLockScreenThemePackageNameUnravelFromParcel() {
        String expectedLiveLockscreenThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setLiveLockScreen(expectedLiveLockscreenThemePN)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedLiveLockscreenThemePN,
                fromParcel.getLiveLockScreenThemePackageName());
    }

    @SmallTest
    public void testGetLockWallpaperThemePackageNameUnravelFromParcel() {
        String expectedLockWallpaperThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setLockWallpaper(expectedLockWallpaperThemePN)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(request);
        assertEquals(expectedLockWallpaperThemePN,
                fromParcel.getLockWallpaperThemePackageName());
    }

    @SmallTest
    public void testGetNavBarThemePackageNameUnravelFromParcel() {
        String expectedNavBarThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setNavBar(expectedNavBarThemePackageName)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedNavBarThemePackageName,
                fromParcel.getNavBarThemePackageName());
    }

    @SmallTest
    public void testGetNotificationThemePackageNameUnravelFromParcel() {
        String expectedNotificationThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setNotification(expectedNotificationThemePackageName)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(request);
        assertEquals(expectedNotificationThemePackageName,
                fromParcel.getNotificationThemePackageName());
    }

    @SmallTest
    public void testGetNumChangesRequestedUnravelFromParcel() {
        int expectedNumChangesRequested = 5;
        String dummyData = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setAlarm(dummyData)
                .setNavBar(dummyData)
                .setBootanimation(dummyData)
                .setLockWallpaper(dummyData)
                .setLiveLockScreen(dummyData)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(request);
        assertEquals(expectedNumChangesRequested,
                fromParcel.getNumChangesRequested());
    }

    @SmallTest
    public void testGetOverlayThemePackageNameUnravelFromParcel() {
        String expectedOverlayPN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setOverlay(expectedOverlayPN)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedOverlayPN,
                fromParcel.getOverlayThemePackageName());
    }

    @SmallTest
    public void testGetPerAppOverlaysUnravelFromParcel() {
        Map<String, String> expectedAppOverlayMap = new HashMap<>();

        String appkey1 = "app1";
        String appkey2 = "app2";
        String appkey3 = "app3";
        String appvalue1 = "dummy1";
        String appvalue2 = "dummy2";
        String appvalue3 = "dummy3";

        expectedAppOverlayMap.put(appkey1, appvalue1);
        expectedAppOverlayMap.put(appkey2, appvalue2);
        expectedAppOverlayMap.put(appkey3, appvalue3);

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setAppOverlay(appkey1, appvalue1)
                .setAppOverlay(appkey2, appvalue2)
                .setAppOverlay(appkey3, appvalue3)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        Map<String, String> actualAppOverlayMap = fromParcel.getPerAppOverlays();
        for (String key : actualAppOverlayMap.keySet()) {
            assertNotNull(expectedAppOverlayMap.get(key));
            assertEquals(expectedAppOverlayMap.get(key), actualAppOverlayMap.get(key));
        }
    }

    @SmallTest
    public void testGetReqeustTypeUnravelFromParcel() {
        ThemeChangeRequest.RequestType expectedRequestType =
                ThemeChangeRequest.RequestType.USER_REQUEST;

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setRequestType(expectedRequestType)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedRequestType,
                fromParcel.getReqeustType());
    }

    @SmallTest
    public void testGetRingtoneThemePackageNameUnravelFromParcel() {
        String expectedRingtoneThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setRingtone(expectedRingtoneThemePN)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedRingtoneThemePN,
                fromParcel.getRingtoneThemePackageName());
    }

    @SmallTest
    public void testGetStatusBarThemePackageNameUnravelFromParcel() {
        String expectedStatusBarThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setStatusBar(expectedStatusBarThemePN)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedStatusBarThemePN,
                fromParcel.getStatusBarThemePackageName());
    }

    @SmallTest
    public void testGetThemeComponentsMapUnravelFromParcel() {
        Map<String, String> expectedAppOverlayMap = new HashMap<>();

        String appkey1 = "app1";
        String appkey2 = "app2";
        String appkey3 = "app3";
        String appvalue1 = "dummy1";
        String appvalue2 = "dummy2";
        String appvalue3 = "dummy3";

        expectedAppOverlayMap.put(appkey1, appvalue1);
        expectedAppOverlayMap.put(appkey2, appvalue2);
        expectedAppOverlayMap.put(appkey3, appvalue3);

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setComponent(appkey1, appvalue1)
                .setComponent(appkey2, appvalue2)
                .setComponent(appkey3, appvalue3)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        Map<String, String> actualAppOverlayMap = fromParcel.getThemeComponentsMap();
        for (String key : actualAppOverlayMap.keySet()) {
            assertNotNull(expectedAppOverlayMap.get(key));
            assertEquals(expectedAppOverlayMap.get(key), actualAppOverlayMap.get(key));
        }
    }

    @SmallTest
    public void testGetWallpaperIdUnravelFromParcel() {
        long expectedWallpaperId = 123971231L;

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setWallpaperId(expectedWallpaperId)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedWallpaperId,
                fromParcel.getWallpaperId());
    }

    @SmallTest
    public void testGetWallpaperThemePackageNameUnravelFromParcel() {
        String expectedWallpaperThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setWallpaper(expectedWallpaperThemePN)
                .build();

        // Write to parcel
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);

        // Rewind
        parcel.setDataPosition(0);

        // Verify data when unraveling
        ThemeChangeRequest fromParcel = ThemeChangeRequest.CREATOR.createFromParcel(parcel);

        assertNotNull(fromParcel);
        assertEquals(expectedWallpaperThemePN,
                fromParcel.getWallpaperThemePackageName());
    }

}
