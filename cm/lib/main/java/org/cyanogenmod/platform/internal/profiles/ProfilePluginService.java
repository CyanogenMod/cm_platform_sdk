package org.cyanogenmod.platform.internal.profiles;

import android.content.Context;
import android.os.IBinder;

import android.os.RemoteException;
import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.profiles.Action;
import cyanogenmod.app.profiles.IProfilePluginService;
import cyanogenmod.app.profiles.Trigger;

import java.util.List;

public class ProfilePluginService extends SystemService {

    public ProfilePluginService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PROFILE_PLUGIN_SERVICE, mService);
    }

    private final IBinder mService = new IProfilePluginService.Stub() {
        public void registerTrigger(Trigger trigger) {
        }
        public void registerAction(Action action) {
        }

        @Override
        public List getRegisteredTriggers() throws RemoteException {
            return null;
        }

        @Override
        public List getRegisteredActions() throws RemoteException {
            return null;
        }
    };
}
