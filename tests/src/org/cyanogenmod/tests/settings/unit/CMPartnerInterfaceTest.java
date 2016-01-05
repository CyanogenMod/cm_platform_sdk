package org.cyanogenmod.tests.settings.unit;

import android.app.INotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.IConditionListener.Stub;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.provider.Settings;
import android.util.Log;
import cyanogenmod.app.PartnerInterface;

import java.util.List;

/**
 * Unit test for PartnerInterface
 */
public class CMPartnerInterfaceTest extends AndroidTestCase {

    private static final String TAG = "CMPartnerInterfaceTest";

    private static final long DURATION_MINUTE_MS = 60000;
    private static final long DURATION_TOLERANCE_MS = 5;    //5 ms in tolerance due to latency
    private static final long DURATION_TEST_MAX_MS = DURATION_MINUTE_MS; //Allow 1 minute max for unit testing

    private PartnerInterface mPartnerInterface;
    private INotificationManager mNotificationManager;
    private CountdownConditionListener mConditionListener;

    /**
     * CountdownConditionListener
     * This class blocks until the Countdown is received
     */
    private static class CountdownConditionListener
            extends IConditionListener.Stub {

        private long mEndtime = -1;

        public CountdownConditionListener() {

        }

        /**
         * waitForDuration: blocks until onConditionReceived
         * @return
         */
        public synchronized long waitForDuration(long startTimeMillis) {
            Log.d(TAG, "waitForDuration");
            // discard old end time
            if (mEndtime < startTimeMillis) {
                mEndtime = -1;
            }
            // wait for new endtime
            if (mEndtime == -1) {
                try {
                    wait(DURATION_TEST_MAX_MS);
                } catch (InterruptedException iex) {
                    Log.e(TAG, "waitForDuration", iex);
                    return -1;
                }
            }
            if (mEndtime == -1) {
                Log.d(TAG, "waitForDuration found invalid endtime:" + mEndtime);
                return -1;
            }

            Log.d(TAG, "waitForDuration returning endtime:" + mEndtime + " duration:" + (mEndtime - startTimeMillis));
            final long duration = mEndtime - startTimeMillis;
            mEndtime = -1;
            return duration;
        }

        /**
         * onConditionReceived: called when a condition is triggered
         * @param conditions - conditions that triggered
         * @throws RemoteException
         */
        @Override
        public synchronized void onConditionsReceived(Condition[] conditions) throws RemoteException {
            // CountdownConditionProvider only triggers 1 condition at a time
            mEndtime = parseEndTime(conditions[0].id);
            notify();
        }

        private long parseEndTime(Uri conditionUri) {
            final List<String> pathSegments = conditionUri.getPathSegments();
            return Long.decode(pathSegments.get(pathSegments.size() - 1));
        }

        // Private method for debugging
        private void logConditions(Condition[] conditions) {
            for (int i = 0; i < conditions.length; i++) {
                Log.d(TAG, "condition[" + i + "]:" + conditions[i].id);
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPartnerInterface = PartnerInterface.getInstance(getContext());
        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mConditionListener = new CountdownConditionListener();
        try {
            mNotificationManager.requestZenModeConditions(mConditionListener, Condition.FLAG_RELEVANT_ALWAYS);
        } catch (RemoteException e) {
            fail("requestZenModeConditions exception " + e);
        }
    }

    @SmallTest
    public void testPartnerInterfaceExists() {
        assertNotNull(mPartnerInterface);
    }

    @SmallTest
    public void testPartnerInterfaceAvailable() {
        assertNotNull(mPartnerInterface.getService());
    }

    @SmallTest
    public void testSetAirplaneModeOn() {
        mPartnerInterface.setAirplaneModeEnabled(true);
        assertTrue(Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0);
    }

    @SmallTest
    public void testSetAirplaneModeOff() {
        mPartnerInterface.setAirplaneModeEnabled(false);
        assertTrue(Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 0);
    }

    @SmallTest
    public void testSetZenModeImportantInterruptions() {
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        assertEquals(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, getZenMode());
    }

    @MediumTest
    public void testSetZenModeImportantInterruptionsWithDurations() {
        // 0 duration
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, 0);

        // Indefinitely
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, Long.MAX_VALUE);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, -1);

        // normal duration values (1, 5s)
        // NOTE: these tests do not return until duration has passed. Use with care!
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 1000);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 5000);
    }

    @SmallTest
    public void testSetZenModeNoInterruptions() {
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS);
        assertEquals(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, getZenMode());
    }

    @MediumTest
    public void testSetZenModeNoInterruptionsWithDurations() {
        // 0 duration
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 0);

        // Indefinitely
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, Long.MAX_VALUE);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, -1);

        // normal duration values (1, 5s)
        // NOTE: these tests do not return until duration has passed. Use with care!
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 1000);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 5000);
    }

    @SmallTest
    public void testSetZenModeOff() {
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_OFF);
        assertEquals(PartnerInterface.ZEN_MODE_OFF, getZenMode());
    }

    /**
     * testZenModeWithDuration: sets Zen Mode with duration and blocks
     * until the duration is verified. Use with care! This does not
     * return until after durationMillis
     * @param mode
     * @param durationMillis
     */
    private void testZenModeWithDuration(int mode, long durationMillis) {

        final long startTimeMillis = System.currentTimeMillis();
        mPartnerInterface.setZenModeWithDuration(mode, durationMillis);
        final int actualZenMode = getZenMode();

        // check zen mode is correct
        if (durationMillis == 0) {
            assertTrue(actualZenMode == mode || actualZenMode == PartnerInterface.ZEN_MODE_OFF);
        } else {
            assertEquals(mode, actualZenMode);
        }
        // skip duration check for indefinite cases
        if (actualZenMode == PartnerInterface.ZEN_MODE_OFF || durationMillis < 0 || durationMillis == Long.MAX_VALUE) {
            return;
        }
        // check duration is correct
        final long zenDuration = getZenModeDuration(startTimeMillis);
        assertTrue(Math.abs(zenDuration - durationMillis) <= DURATION_TOLERANCE_MS); //Allow a tolerance
    }

    /**
     * getZenModeDuration: Blocking call to wait for ZenMode duration.
     * @param startTimeMillis - start time of the duration
     * @return
     */
    private long getZenModeDuration(long startTimeMillis) {
        // NOTE: waits for the next duration to be triggered
        return mConditionListener.waitForDuration(startTimeMillis);
    }

    private int getZenMode() {
        try {
            return mNotificationManager.getZenMode();
        } catch (RemoteException rex) {
            fail("INotificationManager.getZenMode() " + rex.getMessage());
            return -1;
        }
    }
}
