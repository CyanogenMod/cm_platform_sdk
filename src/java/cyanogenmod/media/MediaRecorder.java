/*
 * Copyright (C) 2015 The CyanogenMod Project
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

public class MediaRecorder {
    /**
     * Allows an application to listen passively to the device microphone in the background
     * to detect speech of a hotword or phrase.
     *
     * This is a system|signature permission.
     */
    public static final String CAPTURE_AUDIO_HOTWORD_PERMISSION
            = "android.permission.CAPTURE_AUDIO_HOTWORD";

    public class AudioSource {
        /**
         * Audio source for preemptible, low-priority software hotword detection
         * It presents the same gain and pre processing tuning as
         * {@link android.media.MediaRecorder.AudioSource#VOICE_RECOGNITION}.
         * <p>
         * An application should use this audio source when it wishes to do
         * always-on software hotword detection, while gracefully giving in to any other application
         * that might want to read from the microphone.
         * </p>
         * You must hold {@link cyanogenmod.media.MediaRecorder#CAPTURE_AUDIO_HOTWORD_PERMISSION}
         * to use this audio source.
         */
        public static final int HOTWORD = 1999;
    }
}
