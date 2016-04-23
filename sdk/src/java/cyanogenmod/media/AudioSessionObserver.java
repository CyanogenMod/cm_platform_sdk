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
package cyanogenmod.media;

import android.util.ArraySet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * AudioSessionObserver allows an application to observe when other applications
 * open and close audio output effect sessions. At the native layer, this is
 * bound to the start and stop of individual audio streams and is reference
 * counted in the same way.
 *
 * This functionality is meant to augment AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
 * and AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, which is the
 * recommended way to notify control apps that they should activate.
 * Unfortunately, this isn't adopted by all applications today. Using this API,
 * an application can make a decision using the AudioSessionInfo about how
 * it should handle the stream.
 *
 */
public class AudioSessionObserver {

    private static final String TAG = "AudioSessionObserver";

    private static boolean sNativeLibraryLoaded;

    private static final int AUDIO_STATUS_OK = 0;

    //keep in sync with include/media/AudioPolicy.h
    private final static int AUDIO_OUTPUT_SESSION_EFFECTS_UPDATE = 10;

    private static final ArraySet<AudioSessionCallback> sCallbacks =
            new ArraySet<AudioSessionCallback>();

    static {
        try {
            System.loadLibrary("cmsdk_media_jni");
            sNativeLibraryLoaded = true;

        } catch (Throwable t) {
            sNativeLibraryLoaded = false;
            Log.w(TAG, "CMSDK native interface unavailable");
        }
    }

    public static final class AudioSessionInfo {
        /**
         * Unique session id
         */
        public final int mSessionId;
        /**
         * Stream type - see audio_stream_type_t
         */
        public final int mStream;
        /**
         * Output flags - see audio_output_flags_t
         */
        public final int mFlags;
        /**
         * Channel mask - see audio_channel_mask_t
         */
        public final int mChannelMask;
        /**
         * UID of the source application
         */
        public final int mUid;

        public AudioSessionInfo(int sessionId, int stream, int flags, int channelMask, int uid) {
            mSessionId = sessionId;
            mStream = stream;
            mFlags = flags;
            mChannelMask = channelMask;
            mUid = uid;
        }

        @Override
        public String toString() {
            return String.format(
                    "audioSessionInfo[sessionId=%d, stream=%d, flags=%d, channelMask=%d, uid=%d",
                    mSessionId, mStream, mFlags, mChannelMask, mUid);
        }
    }

    /**
     * Handles events from AudioPolicyManager sent when streams are setup and
     * torn down. Clients can observe these events to make decisions about
     * audio effects on the stream.
     *
     * @see android.media.audiopolicy.AudioPolicy
     */
    public interface AudioSessionCallback {

        void onSessionAdded(AudioSessionInfo sessionInfo);

        void onSessionRemoved(AudioSessionInfo sessionInfo);
    }

    /**
     * Register a new callback to fire when audio sessions are
     * added and removed.
     *
     * @param cb
     * @return true if successful
     */
    public static boolean addCallback(AudioSessionCallback cb) {
        if (!sNativeLibraryLoaded) {
            return false;
        }
        synchronized (AudioSessionObserver.class) {
            sCallbacks.add(cb);
            if (sCallbacks.size() == 1) {
                native_registerAudioSessionCallback(true);
            }
        }
        return true;
    }

    /**
     * Unregister a previously registered callback.
     *
     * @param cb
     * @return true if successful
     */
    public static boolean removeCallback(AudioSessionCallback cb) {
        if (!sNativeLibraryLoaded) {
            return false;
        }
        synchronized (AudioSessionObserver.class) {
            sCallbacks.remove(cb);
            if (sCallbacks.size() == 0) {
                native_registerAudioSessionCallback(false);
            }
        }
        return true;
    }

    /**
     * List all audio sessions for the given stream type.
     *
     * @param streamType
     * @return the current active audio sessions
     * @see android.media.AudioSystem
     */
    public static List<AudioSessionInfo> listAudioSessions(int streamType) {
        final ArrayList<AudioSessionInfo> sessions = new ArrayList<AudioSessionInfo>();
        if (!sNativeLibraryLoaded) {
            // no sessions for u
            return sessions;
        }

        int status = native_listAudioSessions(streamType, sessions);
        if (status != AUDIO_STATUS_OK) {
            Log.e(TAG, "Error retrieving audio sessions! status=" + status);
        }

        return sessions;
    }

    /*
     * Handles events from JNI
     */
    private static void audioSessionCallbackFromNative(int event,
            AudioSessionInfo sessionInfo, boolean added) {
        synchronized (AudioSessionObserver.class) {
            for (AudioSessionCallback cb : sCallbacks) {

                switch(event) {
                    case AUDIO_OUTPUT_SESSION_EFFECTS_UPDATE:
                        if (added) {
                            cb.onSessionAdded(sessionInfo);
                        } else {
                            cb.onSessionRemoved(sessionInfo);
                        }
                        break;
                    default:
                        Log.e(TAG, "AudioSessionCallbackFromNative: unknown event " + event);
                }
            }
        }
    }

    private static native final void native_registerAudioSessionCallback(boolean enabled);

    private static native final int native_listAudioSessions(
            int stream, ArrayList<AudioSessionInfo> sessions);
}
