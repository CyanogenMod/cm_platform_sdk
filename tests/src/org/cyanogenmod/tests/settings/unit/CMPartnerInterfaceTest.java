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

package org.cyanogenmod.tests.settings.unit;

import android.app.INotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.test.AndroidTestCase;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.PartnerInterface;

import java.util.List;

/**
 * Unit test for PartnerInterface
 */
public class CMPartnerInterfaceTest extends AndroidTestCase {

    private static final String TAG = "CMPartnerInterfaceTest";

    private PartnerInterface mPartnerInterface;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Only run this if we support partner interfaces
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.PARTNER));
        mPartnerInterface = PartnerInterface.getInstance(getContext());

        setupAirplaneModeTests();
        setupZenModeTests();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        teardownAirplaneModeTests();
        teardownZenModeTests();
    }

    @SmallTest
    public void testPartnerInterfaceExists() {
        assertNotNull(mPartnerInterface);
    }

    @SmallTest
    public void testPartnerInterfaceAvailable() {
        assertNotNull(mPartnerInterface.getService());
    }

    /////////////////////////////////////////////////////
    // Airplane Mode tests

    private boolean mAirplaneModeEnabled;
    private void setupAirplaneModeTests() {
        // Remember the initial state
        mAirplaneModeEnabled = getAirplaneModeEnabled();
    }

    private void teardownAirplaneModeTests() {
        // Restore airplane mode
        mPartnerInterface.setAirplaneModeEnabled(mAirplaneModeEnabled);
    }
    @SmallTest
    public void testSetAirplaneModeOn() {
        mPartnerInterface.setAirplaneModeEnabled(true);
        assertTrue(getAirplaneModeEnabled());
    }

    @SmallTest
    public void testSetAirplaneModeOff() {
        mPartnerInterface.setAirplaneModeEnabled(false);
        assertTrue(getAirplaneModeEnabled() == false);
    }

    private boolean getAirplaneModeEnabled() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    /////////////////////////////////////////////////////
    // Zen Mode Testing:
    // Allow the tests below for now, but in the future
    // implement a valid method for restoring the zen mode
    // setting, e.g. in tearDownZenModeTests

    // Because it's not possible to get & restore zen mode
    // duration, these tests will contaminate system state
    // whenver they're run. Ideally testing should instead
    // be done in another suite, such as using Mokito
    // https://corner.squareup.com/2012/10/mockito-android.html
    //
    // However because of the complexity, for now allow
    // these unit tests as-is.
    //
    // THESE WILL WIPE OUT any duration-based zen modes in
    // effect!
    private static final long DURATION_30s_MS = 30 * DateUtils.SECOND_IN_MILLIS;
    private static final long DURATION_TOLERANCE_MS = 5;    //5 ms in tolerance due to latency
    private static final long DURATION_TEST_MAX_MS = DateUtils.MINUTE_IN_MILLIS; //Allow 1 minute max for unit testing

    private INotificationManager mNotificationManager;
    private CountdownConditionListener mConditionListener;

    private void setupZenModeTests() {
        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mConditionListener = new CountdownConditionListener();
        try {
            mNotificationManager.requestZenModeConditions(mConditionListener, Condition.FLAG_RELEVANT_ALWAYS);
        } catch (RemoteException e) {
            fail("requestZenModeConditions exception " + e);
        }
    }

    private void teardownZenModeTests() {
        // For now, restore the zen mode to the system default
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_OFF);
    }

    @SmallTest
    public void testSetZenModeImportantInterruptions() {
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        assertEquals(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, getZenMode());
    }

    @FlakyTest(tolerance = 5)
    @SmallTest
    public void testSetZenModeImportantInterruptionsWithDurations() {
        // 0 duration
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, 0);

        // Indefinitely
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, Long.MAX_VALUE);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, -1);

        // normal duration values (1, 5s)
        // NOTE: these tests do not return until duration has passed. Use with care!
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, DateUtils.SECOND_IN_MILLIS);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS, 5 * DateUtils.SECOND_IN_MILLIS);
    }

    @SmallTest
    public void testSetZenModeNoInterruptions() {
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS);
        assertEquals(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, getZenMode());
    }

    @FlakyTest(tolerance = 5)
    @SmallTest
    public void testSetZenModeNoInterruptionsWithDurations() {
        // 0 duration
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 0);

        // Indefinitely
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, Long.MAX_VALUE);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, -1);

        // normal duration values (1, 5s)
        // NOTE: these tests do not return until duration has passed. Use with care!
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, DateUtils.SECOND_IN_MILLIS);
        testZenModeWithDuration(PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS, 5 * DateUtils.SECOND_IN_MILLIS);
    }

    @SmallTest
    public void testSetZenModeOff() {
        mPartnerInterface.setZenMode(PartnerInterface.ZEN_MODE_OFF);
        assertEquals(PartnerInterface.ZEN_MODE_OFF, getZenMode());
    }

    private final static int BUFFER_ELEMENTS_TO_REC = 1024;
    private final static int BYTES_PER_ELEMENT = 2;
    private static final int RECORDER_SAMPLERATE = 41000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT,
                    AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(
                                    cyanogenmod.media.MediaRecorder.AudioSource.HOTWORD,
                                    rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

    @SmallTest
    public void testGetCurrentHotwordPackageName() {
        // make sure no one is actively stealing this as we attempt to
        assertNull(mPartnerInterface.getCurrentHotwordPackageName());

        // find first viable audio record
        final AudioRecord audioRecorder = findAudioRecord();

        audioRecorder.startRecording();
        assertEquals(mContext.getPackageName(), mPartnerInterface.getCurrentHotwordPackageName());
        audioRecorder.stop();
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
        // skip durations that are indefinite
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
        int zenMode = -1;
        try {
            zenMode = mNotificationManager.getZenMode();
        } catch (RemoteException rex) {
            fail("INotificationManager.getZenMode() " + rex.getMessage());
            return -1;
        }
        Log.d(TAG, "getZenMode returning " + zenMode);
        return zenMode;
    }
    /**
     * CountdownConditionListener
     * This class is passed to blocks until the Countdown is received
     */
    private static class CountdownConditionListener
            extends IConditionListener.Stub {

        private static long INVALID_ENDTIME = -1;
        private long mEndTime = INVALID_ENDTIME;

        public CountdownConditionListener() {
        }

        /**
         * waitForDuration: blocks until onConditionReceived
         * This CountdownConditionListener was previously passed
         * to the
         * @return
         * the duration of
         */
        public synchronized long waitForDuration(long startTimeMillis) {
            Log.d(TAG, "waitForDuration");
            // If we have a stale endtime, then discard it
            if (mEndTime < startTimeMillis) {
                mEndTime = INVALID_ENDTIME;
            }
            // If no valid endtime, then block and wait for the current
            // duration to expire. The wait ends when
            // onConditionsReceived is called
            if (mEndTime == INVALID_ENDTIME) {
                try {
                    // wait no more than DURATION_TEST_MAX_MS
                    wait(DURATION_TEST_MAX_MS);
                } catch (InterruptedException iex) {
                    Log.e(TAG, "waitForDuration", iex);

                    return -1;
                }
            }
            if (mEndTime == INVALID_ENDTIME) {
                Log.d(TAG, "waitForDuration found invalid endtime. Did you exceed the max duration (" + DURATION_TEST_MAX_MS + " ms)?");
                return -1;
            }

            Log.d(TAG, "waitForDuration returning endtime:" + mEndTime + " duration:" +  (mEndTime - startTimeMillis));
            final long duration = mEndTime - startTimeMillis;

            // Reset endtime to show that it's been consumed
            mEndTime = INVALID_ENDTIME;
            return duration;
        }

        /**
         * onConditionReceived: called when a condition is triggered
         * @param conditions - conditions that triggered
         * This is actually just the Alarm endtime that CountdownConditionProvider
         * previously submitted
         * @throws RemoteException
         */
        @Override
        public synchronized void onConditionsReceived(Condition[] conditions) throws RemoteException {
            // CountdownConditionProvider only triggers 1 condition at a time
            mEndTime = parseEndTime(conditions[0].id);
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
}

