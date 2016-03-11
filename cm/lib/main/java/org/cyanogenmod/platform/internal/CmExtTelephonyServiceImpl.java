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

import cyanogenmod.app.CMContextConstants;

import com.android.internal.telephony.IExtTelephony;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.Phone;
import android.telephony.ServiceState;

/**
 * This class implements IExtTelephony aidl interface.
 * @hide
 */
public class CmExtTelephonyServiceImpl extends SystemService {
    private static final String LOG_TAG = "CmExtTelephonyServiceImpl";
    private static final boolean DBG = true;

    private Context mContext;
    private TelephonyManager mTelephonyManager;

    static final int PROVISIONED = 1;
    static final int NOT_PROVISIONED = 0;
    static final int INVALID_STATE = -1;
    static final int CARD_NOT_PRESENT = -2;

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
        return PROVISIONED;
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
        return SmsManager.getDefault().isSMSPromptEnabled();
    }

    private void setSMSPromptEnabled(boolean enabled) {
        if (DBG) logd("setSMSPromptEnabled()");
        SmsManager.getDefault().setSMSPromptEnabled(enabled);
    }

    /**
      * Place E911 call on a sub(i.e Phone) whichever is In service/Emergency Only
      * If both subs are In service/Emergency Only then place call on voice pref sub
      * Else choose the default phone_id = 0
      */
    private int getPhoneIdForECall() {
        if (DBG) logd("getPhoneIdForECall()");

        int voiceSubId = SubscriptionManager.getDefaultVoiceSubId();
        int voicePhoneId = SubscriptionController.getInstance().getPhoneId(voiceSubId);
        int phoneId = -1;
        TelephonyManager tm = TelephonyManager.getDefault();
        int phoneCount = tm.getPhoneCount();

        for (int phId = 0; phId < phoneCount; phId++) {
            Phone phone = PhoneFactory.getPhone(phId);
            int ss = phone.getServiceState().getState();
            if(ss == ServiceState.STATE_IN_SERVICE ||phone.getServiceState().isEmergencyOnly()) {
                phoneId = phId;
                if (phoneId == voicePhoneId) break;
            }
        }

        if (DBG) logd("Voice phoneId in service = "+ phoneId);

        if (phoneId == -1) {
            phoneId = 0; // default phoneId
        }

        if (DBG) logd("Voice phoneId in service = " + phoneId + " preferred phoneId =" + voicePhoneId);

        return phoneId;
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
