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

import android.content.res.ThemeConfig;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import cyanogenmod.themes.ThemeChangeRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Exercise both the ThemeChangeRequest object and the builder.
 */
public class ThemeChangeRequestBuilderTest extends AndroidTestCase {

    @SmallTest
    public void testConstructor() {
        ThemeChangeRequest themeChangeRequest = new ThemeChangeRequest.Builder().build();
        assertNotNull(themeChangeRequest);
    }

    @SmallTest
    public void testThemeConfigConstructor() {
        Map<String, ThemeConfig.AppTheme> dummyMap = new HashMap<>();
        ThemeConfig config = new ThemeConfig(dummyMap);
        ThemeChangeRequest themeChangeRequest = new ThemeChangeRequest.Builder(config).build();
        assertNotNull(themeChangeRequest);
    }

    @SmallTest
    public void testGetAlarmThemePackageName() {
        String expectedAlarmPackage = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setAlarm(expectedAlarmPackage)
                .build();

        assertNotNull(request);
        assertEquals(expectedAlarmPackage, request.getAlarmThemePackageName());
    }

    @SmallTest
    public void testGetBootanimationThemePackageName() {
        String expectedBootAnimationThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setBootanimation(expectedBootAnimationThemePackageName)
                .build();

        assertNotNull(request);
        assertEquals(expectedBootAnimationThemePackageName,
                request.getBootanimationThemePackageName());
    }

    @SmallTest
    public void testGetFontThemePackageName() {
        String expectedFontThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setFont(expectedFontThemePackageName)
                .build();

        assertNotNull(request);
        assertEquals(expectedFontThemePackageName,
                request.getFontThemePackageName());
    }

    @SmallTest
    public void testGetIconsThemePackageName() {
        String expectedIconThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setIcons(expectedIconThemePackageName)
                .build();

        assertNotNull(request);
        assertEquals(expectedIconThemePackageName,
                request.getIconsThemePackageName());
    }

    @SmallTest
    public void testGetLiveLockScreenThemePackageName() {
        String expectedLiveLockscreenThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setLiveLockScreen(expectedLiveLockscreenThemePN)
                .build();

        assertNotNull(request);
        assertEquals(expectedLiveLockscreenThemePN,
                request.getLiveLockScreenThemePackageName());
    }

    @SmallTest
    public void testGetLockWallpaperThemePackageName() {
        String expectedLockWallpaperThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setLockWallpaper(expectedLockWallpaperThemePN)
                .build();

        assertNotNull(request);
        assertEquals(expectedLockWallpaperThemePN,
                request.getLockWallpaperThemePackageName());
    }

    @SmallTest
    public void testGetNavBarThemePackageName() {
        String expectedNavBarThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setNavBar(expectedNavBarThemePackageName)
                .build();

        assertNotNull(request);
        assertEquals(expectedNavBarThemePackageName,
                request.getNavBarThemePackageName());
    }

    @SmallTest
    public void testGetNotificationThemePackageName() {
        String expectedNotificationThemePackageName = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setNotification(expectedNotificationThemePackageName)
                .build();

        assertNotNull(request);
        assertEquals(expectedNotificationThemePackageName,
                request.getNotificationThemePackageName());
    }

    @SmallTest
    public void testGetNumChangesRequested() {
        int expectedNumChangesRequested = 5;
        String dummyData = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setAlarm(dummyData)
                .setNavBar(dummyData)
                .setBootanimation(dummyData)
                .setLockWallpaper(dummyData)
                .setLiveLockScreen(dummyData)
                .build();

        assertNotNull(request);
        assertEquals(expectedNumChangesRequested,
                request.getNumChangesRequested());
    }

    @SmallTest
    public void testGetOverlayThemePackageName() {
        String expectedOverlayPN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setOverlay(expectedOverlayPN)
                .build();

        assertNotNull(request);
        assertEquals(expectedOverlayPN,
                request.getOverlayThemePackageName());
    }

    @SmallTest
    public void testGetPerAppOverlays() {
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

        assertNotNull(request);
        Map<String, String> actualAppOverlayMap = request.getPerAppOverlays();
        for (String key : actualAppOverlayMap.keySet()) {
            assertNotNull(expectedAppOverlayMap.get(key));
            assertEquals(expectedAppOverlayMap.get(key), actualAppOverlayMap.get(key));
        }
    }

    @SmallTest
    public void testGetReqeustType() {
        ThemeChangeRequest.RequestType expectedRequestType =
                ThemeChangeRequest.RequestType.USER_REQUEST;

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setRequestType(expectedRequestType)
                .build();

        assertNotNull(request);
        assertEquals(expectedRequestType,
                request.getReqeustType());
    }

    @SmallTest
    public void testGetRingtoneThemePackageName() {
        String expectedRingtoneThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setRingtone(expectedRingtoneThemePN)
                .build();

        assertNotNull(request);
        assertEquals(expectedRingtoneThemePN,
                request.getRingtoneThemePackageName());
    }

    @SmallTest
    public void testGetStatusBarThemePackageName() {
        String expectedStatusBarThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setStatusBar(expectedStatusBarThemePN)
                .build();

        assertNotNull(request);
        assertEquals(expectedStatusBarThemePN,
                request.getStatusBarThemePackageName());
    }

    @SmallTest
    public void testGetThemeComponentsMap() {
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

        assertNotNull(request);
        Map<String, String> actualAppOverlayMap = request.getThemeComponentsMap();
        for (String key : actualAppOverlayMap.keySet()) {
            assertNotNull(expectedAppOverlayMap.get(key));
            assertEquals(expectedAppOverlayMap.get(key), actualAppOverlayMap.get(key));
        }
    }

    @SmallTest
    public void testGetWallpaperId() {
        long expectedWallpaperId = 123971231L;

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setWallpaperId(expectedWallpaperId)
                .build();

        assertNotNull(request);
        assertEquals(expectedWallpaperId,
                request.getWallpaperId());
    }

    @SmallTest
    public void testGetWallpaperThemePackageName() {
        String expectedWallpaperThemePN = "dummy";

        ThemeChangeRequest request = new ThemeChangeRequest.Builder()
                .setWallpaper(expectedWallpaperThemePN)
                .build();

        assertNotNull(request);
        assertEquals(expectedWallpaperThemePN,
                request.getWallpaperThemePackageName());
    }
}
