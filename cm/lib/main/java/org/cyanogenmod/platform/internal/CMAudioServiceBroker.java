/**
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

public class CMAudioServiceBroker extends BrokerableCMSystemService<ICMAudioService> {

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
    protected ICMAudioService getIBinderAsIInterface(@NonNull IBinder service) {
        return ICMAudioService.Stub.asInterface(service);
    }

    @Override
    protected ICMAudioService getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    private void checkPermission() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.OBSERVE_AUDIO_SESSIONS, null);
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.AUDIO;
    }

    private final ICMAudioService mServiceStubForFailure = new ICMAudioService.Stub() {
        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
            checkPermission();
            return Collections.emptyList();
        }
    };

    private final class BinderService extends ICMAudioService.Stub {
        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
            checkPermission();
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
