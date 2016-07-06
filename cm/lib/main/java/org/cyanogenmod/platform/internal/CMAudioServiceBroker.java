package org.cyanogenmod.platform.internal;

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

import android.annotation.NonNull;
import android.annotation.SdkConstant;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.media.AudioSessionInfo;
import cyanogenmod.media.ICMAudioService;
import org.cyanogenmod.internal.media.ICMAudioServiceProvider;

public class CMAudioServiceBroker extends BrokerableCMSystemService<ICMAudioServiceProvider> {

    private static final String TAG = "CMAudioServiceBroker";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmaudio.service",
                    "org.cyanogenmod.cmaudio.service.CMAudioService");

    public CMAudioServiceBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == PHASE_THIRD_PARTY_APPS_CAN_START) {
            publishBinderService(CMContextConstants.CM_AUDIO_SERVICE, new BinderService());
        }
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
    }

    @Override
    protected ICMAudioServiceProvider getIBinderAsIInterface(@NonNull IBinder service) {
        return ICMAudioServiceProvider.Stub.asInterface(service);
    }

    @Override
    protected ICMAudioServiceProvider getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.AUDIO;
    }

    private final ICMAudioServiceProvider mServiceStubForFailure =
            new ICMAudioServiceProvider.Stub() {
        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
            return Collections.emptyList();
        }
    };

    private final class BinderService extends ICMAudioService.Stub {
        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
            return getBrokeredService().listAudioSessions(streamType);
        }

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);

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
    }
}
