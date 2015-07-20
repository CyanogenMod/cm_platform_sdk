package org.cyanogenmod.tests.alarmclock;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.AlarmClock;
import android.util.Log;
import android.widget.Toast;
import cyanogenmod.alarmclock.ClockContract;
import cyanogenmod.alarmclock.CyanogenModAlarmClock;
import org.cyanogenmod.tests.TestActivity;

/**
 * Tests functionality added in {@link cyanogenmod.alarmclock.CyanogenModAlarmClock}
 */
public class CMAlarmClockTest extends TestActivity {
    private static final String TAG = "CMAlarmClockTest";

    private static final String[] ALARM_QUERY_COLUMNS = {
            ClockContract.AlarmsColumns._ID,
            ClockContract.AlarmsColumns.LABEL,
            ClockContract.AlarmsColumns.VIBRATE,
            ClockContract.AlarmsColumns.RINGTONE,
            ClockContract.AlarmsColumns.INCREASING_VOLUME,
            ClockContract.AlarmsColumns.PROFILE,
            ClockContract.AlarmsColumns.ENABLED
    };

    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("Test query alarms and dump to log") {
                public void run() {
                    Uri clockUri = ClockContract.AlarmsColumns.CONTENT_URI;
                    Cursor allAlarms = getContentResolver().query(clockUri,
                            ALARM_QUERY_COLUMNS, null, null, null);
                    Log.d(TAG, "All alarms: " + DatabaseUtils.dumpCursorToString(allAlarms));
                    if (allAlarms != null && !allAlarms.isClosed()) {
                        allAlarms.close();
                    }
                }
            },
            new Test("Test create alarm") {
                public void run() {
                    Intent intent = CyanogenModAlarmClock.createAlarmIntent(CMAlarmClockTest.this);
                    intent.putExtra(AlarmClock.EXTRA_HOUR, 13);
                    intent.putExtra(AlarmClock.EXTRA_MINUTES, 35);
                    intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Test from third party!");
                    intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                    startActivityForResult(intent, 0);
                }
            },
            new Test("Enable the first alarm if it exists") {
                public void run() {
                    setAlarmEnabledAtIndex(0, true);
                }
            },
            new Test("Disable the first alarm if it exists") {
                public void run() {
                    setAlarmEnabledAtIndex(0, false);
                }
            },
            new Test("Enable the second alarm if it exists") {
                public void run() {
                    setAlarmEnabledAtIndex(1, true);
                }
            },
            new Test("Disable the second alarm if it exists") {
                public void run() {
                    setAlarmEnabledAtIndex(1, false);
                }
            },
    };

    /**
     * Retrieve the id of the alarm within the Alarms table at the given index.
     * @param index The index of the alarm for which to retrieve the id, beginning at zero.
     * @return The ID of the alarm at the given index or -1L if
     *         no alarm exists at that index.
     */
    private long getAlarmIdAtIndex(int index) {
        Uri clockUri = ClockContract.AlarmsColumns.CONTENT_URI;
        Cursor allAlarms = getContentResolver().query(clockUri,
                new String[]{ClockContract.AlarmsColumns._ID}, null, null, null);
        long theIdToReturn = -1L;
        int current = 0;
        int idColumnIndex = allAlarms.getColumnIndex(ClockContract.AlarmsColumns._ID);
        allAlarms.moveToFirst();
        while(!allAlarms.isAfterLast()) {
            if (current == index) {
                theIdToReturn = allAlarms.getLong(idColumnIndex);
                break;
            }
            current++;
            allAlarms.moveToNext();
        }
        if (allAlarms != null && !allAlarms.isClosed()) {
            allAlarms.close();
        }
        return theIdToReturn;
    }

    /**
     * Construct a new Intent that will launch a DeskClock IntentService to
     * set an alarm's state to enabled or disabled.
     * @param alarmId The ID of the alarm that we will toggle.
     * @param enabledState The new state of the alarm, whether it will be enabled or disabled.
     * @return The Intent to launch that will perform this action.
     */
    private Intent getIntentToSetAlarmEnabled(long alarmId, boolean enabledState) {
        Intent intent = new Intent(CyanogenModAlarmClock.ACTION_SET_ALARM_ENABLED);
        intent.setPackage("com.android.deskclock");
        intent.putExtra(CyanogenModAlarmClock.EXTRA_ALARM_ID, alarmId);
        intent.putExtra(CyanogenModAlarmClock.EXTRA_ENABLED, enabledState);
        return intent;
    }

    private void setAlarmEnabledAtIndex(int index, boolean enabled) {
        long firstAlarmId = getAlarmIdAtIndex(index);
        if (firstAlarmId == -1L) {
            Toast.makeText(this, "Alarm not found!", Toast.LENGTH_SHORT);
        } else {
            startService(getIntentToSetAlarmEnabled(firstAlarmId, enabled));
        }
    }
}
