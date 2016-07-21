/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
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

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.hardware.ICMHardwareService;
import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.DisplayMode;
import cyanogenmod.hardware.IThermalListenerCallback;
import cyanogenmod.hardware.ThermalListenerCallback;

import java.util.Arrays;

import org.cyanogenmod.hardware.ThermalMonitor;
import org.cyanogenmod.platform.internal.common.BrokeredServiceConnection;

/** @hide */
public class CMHardwareServiceBroker extends
        BrokerableCMSystemService<ICMHardwareService> {

    private static final boolean DEBUG = true;
    private static final String TAG = "CMHardwareBroker";

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmhardware.service",
                    "org.cyanogenmod.cmhardware.service.CMHardwareService");

    private Context mContext;

    private final ICMHardwareService mServiceStubForFailure =
            new ICMHardwareService.NoOp();

    public CMHardwareServiceBroker(Context context) {
        super(context);
        mContext = context;
        setBrokeredServiceConnection(mServiceConnection);
        Log.d(TAG, "CMHardwareServiceBroker() called with: " + "context = [" + context + "]");
    }

    private BrokeredServiceConnection mServiceConnection = new BrokeredServiceConnection() {
        @Override
        public void onBrokeredServiceConnected() {
            Log.d(TAG, "onBrokeredServiceConnected() called with: " + "");
        }

        @Override
        public void onBrokeredServiceDisconnected() {
            Log.d(TAG, "onBrokeredServiceDisconnected() called with: " + "");
        }
    };

    @Override
    protected ICMHardwareService getIBinderAsIInterface(@NonNull IBinder service) {
        return ICMHardwareService.Stub.asInterface(service);
    }

    @Override
    protected ICMHardwareService getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }


    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.HARDWARE_ABSTRACTION;
    }

    @Override
    public void onBootPhase(int phase) {
        Log.d(TAG, "onBootPhase() called with: " + "phase = [" + phase + "]");
        if (phase == PHASE_BOOT_COMPLETED) {
            publishBinderService(CMContextConstants.CM_HARDWARE_SERVICE, mService);

            Intent intent = new Intent(cyanogenmod.content.Intent.ACTION_INITIALIZE_CM_HARDWARE);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcast(intent,
                    cyanogenmod.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS);
        }
    }


    @Override
    public void onStart() {
        Log.d(TAG, "onStart() called with: " + "");
    }

    private final IBinder mService = new ICMHardwareService.Stub() {

        @Override
        public int getSupportedFeatures() throws RemoteException {
            checkPermission();
            return getBrokeredService().getSupportedFeatures();
        }

        @Override
        public boolean get(int feature) throws RemoteException {
            checkPermission();
            return getBrokeredService().get(feature);
        }

        @Override
        public boolean set(int feature, boolean enable) throws RemoteException {
            checkPermission();
            return getBrokeredService().set(feature, enable);
        }

        @Override
        public int[] getDisplayColorCalibration() throws RemoteException {
            checkPermission();
            return getBrokeredService().getDisplayColorCalibration();
        }

        @Override
        public boolean setDisplayColorCalibration(int[] rgb) throws RemoteException {
            checkPermission();
            return getBrokeredService().setDisplayColorCalibration(rgb);
        }

        @Override
        public int getNumGammaControls() throws RemoteException {
            checkPermission();
            return getBrokeredService().getNumGammaControls();
        }

        @Override
        public int[] getDisplayGammaCalibration(int idx) throws RemoteException {
            checkPermission();
            return getBrokeredService().getDisplayGammaCalibration(idx);
        }

        @Override
        public boolean setDisplayGammaCalibration(int idx, int[] rgb) throws RemoteException {
            checkPermission();
            return getBrokeredService().setDisplayGammaCalibration(idx, rgb);
        }

        @Override
        public int[] getVibratorIntensity() throws RemoteException {
            checkPermission();
            return getBrokeredService().getVibratorIntensity();
        }

        @Override
        public boolean setVibratorIntensity(int intensity) throws RemoteException {
            checkPermission();
            return getBrokeredService().setVibratorIntensity(intensity);
        }

        @Override
        public String getLtoSource() throws RemoteException {
            checkPermission();
            return getBrokeredService().getLtoSource();
        }

        @Override
        public String getLtoDestination() throws RemoteException {
            checkPermission();
            return getBrokeredService().getLtoDestination();
        }

        @Override
        public long getLtoDownloadInterval() throws RemoteException {
            checkPermission();
            return getBrokeredService().getLtoDownloadInterval();
        }

        @Override
        public String getSerialNumber() throws RemoteException {
            checkPermission();
            return getBrokeredService().getSerialNumber();
        }

        @Override
        public String getUniqueDeviceId() throws RemoteException {
            checkPermission();
            return getBrokeredService().getUniqueDeviceId();
        }

        @Override
        public boolean requireAdaptiveBacklightForSunlightEnhancement() throws RemoteException {
            checkPermission();
            return getBrokeredService().requireAdaptiveBacklightForSunlightEnhancement();
        }

        @Override
        public boolean isSunlightEnhancementSelfManaged() throws RemoteException {
            checkPermission();
            return getBrokeredService().isSunlightEnhancementSelfManaged();
        }

        @Override
        public DisplayMode[] getDisplayModes() throws RemoteException {
            checkPermission();
            return getBrokeredService().getDisplayModes();
        }

        @Override
        public DisplayMode getCurrentDisplayMode() throws RemoteException {
            checkPermission();
            return getBrokeredService().getCurrentDisplayMode();
        }

        @Override
        public DisplayMode getDefaultDisplayMode() throws RemoteException {
            checkPermission();
            return getBrokeredService().getDefaultDisplayMode();
        }

        @Override
        public boolean setDisplayMode(DisplayMode mode,
                boolean makeDefault) throws RemoteException {
            checkPermission();
            return getBrokeredService().setDisplayMode(mode, makeDefault);
        }

        @Override
        public boolean writePersistentBytes(String key, byte[] value) throws RemoteException {
            checkPermission();
            return getBrokeredService().writePersistentBytes(key, value);
        }

        @Override
        public byte[] readPersistentBytes(String key) throws RemoteException {
            checkPermission();
            return getBrokeredService().readPersistentBytes(key);
        }

        @Override
        public int getThermalState() throws RemoteException {
            checkPermission();
            return getBrokeredService().getThermalState();
        }

        @Override
        public boolean registerThermalListener(
                IThermalListenerCallback callback) throws RemoteException {
            checkPermission();
            return getBrokeredService().registerThermalListener(callback);
        }

        @Override
        public boolean unRegisterThermalListener(
                IThermalListenerCallback callback) throws RemoteException {
            checkPermission();
            return getBrokeredService().unRegisterThermalListener(callback);
        }
    };

    private void checkPermission() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
    }
}
