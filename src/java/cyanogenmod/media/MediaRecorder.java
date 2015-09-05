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

    /**
     * <p>Broadcast Action: The state of the HOTWORD audio input has changed.:</p>
     * <ul>
     *   <li><em>state</em> - A String value indicating the state of the input.
     *   {@link #EXTRA_HOTWORD_INPUT_STATE}. The value will be one of:
     *   {@link android.media.AudioRecord#RECORDSTATE_RECORDING} or
     *   {@link android.media.AudioRecord#RECORDSTATE_STOPPED}.
     *   </li>
     *   <li><em>package</em> - A String value indicating the package name of the application
     *   that currently holds the HOTWORD input.
     *   {@link #EXTRA_CURRENT_PACKAGE_NAME}
     *   </li>

     * </ul>
     *
     * <p class="note">This is a protected intent that can only be sent
     * by the system. It can only be received by packages that hold
     * {@link android.Manifest.permission#CAPTURE_AUDIO_HOTWORD}.
     */
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HOTWORD_INPUT_CHANGED
            = "com.cyanogenmod.intent.action.HOTWORD_INPUT_CHANGED";

    /**
     * Extra for {@link #ACTION_HOTWORD_INPUT_CHANGED} that provides the package name of the
     * app in that controlled the HOTWORD input when the state changed. Can be reused for other
     * purposes.
     */
    public static final String EXTRA_CURRENT_PACKAGE_NAME =
            "com.cyanogenmod.intent.extra.CURRENT_PACKAGE_NAME";

    /**
     * Extra for {@link #ACTION_HOTWORD_INPUT_CHANGED} that provides the state of
     * the input when the broadcast action was sent.
     * @hide
     */
    public static final String EXTRA_HOTWORD_INPUT_STATE =
            "com.cyanogenmod.intent.extra.HOTWORD_INPUT_STATE";


    public static class AudioSource {
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
