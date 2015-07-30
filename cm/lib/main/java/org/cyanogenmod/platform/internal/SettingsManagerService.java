/*
 * Copyright (c) 2011-2015 CyanogenMod Project
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

import android.app.INotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ISettingsManager;
import cyanogenmod.app.SettingsManager;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

/** {@hide} */
public class SettingsManagerService extends SystemService {

    private static final String TAG = "CMSettingsService";

    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private INotificationManager mNotificationManager;

    public SettingsManagerService(Context context) {
        super(context);
        mContext = context;
        publishBinderService(CMContextConstants.CM_SETTINGS_SERVICE, mService);
    }

    @Override
    public void onStart() {
        mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
    }

    private void enforceModifyNetworkSettingsPermission() {
        mContext.enforceCallingOrSelfPermission(SettingsManager.MODIFY_NETWORK_SETTINGS_PERMISSION,
                "You do not have permissions to change system network settings.");
    }

    private void enforceModifySoundSettingsPermission() {
        mContext.enforceCallingOrSelfPermission(SettingsManager.MODIFY_SOUND_SETTINGS_PERMISSION,
                "You do not have permissions to change system sound settings.");
    }

    private void enforceShutdownPermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.REBOOT,
                "You do not have permissions to shut down the device.");
    }

    private final IBinder mService = new ISettingsManager.Stub() {

        @Override
        public void setAirplaneModeEnabled(boolean enabled) {
            enforceModifyNetworkSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            setAirplaneModeEnabledInternal(enabled);
            restoreCallingIdentity(token);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            enforceModifyNetworkSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            setMobileDataEnabledInternal(enabled);
            restoreCallingIdentity(token);
        }

        @Override
        public void shutdown() {
            enforceShutdownPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            shutdownInternal(false);
            restoreCallingIdentity(token);
        }

        @Override
        public void reboot() {
            enforceShutdownPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            shutdownInternal(true);
            restoreCallingIdentity(token);
        }

        @Override
        public boolean setZenMode(int mode) {
            enforceModifySoundSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            boolean success = setZenModeInternal(mode);
            restoreCallingIdentity(token);
            return success;
        }
    };

    private void setAirplaneModeEnabledInternal(boolean enabled) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                enabled ? 1 : 0);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabled);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void setMobileDataEnabledInternal(boolean enabled) {
        mTelephonyManager.setDataEnabled(enabled);
    }

    private void shutdownInternal(boolean reboot) {
        IPowerManager pm = IPowerManager.Stub.asInterface(
                        ServiceManager.getService(Context.POWER_SERVICE));
        try {
            if (reboot) {
                pm.reboot(false, null, false);
            } else {
                pm.shutdown(false, false);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Unable to shutdown.");
        }
    }

    private boolean setZenModeInternal(int mode) {
        ContentResolver contentResolver = mContext.getContentResolver();
        int zenModeValue = -1;
        switch(mode) {
            case SettingsManager.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                zenModeValue = Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                break;
            case SettingsManager.ZEN_MODE_OFF:
                zenModeValue = Settings.Global.ZEN_MODE_OFF;
                break;
            case SettingsManager.ZEN_MODE_NO_INTERRUPTIONS:
                zenModeValue = Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;
                break;
            default:
                // Invalid mode parameter
                Log.w(TAG, "setZenMode() called with invalid mode: " + mode);
                return false;
        }
        Settings.Global.putInt(contentResolver,
                Settings.Global.ZEN_MODE,
                zenModeValue);
        try {
            // Setting the exit condition to null signifies "indefinitely"
            mNotificationManager.setZenModeCondition(null);
        } catch (RemoteException e) {
            // An error occurred, return false since the
            // condition failed to set.
            Log.e(TAG, "setZenMode() failed for mode: " + mode);
            return false;
        }
        return true;
    }
}

