package org.cyanogenmod.platform.internal;

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.util.Slog;

import java.util.Collections;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.power.PerformanceManager;
import cyanogenmod.power.IPerformanceManagerProvider;
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManagerInternal;

public class CMPerformanceServiceBroker extends
        BrokerableCMSystemService<IPerformanceManagerProvider> {

    private static final boolean DEBUG = true;
    private static final String TAG = "CMPerformanceServiceBroker";

    private final Context mContext;
    private PowerManagerInternal mPm;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmperformance.service",
                    "org.cyanogenmod.cmperformance.service.PerformanceManagerService");

    public CMPerformanceServiceBroker(Context context) {
        super(context);
        mContext = context;
        setBrokeredServiceConnection(mServiceConnection);
    }


    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
        publishBinderService(CMContextConstants.CM_PERFORMANCE_SERVICE, new BinderService());
        publishLocalService(PerformanceManagerInternal.class, new LocalService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            synchronized (this) {
                mPm = getLocalService(PowerManagerInternal.class);
//                mNumProfiles = mPm.getFeature(POWER_FEATURE_SUPPORTED_PROFILES);
//                if (mNumProfiles > 0) {
//                    int profile = getUserProfile();
//                    if (profile == PerformanceManager.PROFILE_HIGH_PERFORMANCE) {
//                        Slog.i(TAG, String.format("Reverting profile %d to %d",
//                                profile, PerformanceManager.PROFILE_BALANCED));
//                        setPowerProfileInternal(
//                                PerformanceManager.PROFILE_BALANCED, true);
//                    } else {
//                        setPowerProfileInternal(profile, false);
//                    }

                mPm.registerLowPowerModeObserver(mLowPowerModeListener);
            }
        }
    }

    @Override
    protected IPerformanceManagerProvider getIBinderAsIInterface(@NonNull IBinder service) {
        return IPerformanceManagerProvider.Stub.asInterface(service);
    }

    @Override
    protected IPerformanceManagerProvider getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.PERFORMANCE;
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

    private final IPerformanceManagerProvider mServiceStubForFailure = new IPerformanceManagerProvider() {
        @Override
        public void cpuBoost(int duration) {
            // do nothing
        }

        @Override
        public boolean setPowerProfile(int profile) {
            // do nothing
            return false;
        }

        @Override
        public int getPowerProfile() {
            return PerformanceManager.PROFILE_BALANCED;
        }

        @Override
        public int getNumberOfProfiles() {
            return 1;
        }

        @Override
        public boolean getProfileHasAppProfiles(int profile) {
            return false;
        }

        @Override
        public void launchBoost(int pid, String packageName) {
            // hit local service?
        }

        @Override
        public void activityResumed(Intent intent) {
            // hit local service?
        }

        @Override
        public void onLowPowerModeChanged(boolean on) {
            // do nothing
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };

    private final class BinderService extends IPerformanceManager.Stub {
        @Override
        public void cpuBoost(int duration) throws RemoteException {
            getBrokeredService().cpuBoost(duration);
        }

        @Override
        public boolean setPowerProfile(int profile) throws RemoteException {
            return getBrokeredService().setPowerProfile(profile);
        }

        @Override
        public int getPowerProfile() throws RemoteException {
            return getBrokeredService().getPowerProfile();
        }

        @Override
        public int getNumberOfProfiles() throws RemoteException {
            return getBrokeredService().getNumberOfProfiles();
        }

        @Override
        public boolean getProfileHasAppProfiles(int profile) throws RemoteException {
            return getBrokeredService().getProfileHasAppProfiles(profile);
        }

        @Override
        public void launchBoost(int pid, String packageName) throws RemoteException {
            // hit local service?
        }

        @Override
        public void activityResumed(Intent intent) throws RemoteException {
            // hit local service?
        }

        @Override
        public void onLowPowerModeChanged(boolean on) throws RemoteException {
             getBrokeredService().onLowPowerModeChanged(on);
        }

    }

    private final class LocalService implements PerformanceManagerInternal {

        @Override
        public void activityResumed(Intent intent) {
            try {
                getBrokeredService().activityResumed(intent);
            } catch (RemoteException e) {
                // ignored
            }
        }

        @Override
        public void cpuBoost(int duration) {
            try {
                getBrokeredService().cpuBoost(duration);
            } catch (RemoteException e) {
                // ignored
            }
        }

        @Override
        public void launchBoost(int pid, String packageName) {
            try {
                getBrokeredService().launchBoost(pid, packageName);
            } catch (RemoteException e) {
                // ignored
            }
        }
    }
//    private final class LocalService implements PerformanceManagerInternal {
//
//        @Override
//        public void cpuBoost(int duration) {
//            cpuBoostInternal(duration);
//        }
//
//        @Override
//        public void launchBoost(int pid, String packageName) {
//            synchronized (CMPerformanceServiceBroker.this) {
//                if (mPm == null) {
//                    Slog.e(TAG, "System is not ready, dropping launch boost request");
//                    return;
//                }
//            }
//            // Don't send boosts if we're in another power profile
//            if (mCurrentProfile == PerformanceManager.PROFILE_POWER_SAVE ||
//                    mCurrentProfile == PerformanceManager.PROFILE_HIGH_PERFORMANCE) {
//                return;
//            }
//            mHandler.obtainMessage(MSG_LAUNCH_BOOST, pid, 0, packageName).sendToTarget();
//        }
//
//        @Override
//        public void activityResumed(Intent intent) {
//            String activityName = null;
//            if (intent != null) {
//                final ComponentName cn = intent.getComponent();
//                if (cn != null) {
//                    activityName = cn.flattenToString();
//                }
//            }
//
//            mCurrentActivityName = activityName;
//            applyProfile(false);
//        }
//    }

    private final PowerManagerInternal.LowPowerModeListener mLowPowerModeListener = new
            PowerManagerInternal.LowPowerModeListener() {

                @Override
                public void onLowPowerModeChanged(boolean enabled) {
                    try {
                        getBrokeredService().onLowPowerModeChanged(enabled);
                    } catch (RemoteException e) {
                        // ignored
                    }
                }
           };

}
