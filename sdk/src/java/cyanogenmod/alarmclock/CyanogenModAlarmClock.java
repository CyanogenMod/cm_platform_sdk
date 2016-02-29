/**
 * Copyright (c) 2015, The CyanogenMod Project
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
package cyanogenmod.alarmclock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.AlarmClock;

import java.util.List;

/**
 * The CyanogenModAlarmClock class contains utilities for interacting with
 * a variety of Alarm features that the CyanogenMod AlarmClock application
 * (based on AOSP DeskClock) supports.
 */
public class CyanogenModAlarmClock {
    /**
     * The package name of the CyanogenMod DeskClock application.
     */
    private static final String DESKCLOCK_PACKAGE = "com.android.deskclock";

    /**
     * Allows an application to make modifications to existing alarms,
     * such as turning them on or off.
     *
     * @see #ACTION_SET_ALARM_ENABLED
     */
    public static final String MODIFY_ALARMS_PERMISSION
            = "cyanogenmod.alarmclock.permission.MODIFY_ALARMS";

    /**
     * Allows an application to have read access to all alarms in the
     * CyanogenMod DeskClock application.
     *
     * @see cyanogenmod.alarmclock.ClockContract
     */
    public static final String READ_ALARMS_PERMISSION
            = "cyanogenmod.alarmclock.permission.READ_ALARMS";

    /**
     * Allows an application to have write access to all alarms in the
     * CyanogenMod DeskClock application. This is a system level permission.
     *
     * @see cyanogenmod.alarmclock.ClockContract
     * @hide
     */
    public static final String WRITE_ALARMS_PERMISSION
            = "cyanogenmod.alarmclock.permission.WRITE_ALARMS";

    /**
     * Service Action: Set an existing alarm to be either enabled or disabled.
     * <p>
     * This action sets an alarm to be enabled or disabled.
     * </p><p>
     * This action requests an alarm with the id specified by {@link #EXTRA_ALARM_ID}
     * be set to enabled or disabled, depending on the value set with {@link #EXTRA_ENABLED}.
     * </p>
     *
     * <p>Requires permission {@link #MODIFY_ALARMS_PERMISSION} to launch this
     * intent.
     * </p>
     *
     * <p>Always set the package name of the Intent that will launch this action
     * to {@link #DESKCLOCK_PACKAGE} explicitly, for security.</p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     *     <li>{@link #EXTRA_ALARM_ID} <em>(required)</em>: The id of the alarm to modify,
     *     as stored in {@link cyanogenmod.alarmclock.ClockContract.AlarmsColumns#_ID}</li>
     *     <li>{@link #EXTRA_ENABLED} <em>(required)</em>: Whether to set this alarm to be enabled
     *     or disabled. </li>
     * </ul>
     */
    public static final String ACTION_SET_ALARM_ENABLED
            = "cyanogenmod.alarmclock.SET_ALARM_ENABLED";

    /**
     * Bundle extra: The id of the alarm.
     * <p>
     * Used by {@link #ACTION_SET_ALARM_ENABLED}.
     * </p><p>
     * This extra is required.
     * </p><p>
     * The value is an {@link Long} and is the ID stored in
     * {@link cyanogenmod.alarmclock.ClockContract.AlarmsColumns#_ID} for this alarm.
     * </p>
     *
     * @see #ACTION_SET_ALARM_ENABLED
     * @see #EXTRA_ENABLED
     */
    public static final String EXTRA_ALARM_ID = "cyanogenmod.intent.extra.alarmclock.ID";

    /**
     * Bundle extra: Whether to set the alarm to enabled to disabled.
     * <p>
     * Used by {@link #ACTION_SET_ALARM_ENABLED}.
     * </p><p>
     * This extra is required.
     * </p><p>
     * The value is an {@link Boolean} and if true, will set the alarm specified by
     * {@link #EXTRA_ALARM_ID} to be enabled. Otherwise, the alarm will be disabled.
     * </p>
     *
     * @see #ACTION_SET_ALARM_ENABLED
     * @see #EXTRA_ALARM_ID
     */
    public static final String EXTRA_ENABLED = "cyanogenmod.intent.extra.alarmclock.ENABLED";

    /**
     * <p>
     * Retrieves an Intent that is prepopulated with the proper action and ComponentName to
     * create a new alarm in the CyanogenMod DeskClock application.
     * </p>
     * <p> The action will be set to {@link android.provider.AlarmClock#ACTION_SET_ALARM}. Use the
     * Intent extras contained at {@link android.provider.AlarmClock} to configure the alarm.
     * </p>
     * <p>Requires permission {@link android.Manifest.permission#SET_ALARM} to launch this
     * intent.
     * </p>
     *
     * @see android.provider.AlarmClock#ACTION_SET_ALARM
     * @return The Intent to create a new alarm with the CyanogenMod DeskClock application.
     */
    public static Intent createAlarmIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction(AlarmClock.ACTION_SET_ALARM);

        // Retrieve the ComponentName of the best result
        // for ACTION_SET_ALARM within system applications only.
        // This will exclude third party alarm apps that have been installed.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolves = pm.queryIntentActivities(intent, 0);
        ComponentName selectedSystemComponent = null;
        for (ResolveInfo info : resolves) {
            if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                selectedSystemComponent = new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name);
                break;
            }
        }
        if (selectedSystemComponent != null) {
            intent.setComponent(selectedSystemComponent);
        }
        return intent;
    }
}
