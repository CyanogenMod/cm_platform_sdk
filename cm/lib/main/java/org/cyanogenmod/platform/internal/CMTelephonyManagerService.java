/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.platform.internal;

import com.android.server.SystemService;

import android.content.Context;
import android.os.IBinder;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ICMTelephonyManager;

/**
 * Internal service which manages interactions with the phone and data connection
 * @hide
 */
public class CMTelephonyManagerService extends SystemService {
    private static final String TAG = "CMTelephonyManagerService";

    private List<SubscriptionInfo> mSubInfoList = null;

    private int mNumSims;
    private SubscriptionManager mSubscriptionManager;

    public CMTelephonyManagerService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "CM telephony manager service start: " + this);
        mSubscriptionManager = SubscriptionManager.from(mContext);
        publishBinderService(CMContextConstants.CM_TELEPHONY_MANAGER_SERVICE, mService);
    }

    private final IBinder mService = new ICMTelephonyManager.Stub() {

        /**
         * Returns the available SIM subcription information.
         * @hide
         */
        @Override
        public List<SubscriptionInfo> getSimInformation() {
            enforceTelephonyPermission();
            if (mSubInfoList == null) {
                mSubInfoList = getActiveSubscriptionInfoList();

                if(mSubInfoList!=null){
                    mNumSims = mSubInfoList.size();
                }
            }

            return mSubInfoList;
        }
    };

    private List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        Log.d(TAG, "The active subscriptions where obtained from the subscription manager.");
        return subInfoList;
    }

    private void enforceTelephonyPermission() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.VIEW_MODIFY_PHONE_STATE,
                "CMTelephonyManagerService");
    }
}
