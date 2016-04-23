package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
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

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println();
            pw.println("CMAudio Service State:");
            try {
                List<AudioSessionInfo> sessions = listAudioSessions(-1);
                if (sessions.size() > 0) {
                    pw.println("  Audio sessions:");
                    for (AudioSessionInfo info : sessions) {
                        pw.println("   " + info.toString());
                    }
                } else {
                    pw.println("  No active audio sessions");
                }
            } catch (RemoteException e) {
                // nothing
            }
        }
    };

    private void broadcastSessionChanged(boolean added, AudioSessionInfo sessionInfo) {
        Intent i = new Intent(CMAudioManager.ACTION_AUDIO_SESSIONS_CHANGED);
        i.putExtra(CMAudioManager.EXTRA_SESSION_INFO, sessionInfo);
        i.putExtra(CMAudioManager.EXTRA_SESSION_ADDED, added);

        sendBroadcastToAll(i, Manifest.permission.OBSERVE_AUDIO_SESSIONS);
    }

    private void sendBroadcastToAll(Intent intent, String receiverPermission) {
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        final long ident = Binder.clearCallingIdentity();
        try {
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL, receiverPermission);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
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
