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

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ISettingsManager;

/** {@hide} */
public class SettingsManagerService extends SystemService {

    private static final String TAG = "CMSettingsService";
    // Enable the below for detailed logging of this class
    private static final boolean LOCAL_LOGV = false;

    public static final String PERMISSION_MODIFY_SETTINGS
            = "cyanogenmod.permission.MODIFY_SETTINGS";

    private Context mContext;
    private TelephonyManager mTelephonyManager;

    public SettingsManagerService(Context context) {
        super(context);
        mContext = context;
        mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        publishBinderService(CMContextConstants.CM_SETTINGS_SERVICE, mService);
    }

    @Override
    public void onStart() {

    }

    private void enforceChangePermissions() {
        mContext.enforceCallingOrSelfPermission(PERMISSION_MODIFY_SETTINGS,
                "You do not have permissions to change system settings.");
    }

    private final IBinder mService = new ISettingsManager.Stub() {

        @Override
        public void setAirplaneModeEnabled(boolean enabled) {
            enforceChangePermissions();
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
            enforceChangePermissions();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            setMobileDataEnabledInternal(enabled);
            restoreCallingIdentity(token);
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
}

