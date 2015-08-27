/**
 * Copyright (c) 2015, The CyanogenMod Project
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

package org.cyanogenmod.cmsettings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

public class PreBootReceiver extends BroadcastReceiver{
    private static final String TAG = "CMSettingsReceiver";
    private static final boolean LOCAL_LOGV = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) {
            Log.d(TAG, "Received pre boot intent. Attempting to migrate CM settings.");
        }

        ContentResolver contentResolver = context.getContentResolver();
        IContentProvider contentProvider = contentResolver.acquireProvider(
                CMSettings.AUTHORITY);

        try{
            contentProvider.call(contentResolver.getPackageName(),
                    CMSettings.CALL_METHOD_MIGRATE_SETTINGS, null, null);

            context.getPackageManager().setComponentEnabledSetting(
                    new ComponentName(context, getClass()),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP);
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed to trigger settings migration due to RemoteException");
        }
    }
}
