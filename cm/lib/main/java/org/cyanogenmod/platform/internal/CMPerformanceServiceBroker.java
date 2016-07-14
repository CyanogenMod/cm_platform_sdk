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
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManagerInternal;

import org.cyanogenmod.internal.power.IPerformanceManagerProvider;

public class CMPerformanceServiceBroker extends
        BrokerableCMSystemService<IPerformanceManagerProvider> {

    private static final boolean DEBUG = true;
    private static final String TAG = "CMPerformanceServiceBroker";

    private final int POWER_FEATURE_SUPPORTED_PROFILES = 0x00001000;

    private final Context mContext;
    private PowerManagerInternal mPm;
    private int mNumProfiles;

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
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            synchronized (this) {
                mNumProfiles = mPm.getFeature(POWER_FEATURE_SUPPORTED_PROFILES);
                mPm = getLocalService(PowerManagerInternal.class);
                mPm.registerLowPowerModeObserver(mLowPowerModeListener);

                publishLocalService(PerformanceManagerInternal.class,
                        new LocalService());
                publishBinderService(CMContextConstants.CM_PERFORMANCE_SERVICE,
                        new BinderService());
            }
        }
    }

    @Override
    protected IPerformanceManagerProvider getIBinderAsIInterface(@NonNull IBinder service) {
        return IPerformanceManagerProvider.Stub.asInterface(service);
    }

    @Override
    protected IPerformanceManagerProvider getDefaultImplementation() {
        return new IPerformanceManagerProvider.NoOp();
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
        }

        @Override
        public void onBrokeredServiceDisconnected() {
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
            // constant value comes from PackageManagerInternal, so we must look it up here in the
            // system process
            return mNumProfiles;
        }

        @Override
        public boolean getProfileHasAppProfiles(int profile) throws RemoteException {
            return getBrokeredService().getProfileHasAppProfiles(profile);
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
