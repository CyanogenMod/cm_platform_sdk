/**
 * Copyright (C) 2016 The CyanogenMod Project
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

import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;

/**
 * Base CM System Server which handles the starting and states of various CM
 * specific system services. Since its part of the main looper provided by the system
 * server, it will be available indefinitely (until all the things die).
 */
public class CMSystemServer {
    private static final String TAG = "CMSystemServer";
    private Context mSystemContext;

    public CMSystemServer(Context systemContext) {
        mSystemContext = systemContext;
    }

    /**
     * Invoked via reflection by the SystemServer
     */
    private void run() {
        // Start services.
        try {
            startServices();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting cm system services", ex);
            throw ex;
        }
    }

    private void startServices() {
        final Context context = mSystemContext;
        final SystemServiceManager ssm = LocalServices.getService(SystemServiceManager.class);
        String[] externalServices = context.getResources().getStringArray(
                org.cyanogenmod.platform.internal.R.array.config_externalCMServices);

        //TODO: Move feature asserts out of core services and to here
        for (String service : externalServices) {
            try {
                Slog.i(TAG, service);
                ssm.startService(service);
            } catch (Throwable e) {
                reportWtf("starting " + service , e);
            }
        }
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }
}
