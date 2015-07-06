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

package cyanogenmod.app;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.util.Slog;

import java.util.List;

/**
 * The CMTelephonyManager allows you to view and manage the phone state and
 * the data connection, with multiple SIMs support.
 *
 * <p>
 * To get the instance of this class, utilize CMTelephonyManager#getInstance(Context context)
 *
 */
public class CMTelephonyManager {
    private static final String TAG = "CMTelephonyManager";
    private static boolean localLOGV = false;

    private Context mContext;

    private static ICMTelephonyManager sService;

    private static CMTelephonyManager sCMTelephonyManagerInstance;

    private CMTelephonyManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.CMTelephonyManager}
     * @param context
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
     * @see SubscriptionInfo
     *
     * @return The list of SIM subscriptions. The returning list can be null or empty.
     */
    public List<SubscriptionInfo> getSimInformation(){
        if (sService == null) {
            Log.w(TAG, "not connected to CMTelephonyManager");
            return null;
        }

        if (localLOGV){
            String pkg = mContext.getPackageName();
            Log.v(TAG, pkg + " getting the SIMs information");
        }
        List<SubscriptionInfo> subInfoList = null;
        try {
            subInfoList = sService.getSimInformation();
            if (subInfoList == null) {
                Log.w(TAG, "no subscription list was returned from the service");
            }else if(subInfoList.isEmpty()){
                Log.w(TAG, "the subscription list is empty");
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no cm telephony manager service");
        }

        return subInfoList;
    }
}
