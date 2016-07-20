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

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.util.Slog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.ICMTelephonyManager;

public class CMTelephonyManagerBroker extends BrokerableCMSystemService<ICMTelephonyManager> {

    private static final String TAG = "CMTelephonyBroker";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmtelephony.service",
                    "org.cyanogenmod.cmtelephony.service.CMTelephonyService");

    private final ICMTelephonyManager mServiceStubForFailure =
            new ICMTelephonyManager.NoOp();

    public CMTelephonyManagerBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        if (DEBUG) Slog.d(TAG, "service started");

        publishBinderService(CMContextConstants.CM_TELEPHONY_MANAGER_SERVICE, new BinderService());
    }

    @Override
    protected ICMTelephonyManager getIBinderAsIInterface(@NonNull IBinder service) {
        return ICMTelephonyManager.Stub.asInterface(service);
    }

    @Override
    protected ICMTelephonyManager getDefaultImplementation() {
        return mServiceStubForFailure;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.TELEPHONY;
    }

    private final class BinderService extends ICMTelephonyManager.Stub {

        /**
         * Returns the available SIM subscription information.
         *
         * @return The list of SIM subscriptions. The returning list can be null or empty.
         * @hide
         */
        @Override
        public List<SubscriptionInfo> getSubInformation() throws RemoteException {
            enforceTelephonyReadPermission();
            return getBrokeredService().getSubInformation();
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
        public boolean isSubActive(int subId) throws RemoteException  {
            enforceTelephonyReadPermission();
            return getBrokeredService().isSubActive(subId);
        }

        /**
         * Sets the state of one of the SIMs by subscription ID.
         *
         * If the subscription ID is not valid or the SIM already
         * is in the desired state the method will do nothing.
         *
         * @param subId The subscription ID to set.
         * @param state {@code true} to activate the SIM, {@code false} to disable.
         * @hide
         */
        @Override
        public void setSubState(int subId, boolean state)  throws RemoteException {
            enforceTelephonyModifyPermission();
            getBrokeredService().setSubState(subId, state);
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
         * @return {@code true} if the data connection is enabled on the SIM, {@code false}
         * otherwise.
         * @hide
         */
        public boolean isDataConnectionSelectedOnSub(int subId) throws RemoteException  {
            enforceTelephonyReadPermission();
            return getBrokeredService().isDataConnectionSelectedOnSub(subId);
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
        public boolean isDataConnectionEnabled() throws RemoteException  {
            enforceTelephonyReadPermission();
            return getBrokeredService().isDataConnectionEnabled();
        }

        /**
         * Sets the network data conection active or inactive.
         *
         * @param state If {@code true} enables the network data connection, if {@code false}
         *              disables it.
         * @hide
         */
        public void setDataConnectionState(boolean state)  throws RemoteException {
            enforceTelephonyModifyPermission();
            getBrokeredService().setDataConnectionState(state);
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
        public void setDataConnectionSelectedOnSub(int subId) throws RemoteException  {
            enforceTelephonyModifyPermission();
            getBrokeredService().setDataConnectionSelectedOnSub(subId);
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
        public void setDefaultPhoneSub(int subId) throws RemoteException  {
            enforceTelephonyModifyPermission();
            getBrokeredService().setDefaultPhoneSub(subId);
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
        public void setDefaultSmsSub(int subId) throws RemoteException  {
            enforceTelephonyModifyPermission();
            getBrokeredService().setDefaultSmsSub(subId);
        }

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);

            pw.println();
        }
    }

    private void enforceTelephonyReadPermission() {
        mContext.enforceCallingPermission(
                cyanogenmod.platform.Manifest.permission.READ_MSIM_PHONE_STATE,
                "CMTelephonyManagerService");
    }

    private void enforceTelephonyModifyPermission() {
        mContext.enforceCallingPermission(
                cyanogenmod.platform.Manifest.permission.MODIFY_MSIM_PHONE_STATE,
                "CMTelephonyManagerService");
    }
}
