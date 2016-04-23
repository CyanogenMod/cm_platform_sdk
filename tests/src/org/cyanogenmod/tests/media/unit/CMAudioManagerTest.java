/*
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
package org.cyanogenmod.tests.media.unit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.junit.Assume;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.media.AudioSessionInfo;
import cyanogenmod.media.CMAudioManager;
import cyanogenmod.media.ICMAudioService;

public class CMAudioManagerTest extends AndroidTestCase {

    private static final String TAG = "CMAudioManagerTest";

    private CMAudioManager mCMAudioManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.AUDIO));

        mCMAudioManager = CMAudioManager.getInstance(mContext);
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mCMAudioManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        ICMAudioService service = CMAudioManager.getService();
        assertNotNull(service);
    }

    @SmallTest
    public void testSessionList() {

        AudioTrack track = createTestTrack();
        int session = track.getAudioSessionId();

        AudioSessionInfo info = findAudioSessionInfo(session);
        assertNotNull(info);
        assertEquals(session, info.mSessionId);
        assertEquals(3, info.mChannelMask);

        track.release();

        info = findAudioSessionInfo(session);
        assertNull(info);
    }

    @SmallTest
    public void testSessionInfoBroadcast() throws Exception {

        IntentFilter filter = new IntentFilter(CMAudioManager.ACTION_AUDIO_SESSIONS_CHANGED);
        AudioSessionReceiver receiver = new AudioSessionReceiver(2);
        mContext.registerReceiver(receiver, filter);

        AudioTrack track = createTestTrack();
        track.play();
        track.release();

        receiver.waitForSessions();

        mContext.unregisterReceiver(receiver);

        assertEquals(1, receiver.getNumAdded());
        assertEquals(1, receiver.getNumRemoved());

        assertEquals(1, receiver.getSessions().size());

        AudioSessionInfo info = receiver.getSessions().get(0);
        assertNotNull(info);
        assertNotNull(info.toString());
        assertEquals(track.getAudioSessionId(), info.mSessionId);
        assertEquals(3, info.mChannelMask);
        assertEquals(AudioManager.STREAM_MUSIC, info.mStream);

    }

    private static final int SESSIONS = 50;

    @SmallTest
    public void testSymphonyOfDestruction() throws Exception {
        IntentFilter filter = new IntentFilter(CMAudioManager.ACTION_AUDIO_SESSIONS_CHANGED);
        AudioSessionReceiver receiver = new AudioSessionReceiver(SESSIONS * 2);
        mContext.registerReceiver(receiver, filter);

        final List<Integer> sessions = new ArrayList<Integer>();

        ExecutorService sexecutioner = Executors.newFixedThreadPool(5);
        for (int i = 0; i < SESSIONS; i++) {
            sexecutioner.submit(new Runnable() {
                @Override
                public void run() {
                    AudioTrack track = createTestTrack();
                    synchronized (sessions) {
                        sessions.add(track.getAudioSessionId());
                    }
                    track.play();
                    track.release();
                }
            });
            if ((i % 2) == 0) {
                Thread.sleep(100);
            }
        }

        receiver.waitForSessions();
        sexecutioner.shutdown();

        assertEquals(SESSIONS, sessions.size());
        assertEquals(SESSIONS, receiver.getNumAdded());
        assertEquals(SESSIONS, receiver.getNumRemoved());

        for (AudioSessionInfo info : receiver.getSessions()) {
            assertTrue(sessions.contains(info.mSessionId));
        }
    }

    private static class AudioSessionReceiver extends BroadcastReceiver {

        private int mAdded = 0;
        private int mRemoved = 0;

        private final CountDownLatch mLatch;

        private List<AudioSessionInfo> mSessions = new ArrayList<AudioSessionInfo>();

        public AudioSessionReceiver(int count) {
            mLatch = new CountDownLatch(count);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            assertNotNull(intent);

            boolean added = intent.getBooleanExtra(CMAudioManager.EXTRA_SESSION_ADDED, false);

            AudioSessionInfo info = intent.getParcelableExtra(CMAudioManager.EXTRA_SESSION_INFO);
            Log.d(TAG, "onReceive: " + info);

            assertNotNull(info);

            synchronized (mSessions) {
                if (added) {
                    mAdded++;
                    mSessions.add(info);
                } else {
                    mRemoved++;
                }
            }

            mLatch.countDown();
        }

        public int getNumAdded() {
            return mAdded;
        }

        public int getNumRemoved() {
            return mRemoved;
        }

        public List<AudioSessionInfo> getSessions() {
            return mSessions;
        }

        public void waitForSessions() throws InterruptedException {
            mLatch.await(60, TimeUnit.SECONDS);
        }
    };

    private AudioSessionInfo findAudioSessionInfo(int sessionId) {
        List<AudioSessionInfo> infos = mCMAudioManager.listAudioSessions(AudioManager.STREAM_MUSIC);
        for (AudioSessionInfo info : infos) {
            if (info.mSessionId == sessionId) {
                return info;
            }
        }
        return null;
    }

    private AudioTrack createTestTrack() {
        int bytes = 2 * 44100 / 1000;
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bytes,
                AudioTrack.STATE_INITIALIZED);
        return track;
    }
}
