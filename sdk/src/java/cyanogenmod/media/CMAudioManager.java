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

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cyanogenmod.app.CMContextConstants;

public class CMAudioManager {

    private static final String TAG = "CMAudioManager";

    public static final String ACTION_AUDIO_SESSIONS_CHANGED =
            "cyanogenmod.intent.action.ACTION_AUDIO_SESSIONS_CHANGED";

    public static final String EXTRA_SESSION_INFO = "session_info";
    public static final String EXTRA_SESSION_ADDED = "added";

    private Context mContext;

    private static CMAudioManager sInstance;
    private static ICMAudioService sService;

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    private CMAudioManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;

        }
        sService = getService();

        if (!context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.AUDIO) || !checkService()) {
            throw new RuntimeException("Unable to get CMAudioService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.media.CMAudioManager}
     * @param context
     * @return {@link CMAudioManager}
     */
    public synchronized static CMAudioManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CMAudioManager(context);
        }
        return sInstance;
    }

    /** @hide */
    public static ICMAudioService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_AUDIO_SERVICE);
        if (b != null) {
            sService = ICMAudioService.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    /**
     * @return true if service is valid
     */
    private boolean checkService() {
        if (sService == null) {
            Log.w(TAG, "not connected to CMAudioService");
            return false;
        }
        return true;
    }

    /**
     * List all audio sessions for the given stream type.
     *
     * Use AUDIO_STREAM_DEFAULT (-1) for all sessions.
     *
     * @param streamType
     * @return the current active audio sessions
     * @see android.media.AudioSystem
     */
    public List<AudioSessionInfo> listAudioSessions(int streamType) {
        if (checkService()) {
            try {
                final List<AudioSessionInfo> sessions = sService.listAudioSessions(streamType);
                if (sessions != null) {
                    return Collections.unmodifiableList(sessions);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to list audio sessions!", e);
            }
        }
        return Collections.emptyList();
    }
}
