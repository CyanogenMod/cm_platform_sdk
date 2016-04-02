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

import android.telephony.SubscriptionInfo;

import java.util.List;

/** @hide */
interface ICMTelephonyManager {
    // --- Methods below are for use by 3rd party applications to manage phone and data connection
    // You need the READ_MSIM_PHONE_STATE permission
    List<SubscriptionInfo> getSubInformation();
    boolean isSubActive(int subId);
    boolean isDataConnectionSelectedOnSub(int subId);
    boolean isDataConnectionEnabled();

    // You need the MODIFY_MSIM_PHONE_STATE permission
    void setSubState(int subId, boolean state);
    void setDataConnectionSelectedOnSub(int subId);
    void setDataConnectionState(boolean state);
    void setDefaultPhoneSub(int subId);
    void setDefaultSmsSub(int subId);
}
