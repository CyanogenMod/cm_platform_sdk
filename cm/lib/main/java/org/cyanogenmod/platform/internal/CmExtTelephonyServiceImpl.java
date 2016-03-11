/**
 * Copyright (c) 2016, The CyanogenMod Project
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
import android.telephony.Rlog;
import android.util.Log;

import com.android.internal.telephony.IExtTelephony;
import android.telephony.TelephonyManager;
import cyanogenmod.app.CMContextConstants;

/**
 * This class implements IExtTelephony aidl interface.
 * @hide
 */
public class CmExtTelephonyServiceImpl extends SystemService {
    private static final String LOG_TAG = "CmExtTelephonyServiceImpl";
    private static final boolean DBG = true;

    private Context mContext;
    private TelephonyManager mTelephonyManager;

    private final IBinder mService = new IExtTelephony.Stub() {
        @Override
        public int getCurrentUiccCardProvisioningStatus(int slotId) {
            return CmExtTelephonyServiceImpl.this.getCurrentUiccCardProvisioningStatus(slotId);
        }

        @Override
        public int getUiccCardProvisioningUserPreference(int slotId) {
            return CmExtTelephonyServiceImpl.this.getUiccCardProvisioningUserPreference(slotId);
        }

        @Override
        public int activateUiccCard(int slotId) {
            return CmExtTelephonyServiceImpl.this.activateUiccCard(slotId);
        }

        @Override
        public int deactivateUiccCard(int slotId) {
            return CmExtTelephonyServiceImpl.this.deactivateUiccCard(slotId);
        }

        @Override
        public boolean isSMSPromptEnabled() {
            return CmExtTelephonyServiceImpl.this.isSMSPromptEnabled();
        }

        @Override
        public void setSMSPromptEnabled(boolean enabled) {
            CmExtTelephonyServiceImpl.this.setSMSPromptEnabled(enabled);
        }

        @Override
        public int getPhoneIdForECall() {
            return CmExtTelephonyServiceImpl.this.getPhoneIdForECall();
        }

        @Override
        public boolean isFdnEnabled() {
            return CmExtTelephonyServiceImpl.this.isFdnEnabled();
        }
    };

    public CmExtTelephonyServiceImpl(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        if (DBG) logd("CM Ext telephony service start: " + this);

        publishBinderService(CMContextConstants.CM_TELEPHONY_MANAGER_EXT_SERVICE, mService);

        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private int getCurrentUiccCardProvisioningStatus(int slotId) {
        if (DBG) logd("getCurrentUiccCardProvisioningStatus()");
        return 0;
    }

    private int getUiccCardProvisioningUserPreference(int slotId) {
        if (DBG) logd("getUiccCardProvisioningUserPreference()");
        return 0;
    }

    private int activateUiccCard(int slotId) {
        if (DBG) logd("activateUiccCard()");
        return 0;
    }

    private int deactivateUiccCard(int slotId) {
        if (DBG) logd("deactivateUiccCard()");
        return 0;
    }

    private boolean isSMSPromptEnabled() {
        if (DBG) logd("isSMSPromptEnabled()");
        return false;
    }

    private void setSMSPromptEnabled(boolean enabled) {
        if (DBG) logd("setSMSPromptEnabled()");
    }

    private int getPhoneIdForECall() {
        if (DBG) logd("getPhoneIdForECall()");
        return 0;
    }

    private boolean isFdnEnabled() {
        if (DBG) logd("isFdnEnabled()");
        return false;
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }
}
