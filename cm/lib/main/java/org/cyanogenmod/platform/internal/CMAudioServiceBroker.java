package org.cyanogenmod.platform.internal;

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.media.AudioSessionInfo;
import cyanogenmod.media.ICMAudioServiceProvider;
import cyanogenmod.media.ICMAudioService;

public class CMAudioServiceBroker extends BrokerableCMSystemService<ICMAudioServiceProvider> {

    private static final boolean DEBUG = true;
    private static final String TAG = "CMAudioServiceBroker";

    private final Context mContext;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmaudio.service",
                    "org.cyanogenmod.cmaudio.service.CMAudioService");

    public CMAudioServiceBroker(Context context) {
        super(context);
        mContext = context;
    }


    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
        setBrokeredServiceConnection(mServiceConnection);
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

    private BrokeredServiceConnection mServiceConnection = new BrokeredServiceConnection() {
        @Override
        public void onBrokeredServiceConnected() {
            // no action
        }

        @Override
        public void onBrokeredServiceDisconnected() {
            // no action
        }
    };

    private final ICMAudioServiceProvider mServiceStubForFailure = new ICMAudioServiceProvider() {
        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
            return Collections.emptyList();
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };

    private final class BinderService extends ICMAudioService.Stub {
        @Override
        public List<AudioSessionInfo> listAudioSessions(int streamType) throws RemoteException {
            return getBrokeredService().listAudioSessions(streamType);
        }
    }
}
