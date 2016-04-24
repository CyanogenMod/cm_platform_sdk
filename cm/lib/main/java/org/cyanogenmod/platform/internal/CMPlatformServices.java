/*
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
import android.os.IBinder;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;

import org.cyanogenmod.internal.ICMPlatform;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cyanogenmod.app.CMContextConstants;

public class CMPlatformServices extends SystemService {

    private static final String TAG = "CMPlatformServices";

    private final List<String> mServices = new ArrayList<String>();

    private final Context mContext;

    public CMPlatformServices(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        final SystemServiceManager ssm = LocalServices.getService(SystemServiceManager.class);

        Slog.i(TAG, "Starting CyanogenMod platform services..");

        String[] externalServices = mContext.getResources().getStringArray(
                org.cyanogenmod.platform.internal.R.array.config_externalCMServices);

        for (String service : externalServices) {
            try {
                Slog.i(TAG, service);
                ssm.startService(service);
                mServices.add(service);
            } catch (Throwable e) {
                reportWtf("starting " + service, e);
            }
        }

        publishBinderService(CMContextConstants.CM_PLATFORM, mBinder);
    }

    private final IBinder mBinder = new ICMPlatform.Stub() {

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println();
            pw.println("CyanogenMod platform services:");
            for (String service : mServices) {
                pw.println("  " + service);
            }
        }
    };

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }
}
