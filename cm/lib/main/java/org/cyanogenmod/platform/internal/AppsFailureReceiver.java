/*
 * Copyright (C) 2010, T-Mobile USA, Inc.
 * Copyright (C) 2015-2016 The CyanogenMod Project
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
package org.cyanogenmod.platform.internal;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ThemeConfig;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;

import org.cyanogenmod.internal.util.ThemeUtils;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.themes.IThemeService;
import cyanogenmod.themes.ThemeChangeRequest;
import cyanogenmod.themes.ThemeChangeRequest.RequestType;

import static cyanogenmod.content.Intent.ACTION_APP_FAILURE;

public class AppsFailureReceiver extends BroadcastReceiver {

    private static final int FAILURES_THRESHOLD = 3;
    private static final int EXPIRATION_TIME_IN_MILLISECONDS = 30000; // 30 seconds

    private int mFailuresCount = 0;
    private long mStartTime = 0;

    // This function implements the following logic.
    // If after a theme was applied the number of application launch failures
    // at any moment was equal to FAILURES_THRESHOLD
    // in less than EXPIRATION_TIME_IN_MILLISECONDS
    // the default theme is applied unconditionally.
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        final long currentTime = SystemClock.uptimeMillis();
        if (ACTION_APP_FAILURE.equals(action)) {
            if (currentTime - mStartTime > EXPIRATION_TIME_IN_MILLISECONDS) {
                // reset both the count and the timer
                mStartTime = currentTime;
                mFailuresCount = 0;
            }
            if (mFailuresCount <= FAILURES_THRESHOLD) {
                mFailuresCount++;
                if (mFailuresCount == FAILURES_THRESHOLD) {
                    // let the theme manager take care of getting us back on the default theme
                    IThemeService tm = IThemeService.Stub.asInterface(ServiceManager
                            .getService(CMContextConstants.CM_THEME_SERVICE));
                    final String themePkgName = ThemeConfig.SYSTEM_DEFAULT;
                    ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
                    builder.setOverlay(themePkgName)
                            .setStatusBar(themePkgName)
                            .setNavBar(themePkgName)
                            .setIcons(themePkgName)
                            .setFont(themePkgName)
                            .setBootanimation(themePkgName)
                            .setWallpaper(themePkgName)
                            .setLockWallpaper(themePkgName)
                            .setAlarm(themePkgName)
                            .setNotification(themePkgName)
                            .setRingtone(themePkgName)
                            .setRequestType(RequestType.THEME_RESET);
                    // Since we are resetting everything to the system theme, we can have the
                    // theme service remove all per app themes without setting them explicitly :)
                    try {
                        tm.requestThemeChange(builder.build(), true);
                        postThemeResetNotification(context);
                    } catch (RemoteException e) {
                        /* ignore */
                    }
                }
            }
        } else if (ThemeUtils.ACTION_THEME_CHANGED.equals(action)) {
            // reset both the count and the timer
            mStartTime = currentTime;
            mFailuresCount = 0;
        }
    }

    /**
     * Posts a notification to let the user know their theme was reset
     * @param context
     */
    private void postThemeResetNotification(Context context) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getString(R.string.theme_reset_notification_title);
        String body = context.getString(R.string.theme_reset_notification_message);
        Notification notice = new Notification.Builder(context)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setWhen(System.currentTimeMillis())
                .setCategory(Notification.CATEGORY_SYSTEM)
                .setPriority(Notification.PRIORITY_MAX)
                .build();
        nm.notify(R.string.theme_reset_notification_title, notice);
    }
}
