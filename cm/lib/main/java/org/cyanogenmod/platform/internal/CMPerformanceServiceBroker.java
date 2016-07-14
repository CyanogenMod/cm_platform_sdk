package org.cyanogenmod.platform.internal;

import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.power.IPerformanceManager;
import cyanogenmod.power.PerformanceManagerInternal;

import org.cyanogenmod.internal.power.IPerformanceManagerProvider;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class CMPerformanceServiceBroker extends
        BrokerableCMSystemService<IPerformanceManagerProvider> {

    private static final String TAG = "CMPerformanceBroker";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int POWER_FEATURE_SUPPORTED_PROFILES = 0x00001000;

    private final Context mContext;
    private int mNumProfiles;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmperformance.service",
                    "org.cyanogenmod.cmperformance.service.PerformanceManagerService");

    public CMPerformanceServiceBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");
    }

    @Override
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            PowerManagerInternal pm = getLocalService(PowerManagerInternal.class);

            mNumProfiles = pm.getFeature(POWER_FEATURE_SUPPORTED_PROFILES);
            pm.registerLowPowerModeObserver(mLowPowerModeListener);

            publishLocalService(PerformanceManagerInternal.class,
                    new LocalService());
            publishBinderService(CMContextConstants.CM_PERFORMANCE_SERVICE,
                    new BinderService());
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

    private void checkPermission() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.PERFORMANCE_ACCESS, null);
    }

    private final class BinderService extends IPerformanceManager.Stub {
        @Override
        public void cpuBoost(int duration) throws RemoteException {
            checkPermission();
            getBrokeredService().cpuBoost(duration);
        }

        @Override
        public boolean setPowerProfile(int profile) throws RemoteException {
            checkPermission();
            return getBrokeredService().setPowerProfile(profile);
        }

        @Override
        public int getPowerProfile() throws RemoteException {
            checkPermission();
            return getBrokeredService().getPowerProfile();
        }

        @Override
        public int getNumberOfProfiles() throws RemoteException {
            checkPermission();
            // constant value comes from PackageManagerInternal, so we must look it up here in the
            // system process
            return mNumProfiles;
        }

        @Override
        public boolean getProfileHasAppProfiles(int profile) throws RemoteException {
            checkPermission();
            return getBrokeredService().getProfileHasAppProfiles(profile);
        }


        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);

            pw.println("---" + TAG + " state---");
            try {
                pw.println("Service instance: " + getBrokeredService());
                pw.println("Number of profiles: " + getNumberOfProfiles());
                pw.println("Current profile: " + getPowerProfile());
            } catch (RemoteException e) {
                // ignored
            }

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
