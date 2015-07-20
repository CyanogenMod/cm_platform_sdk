package cyanogenmod.alarmclock;

import android.content.Intent;
import android.provider.AlarmClock;

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
     * Service Action: Set an existing alarm to be either enabled or disabled.
     * <p>
     * This action sets an alarm to be enabled or disabled.
     * </p><p>
     * This action requests an alarm with the id specified by {@link #EXTRA_ALARM_ID}
     * be set to enabled or disabled, depending on the value set with {@link #EXTRA_ENABLED}.
     * </p>
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
     * Retrieves an Intent that is prepopulated with the proper action and package to
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
    public static Intent createAlarmIntent() {
        Intent intent = new Intent();
        intent.setAction(AlarmClock.ACTION_SET_ALARM);
        intent.setPackage(DESKCLOCK_PACKAGE);
        return intent;
    }
}
