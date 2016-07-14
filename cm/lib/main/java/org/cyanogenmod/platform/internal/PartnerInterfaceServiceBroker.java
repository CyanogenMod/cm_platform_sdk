/*
 * Copyright (c) 2016 CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.platform.internal;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.IPartnerInterface;
import cyanogenmod.app.PartnerInterface;
import cyanogenmod.media.MediaRecorder;

public class PartnerInterfaceServiceBroker extends BrokerableCMSystemService<IPartnerInterface> {

    private static final boolean DEBUG = true;
    private static final String TAG = "PartnerInterfaceServiceBroker";

    private final Context mContext;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.partnerinterfaceservice",
                    "org.cyanogenmod.partnerinterfaceservice.PartnerInterfaceService");


    public PartnerInterfaceServiceBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public IPartnerInterface getIBinderAsIInterface(IBinder service) {
        return IPartnerInterface.Stub.asInterface(service);
    }

    @Override
    protected IPartnerInterface getDefaultImplementation() {
        return mFailureImpl;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.PARTNER;
    }

    @Override
    public void onStart() {
        publishService(new BinderService());
    }

    public void publishService(IBinder binder) {
        publishBinderService(CMContextConstants.CM_PARTNER_INTERFACE, binder);
    }

    private void enforceModifyNetworkSettingsPermission() {
        mContext.enforceCallingOrSelfPermission(PartnerInterface.MODIFY_NETWORK_SETTINGS_PERMISSION,
                "You do not have permissions to change system network settings.");
    }

    private void enforceModifySoundSettingsPermission() {
        mContext.enforceCallingOrSelfPermission(PartnerInterface.MODIFY_SOUND_SETTINGS_PERMISSION,
                "You do not have permissions to change system sound settings.");
    }

    private void enforceShutdownPermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.REBOOT,
                "You do not have permissions to shut down the device.");
    }

    private void enforceCaptureHotwordPermission() {
        mContext.enforceCallingOrSelfPermission(MediaRecorder.CAPTURE_AUDIO_HOTWORD_PERMISSION,
                "You do not have permission to query the hotword input package name.");
    }

    private class BinderService extends IPartnerInterface.Stub {
        @Override
        public void setAirplaneModeEnabled(boolean enabled) throws RemoteException {
            enforceModifyNetworkSettingsPermission();
            getBrokeredService().setAirplaneModeEnabled(enabled);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) throws RemoteException {
            enforceModifyNetworkSettingsPermission();
            getBrokeredService().setMobileDataEnabled(enabled);
        }

        @Override
        public void shutdown() throws RemoteException {
            enforceShutdownPermission();
            getBrokeredService().shutdown();
        }

        @Override
        public void reboot() throws RemoteException {
            enforceShutdownPermission();
            getBrokeredService().reboot();
        }

        @Override
        public boolean setZenMode(int mode) throws RemoteException {
            enforceModifySoundSettingsPermission();
            return getBrokeredService().setZenMode(mode);
        }

        @Override
        public boolean setZenModeWithDuration(int mode, long durationMillis) throws RemoteException {
            enforceModifySoundSettingsPermission();
            return getBrokeredService().setZenModeWithDuration(mode, durationMillis);
        }

        @Override
        public String getCurrentHotwordPackageName() throws RemoteException {
            enforceCaptureHotwordPermission();
            return getBrokeredService().getCurrentHotwordPackageName();
        }
    }

    private IPartnerInterface mFailureImpl = new IPartnerInterface.Stub() {
        @Override
        public void setAirplaneModeEnabled(boolean enabled) throws RemoteException {
            // no-op
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) throws RemoteException {
            // no-op
        }

        @Override
        public boolean setZenMode(int mode) throws RemoteException {
            return false;
        }

        @Override
        public void shutdown() throws RemoteException {
            // no-op
        }

        @Override
        public void reboot() throws RemoteException {
            // no-op
        }

        @Override
        public String getCurrentHotwordPackageName() throws RemoteException {
            return null;
        }

        @Override
        public boolean setZenModeWithDuration(int mode, long durationMillis) throws RemoteException {
            return false;
        }
    };

}
