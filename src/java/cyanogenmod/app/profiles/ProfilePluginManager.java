package cyanogenmod.app.profiles;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.profiles.IProfilePluginService;

import java.util.List;

public class ProfilePluginManager {
    private static IProfilePluginService sService;
    private static ProfilePluginManager sProfilePluginManagerInstance;
    private Context mContext;

    public ProfilePluginManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.ProfilePluginManager}
     * @param context
     * @return {@link cyanogenmod.app.profiles.ProfilePluginManager}
     */
    public static ProfilePluginManager getInstance(Context context) {
        if (sProfilePluginManagerInstance == null) {
            sProfilePluginManagerInstance = new ProfilePluginManager(context);
        }
        return sProfilePluginManagerInstance;
    }

    public void registerTrigger(Trigger trigger) {
        IProfilePluginService service = getService();
        try {
            service.registerTrigger(trigger);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    public void registerAction(Action action) {
        IProfilePluginService service = getService();
        try {
            service.registerAction(action);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /** hide */
    public List<Action> getRegisteredActions() {
        IProfilePluginService service = getService();
        try {
            return service.getRegisteredActions();
        } catch (RemoteException e) {
            //Unable to get service, fail
            return null;
        }
    }

    /** hide */
    public List<Trigger> getRegisteredTriggers() {
        IProfilePluginService service = getService();
        try {
            return service.getRegisteredTriggers();
        } catch (RemoteException e) {
            //Unable to get service, fail
            return null;
        }
    }

    public void sendTrigger(String triggerId, String state) {
        IProfilePluginService service = getService();
        try {
            service.sendTrigger(triggerId, state);
        } catch (RemoteException e) {
            //Unable to get service, fail
        }
    }

    /** @hide */
    public IProfilePluginService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_PROFILE_PLUGIN_SERVICE);
        sService = IProfilePluginService.Stub.asInterface(b);
        return sService;
    }
}
