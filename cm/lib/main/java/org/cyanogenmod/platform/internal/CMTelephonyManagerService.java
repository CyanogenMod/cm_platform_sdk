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
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CMTelephonyManager;
import cyanogenmod.app.ICMTelephonyManager;

/**
 * Internal service which manages interactions with the phone and data connection
 *
 * @hide
 */
public class CMTelephonyManagerService extends SystemService {
    private static final String TAG = "CMTelephonyManagerSrv";
    private static boolean localLOGD = Log.isLoggable(TAG, Log.DEBUG);

    private final IBinder mService = new ICMTelephonyManager.Stub() {

        /**
         * Returns the available SIM subscription information.
         *
         * @return The list of SIM subscriptions. The returning list can be null or empty.
         * @hide
         */
        @Override
        public List<SubscriptionInfo> getSimInformation() {
            CMTelephonyManagerService.this.enforceTelephonyReadPermission();
            return CMTelephonyManagerService.this.getActiveSubscriptionInfoList();
        }

        /**
         * Returns the state of the SIM by subscription ID.
         *
         * If the subscription ID is not valid the method will return {@code false}.
         *
         * @param subId The subscription ID to query.
         * @return {@code true} if the SIM is activated (even without signal or requesting the
         * PIN/PUK), {@code false} otherwise.
         * @hide
         */
        @Override
        public boolean isSimActive(int subId) {
            CMTelephonyManagerService.this.enforceTelephonyReadPermission();
            return CMTelephonyManagerService.this.isSimActive(subId);
        }

        /**
         * Sets the state of one of the SIMs by subscription ID.
         *
         * If the subscription ID is not valid or the SIM already
         * is in the desired state the method will do nothing.
         *
         * @param subId The subscription ID to set.
         * @param active {@code true} to activate the SIM, {@code false} to disable.
         * @hide
         */
        @Override
        public void setSimState(int subId, boolean active) {
            CMTelephonyManagerService.this.enforceTelephonyModifyPermission();
            CMTelephonyManagerService.this.setSimState(subId, active);
        }

        /**
         * Checks if the received subscription received has the data
         * connection enabled.
         *
         * This method will return {@code true} (or {@code false} if inactive on the SIM)
         * even when an internet connection is active through Wifi/BT.
         *
         * If the subscription ID is not valid the method will return {@code false}.
         *
         * @param subId The subscription ID to query.
         * @return {@code true} if the data connection is enabled on the SIM, {@code false} otherwise.
         * @hide
         */
        public boolean isDataConnectionSelectedOnSim(int subId) {
            CMTelephonyManagerService.this.enforceTelephonyReadPermission();
            return CMTelephonyManagerService.this.isDataConnectionSelectedOnSim(subId);
        }

        /**
         * Checks if the network data connection is enabled.
         *
         * This method will return {@code true} (or {@code false} if inactive)
         * even when an internet connection is active through Wifi/BT.
         *
         * @return {@code true} if the network data connection is enabled, {@code false} otherwise.
         * @hide
         */
        public boolean isDataConnectionEnabled() {
            CMTelephonyManagerService.this.enforceTelephonyReadPermission();
            return CMTelephonyManagerService.this.isDataConnectionEnabled();
        }

        /**
         * Sets the network data conection active or inactive.
         *
         * @param active If {@code true} enables the network data connection, if {@code false} disables it.
         * @hide
         */
        public void setDataConnectionEnabled(boolean active) {
            CMTelephonyManagerService.this.enforceTelephonyModifyPermission();
            CMTelephonyManagerService.this.setDataConnectionEnabled(active);
        }

        /**
         * Sets the data connection state on one of the SIMs by subscription ID.
         *
         * If the subscription ID is not valid or the data connection is already
         * enabled on the SIM the method will do nothing.
         *
         * @param subId The subscription ID to set the network data connection.
         * @hide
         */
        public void setDataConnectionSelectedOnSim(int subId) {
            CMTelephonyManagerService.this.enforceTelephonyModifyPermission();
            CMTelephonyManagerService.this.setDataConnectionSelectedOnSim(subId);
        }

        /**
         * Sets the default phone used to make phone calls as the one received on subId.
         *
         * If 0 is used as a parameter, then the option to choose what SIM to use is
         * selected.
         *
         * @param subId The subscription to set as default for phone calls.
         *              To select SIM when calling use 0.
         * @hide
         */
        public void setPhoneCallUsingSim(int subId) {
            CMTelephonyManagerService.this.enforceTelephonyModifyPermission();
            CMTelephonyManagerService.this.setPhoneCallUsingSim(subId);
        }

        /**
         * Sets the default phone used to send SMS as the one received on subId.
         *
         * If 0 is used as a parameter, then the option to choose what SIM to use is
         * selected.
         *
         * @param subId The subscription to set as default for sending SMS.
         *              To select SIM when sending SMS use 0.
         * @hide
         */
        public void setSmsUsingSim(int subId) {
            CMTelephonyManagerService.this.enforceTelephonyModifyPermission();
            CMTelephonyManagerService.this.setSmsUsingSim(subId);
        }
    };

    public CMTelephonyManagerService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        if (localLOGD) {
            Log.d(TAG, "CM telephony manager service start: " + this);
        }
        publishBinderService(CMContextConstants.CM_TELEPHONY_MANAGER_SERVICE, mService);
    }

    private List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (localLOGD) {
            Log.d(TAG, "The active subscriptions where obtained from the subscription manager.");
        }
        return subInfoList;
    }

    private boolean isSimActive(int subId) {
        boolean validSubscriptionId = SubscriptionManager.isValidSubscriptionId(subId);

        if (validSubscriptionId) {
            int simState = SubscriptionManager.getSimStateForSubscriber(subId);
            switch (simState) {
                case TelephonyManager.SIM_STATE_ABSENT:
                case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                case TelephonyManager.SIM_STATE_PERM_DISABLED:
                case TelephonyManager.SIM_STATE_NOT_READY:
                    if (localLOGD) {
                        Log.d(TAG, "The subscription " + subId + " is NOT active: " + simState);
                    }
                    return false;
                default:
                    if (localLOGD) {
                        Log.d(TAG, "The subscription " + subId + " is active: " + simState);
                    }
                    return true;
            }
        } else {
            Log.w(TAG, "Invalid subscription identifier: " + subId);
            return false;
        }
    }

    private void setSimState(int subId, boolean active) {
        if (localLOGD) {
            Log.d(TAG, "Setting the subscription " + subId + " to inactive (false) or active (true): " + active);
        }

        if (active) {
            SubscriptionManager.activateSubId(subId);
        } else {
            SubscriptionManager.deactivateSubId(subId);
        }
    }

    private boolean isDataConnectionSelectedOnSim(int subId) {
        boolean validSubscriptionId = SubscriptionManager.isValidSubscriptionId(subId);

        if (validSubscriptionId) {
            if (subId == SubscriptionManager.getDefaultDataSubId()) {
                if (localLOGD) {
                    Log.d(TAG, "Data connection selected for subscription " + subId);
                }
                return true;
            } else {
                if (localLOGD) {
                    Log.d(TAG, "Data connection not selected for subscription " + subId);
                }
                return false;
            }
        } else {
            Log.w(TAG, "Invalid subscription identifier: " + subId);
            return false;
        }
    }

    private boolean isDataConnectionEnabled() {
        if (localLOGD) {
            Log.d(TAG, "Checking if the network data connection is active");
        }

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        boolean dataEnabled = telephonyManager.getDataEnabled();

        if (localLOGD) {
            Log.d(TAG, "Data network connection is inactive (false) or active (true): " + dataEnabled);
        }

        return dataEnabled;
    }

    private void setDataConnectionEnabled(boolean active) {
        if (localLOGD) {
            Log.d(TAG, "Setting the network data connection inactive (false) or active (true): " + active);
        }

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (active) {
            telephonyManager.enableDataConnectivity();
        } else {
            telephonyManager.disableDataConnectivity();
        }
    }

    private void setDataConnectionSelectedOnSim(int subId) {
        if (localLOGD) {
            Log.d(TAG, "Setting the network data connection for subscription " + subId);
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        subscriptionManager.setDefaultDataSubId(subId);
    }

    private void setPhoneCallUsingSim(int subId) {
        if (localLOGD) {
            Log.d(TAG, "Setting the SIM for phone calls on subscription " + subId);
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        if (subId == CMTelephonyManager.INVALID_SUBSCRIPTION_ID) {
            if (localLOGD) {
                Log.d(TAG, "Activates the prompt for phone calls");
            }
            SubscriptionManager.setVoicePromptEnabled(true);
        } else {
            SubscriptionManager.setVoicePromptEnabled(false);
            subscriptionManager.setDefaultVoiceSubId(subId);
        }
    }

    private void setSmsUsingSim(int subId) {
        if (localLOGD) {
            Log.d(TAG, "Setting the SIM for phone calls on subscription " + subId);
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        if (subId == CMTelephonyManager.INVALID_SUBSCRIPTION_ID) {
            if (localLOGD) {
                Log.d(TAG, "Activates the prompt for SMS");
            }
            SubscriptionManager.setSMSPromptEnabled(true);
        } else {
            SubscriptionManager.setSMSPromptEnabled(false);
            subscriptionManager.setDefaultSmsSubId(subId);
        }
    }

    private void enforceTelephonyReadPermission() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.READ_MSIM_PHONE_STATE,
                "CMTelephonyManagerService");
    }

    private void enforceTelephonyModifyPermission() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.MODIFY_MSIM_PHONE_STATE,
                "CMTelephonyManagerService");
    }
}
