package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.server.SystemService;

import java.util.ArrayList;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.media.AudioSessionInfo;
import cyanogenmod.media.CMAudioManager;
import cyanogenmod.media.ICMAudioService;
import cyanogenmod.platform.Manifest;

public class CMAudioService extends SystemService {

    private static final String TAG = "CMAudioService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;

    private static final int AUDIO_STATUS_OK = 0;

    //keep in sync with include/media/AudioPolicy.h
    private final static int AUDIO_OUTPUT_SESSION_EFFECTS_UPDATE = 10;

    private static boolean sNativeLibraryLoaded;

    static {
        try {
            System.loadLibrary("cmsdk_platform_jni");
            sNativeLibraryLoaded = true;

        } catch (Throwable t) {
            sNativeLibraryLoaded = false;
            Log.w(TAG, "CMSDK native platform unavailable");
        }
    }

    public CMAudioService(Context context) {
        super(context);

        mContext = context;
    }

    @Override
    public void onStart() {
        if (mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.AUDIO)) {
            if (!sNativeLibraryLoaded) {
                Log.wtf(TAG, "CM Audio service started by system server by native library is" +
                             "unavailable. Service will be unavailable.");
            } else {
                publishBinderService(CMContextConstants.CM_AUDIO_SERVICE, mBinder);
            }
        } else {
            Log.wtf(TAG, "CM Audio service started by system server but feature xml not" +
                    " declared. Not publishing binder service!");
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            if (sNativeLibraryLoaded) {
                native_registerAudioSessionCallback(true);
            }
        }
    }

    private final IBinder mBinder = new ICMAudioService.Stub() {

        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
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
    };

    private void broadcastSessionChanged(boolean added, AudioSessionInfo sessionInfo) {
        Intent i = new Intent(CMAudioManager.ACTION_AUDIO_SESSIONS_CHANGED);
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        i.putExtra(CMAudioManager.EXTRA_SESSION_INFO, sessionInfo);
        i.putExtra(CMAudioManager.EXTRA_SESSION_ADDED, added);

        mContext.sendOrderedBroadcast(i, Manifest.permission.OBSERVE_AUDIO_SESSIONS);
    }

    /*
     * Handles events from JNI
     */
    private synchronized void audioSessionCallbackFromNative(int event,
            AudioSessionInfo sessionInfo, boolean added) {

        switch (event) {
            case AUDIO_OUTPUT_SESSION_EFFECTS_UPDATE:
                broadcastSessionChanged(added, sessionInfo);
                break;
            default:
                Log.e(TAG, "Unknown event " + event);
        }
    }

    private native final void native_registerAudioSessionCallback(boolean enabled);

    private native final int native_listAudioSessions(
            int stream, ArrayList<AudioSessionInfo> sessions);
}
