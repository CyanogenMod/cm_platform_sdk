package org.cyanogenmod.tests.media;

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
import android.util.SparseArray;

import org.junit.Assume;

import java.util.List;
import java.util.concurrent.CountDownLatch;
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

        AudioSessionInfo info = receiver.getSessions().get(track.getAudioSessionId());
        assertNotNull(info);
        assertNotNull(info.toString());
        assertEquals(track.getAudioSessionId(), info.mSessionId);
        assertEquals(3, info.mChannelMask);
        assertEquals(AudioManager.STREAM_MUSIC, info.mStream);

    }

    private static class AudioSessionReceiver extends BroadcastReceiver {

        private int mAdded = 0;
        private int mRemoved = 0;

        private final CountDownLatch mLatch;

        private SparseArray<AudioSessionInfo> mSessions = new SparseArray<AudioSessionInfo>();

        public AudioSessionReceiver(int count) {
            mLatch = new CountDownLatch(count);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            assertNotNull(intent);

            mLatch.countDown();

            boolean added = intent.getBooleanExtra(CMAudioManager.EXTRA_SESSION_ADDED, false);

            AudioSessionInfo info = intent.getParcelableExtra(CMAudioManager.EXTRA_SESSION_INFO);
            Log.d(TAG, "onReceive: " + info);

            assertNotNull(info);

            synchronized (mSessions) {
                if (added) {
                    mAdded++;
                    mSessions.put(info.mSessionId, info);
                } else {
                    mRemoved++;
                }
            }
        }

        public int getNumAdded() {
            return mAdded;
        }

        public int getNumRemoved() {
            return mRemoved;
        }

        public SparseArray<AudioSessionInfo> getSessions() {
            return mSessions;
        }

        public void waitForSessions() throws InterruptedException {
            mLatch.await(10, TimeUnit.SECONDS);
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
