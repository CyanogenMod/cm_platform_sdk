package org.cyanogenmod.platform.internal.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.PendingIntent;
import android.app.ProfileManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.profiles.Action;
import cyanogenmod.app.profiles.IProfilePluginService;
import cyanogenmod.app.profiles.Trigger;

import java.util.List;

public class ProfilePluginService extends SystemService {

    private Map<String, Trigger> mTriggers = Collections.synchronizedMap(new HashMap<String, Trigger>());
    private Map<String, Action> mActions = Collections.synchronizedMap(new HashMap<String, Action>());
    private PackageManager mPackageManager;

    public ProfilePluginService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PROFILE_PLUGIN_SERVICE, mService);
        mPackageManager = getContext().getPackageManager();
    }

    private String constructMapKey(String id) {
        return getPackageNameForCaller() + id;
    }

    private String getPackageNameForCaller() {
        return mPackageManager.getNameForUid(Binder.getCallingUid());
    }

    private final IBinder mService = new IProfilePluginService.Stub() {
        public void registerTrigger(Trigger trigger) {
            trigger.setPackage(getPackageNameForCaller());
            String triggerKey = constructMapKey(trigger.getTriggerId());
            mTriggers.put(triggerKey, trigger);
            System.out.println("registerTrigger " + trigger);
        }

        public void registerAction(Action action) {
            action.setPackage(getPackageNameForCaller());
            String actionKey = constructMapKey(action.getKey());
            mActions.put(actionKey, action);
        }

        @Override
        public List getRegisteredTriggers() throws RemoteException {
            return new ArrayList<Trigger>(mTriggers.values());
        }

        @Override
        public List getRegisteredActions() throws RemoteException {
            return new ArrayList<Action>(mActions.values());
        }

        @Override
        public void sendTrigger(String triggerId, String state) {
            String triggerKey = constructMapKey(triggerId);
            Trigger trigger = mTriggers.get(triggerKey);
            if (trigger != null) {
                ProfileManager profileManager = (ProfileManager)
                        getContext().getSystemService(Context.PROFILE_SERVICE);
                profileManager.sendTrigger(trigger.getTriggerId(), state);
            }
        }

        @Override
        public void fireAction(String actionId, String state) {
            Action action = mActions.get(actionId);
            if (action != null) {
                try {
                    action.getAction().send();
                } catch (PendingIntent.CanceledException e) {
                    // Ignore cancelled
                }
            }
        }
    };
}
