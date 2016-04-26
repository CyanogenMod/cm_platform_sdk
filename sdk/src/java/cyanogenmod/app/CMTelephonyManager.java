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

package cyanogenmod.app;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.util.Slog;

import java.util.List;

import cyanogenmod.app.CMContextConstants;

/**
 * The CMTelephonyManager allows you to view and manage the phone state and
 * the data connection, with multiple SIMs support.
 *
 * <p>
 * To get the instance of this class, utilize CMTelephonyManager#getInstance(Context context)
 */
public class CMTelephonyManager {

    /**
     * Subscription ID used to set the default Phone and SMS to "ask every time".
     */
    public static final int ASK_FOR_SUBSCRIPTION_ID = 0;

    private static final String TAG = "CMTelephonyManager";
    private static boolean localLOGD = Log.isLoggable(TAG, Log.DEBUG);

    private static ICMTelephonyManager sService;
    private static CMTelephonyManager sCMTelephonyManagerInstance;
    private Context mContext;

    private CMTelephonyManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(CMContextConstants.Features.TELEPHONY)
                && sService == null) {
            Log.wtf(TAG, "Unable to get CMTelephonyManagerService. " +
                    "The service either crashed, was not started, or the interface has been " +
                    "called to early in SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.CMTelephonyManager}
     *
     * @return {@link cyanogenmod.app.CMTelephonyManager}
     */
    public static CMTelephonyManager getInstance(Context context) {
        if (sCMTelephonyManagerInstance == null) {
            sCMTelephonyManagerInstance = new CMTelephonyManager(context);
        }
        return sCMTelephonyManagerInstance;
    }

    /** @hide */
    public ICMTelephonyManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_TELEPHONY_MANAGER_SERVICE);
        if (b != null) {
            sService = ICMTelephonyManager.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    /**
     * Gets the list of {@link SubscriptionInfo} that are registered on the
     * phone.
     *
     * @return The list of SIM subscriptions. The returning list can be null or empty.
     * @see SubscriptionInfo
     */
    public List<SubscriptionInfo> getSubInformation() {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return null;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " getting the SIMs information");
        }
        List<SubscriptionInfo> subInfoList = null;
        try {
            subInfoList = sService.getSubInformation();
            if (subInfoList == null) {
                Log.w(TAG, "no subscription list was returned from the service");
            } else if (subInfoList.isEmpty()) {
                Log.w(TAG, "the subscription list is empty");
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }

        return subInfoList;
    }

    /**
     * Returns the state of the SIM by subscription ID.
     *
     * If the subscription ID is not valid the method will return {@code false}.
     *
     * @param subId The subscription ID to query.
     * @return {@code true} if the SIM is activated (even without signal or requesting the
     * PIN/PUK), {@code false} otherwise.
     */
    public boolean isSubActive(int subId) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return false;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " getting the state of the SIM with subscription: " + subId);
        }
        boolean simActive = false;
        try {
            simActive = sService.isSubActive(subId);
            if (localLOGD) {
                String pkg = mContext.getPackageName();
                Log.v(TAG, pkg + " getting the SIM state with subscription " + subId + " as active: " + simActive);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }

        return simActive;
    }

    /**
     * Sets the state of one of the SIMs by subscription ID.
     *
     * If the subscription ID is not valid or the SIM already
     * is in the desired state the method will do nothing.
     *
     * @param subId  The subscription ID to change the state.
     * @param state {@code true} to activate the SIM, {@code false} to disable.
     */
    public void setSubState(int subId, boolean state) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " setting the state of the SIM with subscription " + subId + " as active: " + state);
        }

        try {
            sService.setSubState(subId, state);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }
    }

    /**
     * Checks if the received subscription received has the data
     * connection enabled.
     *
     * This method will return {@code true} (or {@code false} if inactive on the SIM)
     * even when an internet connection is active through Wifi/BT.
     *
     * If the subscription ID is not valid the method will return false.
     *
     * @param subId The subscription ID to query.
     * @return {@code true} if the data connection is enabled on the SIM, {@code false} otherwise.
     */
    public boolean isDataConnectionSelectedOnSub(int subId) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return false;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " getting if the data connection is enabled for SIM for subscription: " + subId);
        }
        boolean dataConnectionActiveOnSim = false;
        try {
            dataConnectionActiveOnSim = sService.isDataConnectionSelectedOnSub(subId);
            if (localLOGD) {
                String pkg = mContext.getPackageName();
                Log.v(TAG, pkg + " getting if the data connection is enabled for SIM with subscription " +
                        subId + " as active: " + dataConnectionActiveOnSim);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }

        return dataConnectionActiveOnSim;
    }

    /**
     * Checks if the network data connection is enabled.
     *
     * This method will return {@code true} (or {@code false} if inactive)
     * even when an internet connection is active through Wifi/BT.
     *
     * @return {@code true} if the network data connection is enabled, {@code false} otherwise.
     */
    public boolean isDataConnectionEnabled() {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return false;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " getting if the network data connection is enabled");
        }
        boolean dataConnectionEnabled = false;
        try {
            dataConnectionEnabled = sService.isDataConnectionEnabled();
            if (localLOGD) {
                String pkg = mContext.getPackageName();
                Log.v(TAG, pkg + " getting if the network data connection is enabled: " + dataConnectionEnabled);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }

        return dataConnectionEnabled;
    }

    /**
     * Sets the network data conection active or inactive.
     *
     * @param state If {@code true} enables the network data connection, if {@code false} disables it.
     */
    public void setDataConnectionState(boolean state) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " setting the network data connection enabled: " + state);
        }

        try {
            sService.setDataConnectionState(state);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }
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
    public void setDataConnectionSelectedOnSub(int subId) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " setting the network data connection for SIM with subscription: " + subId);
        }

        try {
            sService.setDataConnectionSelectedOnSub(subId);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }
    }

    /**
     * Sets the default phone used to make phone calls as the one received on subId.
     *
     * If ASK_FOR_SUBSCRIPTION_ID is used as a parameter, then the option to choose
     * what SIM to use is selected.
     *
     * @param subId The subscription to set as default for phone calls.
     *              To select SIM when calling use ASK_FOR_SUBSCRIPTION_ID.
     */
    public void setDefaultPhoneSub(int subId) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " setting the subscription used for phone calls as: " + subId);
        }

        try {
            sService.setDefaultPhoneSub(subId);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }
    }

    /**
     * Sets the default phone used to send SMS as the one received on subId.
     *
     * If ASK_FOR_SUBSCRIPTION_ID is used as a parameter, then the option to choose
     * what SIM to use is selected.
     *
     * @param subId The subscription to set as default for sending SMS.
     *              To select SIM when sending SMS use ASK_FOR_SUBSCRIPTION_ID.
     */
    public void setDefaultSmsSub(int subId) {
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return;
        }

        if (localLOGD) {
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " setting the subscription used for SMS as: " + subId);
        }

        try {
            sService.setDefaultSmsSub(subId);
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }
    }
}
