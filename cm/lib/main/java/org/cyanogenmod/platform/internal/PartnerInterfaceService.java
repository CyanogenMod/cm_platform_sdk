/*
 * Copyright (c) 2011-2015 CyanogenMod Project
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

package org.cyanogenmod.platform.internal;

import android.app.INotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.IAudioService;
import android.net.Uri;
import android.os.IBinder;

import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.Condition;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.IPartnerInterface;
import cyanogenmod.app.PartnerInterface;
import cyanogenmod.media.MediaRecorder;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;


/** {@hide} */
public class PartnerInterfaceService extends SystemService {

    private static final String TAG = "CMSettingsService";

    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private INotificationManager mNotificationManager;
    private IAudioService mAudioService;

    public PartnerInterfaceService(Context context) {
        super(context);
        mContext = context;
        publishBinderService(CMContextConstants.CM_PARTNER_INTERFACE, mService);
    }

    @Override
    public void onStart() {
        mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        IBinder b = ServiceManager.getService(android.content.Context.AUDIO_SERVICE);
        mAudioService = IAudioService.Stub.asInterface(b);
    }

    private void enforceModifyNetworkSettingsPermission() {
        mContext.enforceCallingOrSelfPermission(PartnerInterface.MODIFY_NETWORK_SETTINGS_PERMISSION,
                "You do not have permissions to change system network settings.");
    }

    private void enforceModifySoundSettingsPermission() {
        mContext.enforceCallingOrSelfPermission(PartnerInterface.MODIFY_SOUND_SETTINGS_PERMISSION,
                "You do not have permissions to change system sound settings.");
    }

    private void enforceShutdownPermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.REBOOT,
                "You do not have permissions to shut down the device.");
    }

    private void enforceCaptureHotwordPermission() {
        mContext.enforceCallingOrSelfPermission(MediaRecorder.CAPTURE_AUDIO_HOTWORD_PERMISSION,
                "You do not have permission to query the hotword input package name.");
    }

    private final IBinder mService = new IPartnerInterface.Stub() {

        @Override
        public void setAirplaneModeEnabled(boolean enabled) {
            enforceModifyNetworkSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            setAirplaneModeEnabledInternal(enabled);
            restoreCallingIdentity(token);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            enforceModifyNetworkSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            setMobileDataEnabledInternal(enabled);
            restoreCallingIdentity(token);
        }

        @Override
        public void shutdown() {
            enforceShutdownPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            shutdownInternal(false);
            restoreCallingIdentity(token);
        }

        @Override
        public void reboot() {
            enforceShutdownPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            shutdownInternal(true);
            restoreCallingIdentity(token);
        }

        @Override
        public boolean setZenMode(int mode) {
            enforceModifySoundSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            boolean success = setZenModeInternal(mode, -1);
            restoreCallingIdentity(token);
            return success;
        }

        @Override
        public boolean setZenModeWithDuration(int mode, long durationMillis) {
            enforceModifySoundSettingsPermission();
            /*
             * We need to clear the caller's identity in order to
             *   allow this method call to modify settings
             *   not allowed by the caller's permissions.
             */
            long token = clearCallingIdentity();
            boolean success = setZenModeInternal(mode, durationMillis);
            restoreCallingIdentity(token);
            return success;
        }

        @Override
        public String getCurrentHotwordPackageName() {
            enforceCaptureHotwordPermission();
            long token = clearCallingIdentity();
            String packageName = getHotwordPackageNameInternal();
            restoreCallingIdentity(token);
            return packageName;
        }
    };

    private void setAirplaneModeEnabledInternal(boolean enabled) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                enabled ? 1 : 0);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabled);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void setMobileDataEnabledInternal(boolean enabled) {
        mTelephonyManager.setDataEnabled(enabled);
    }

    private void shutdownInternal(boolean reboot) {
        IPowerManager pm = IPowerManager.Stub.asInterface(
                        ServiceManager.getService(Context.POWER_SERVICE));
        try {
            if (reboot) {
                pm.reboot(false, null, false);
            } else {
                pm.shutdown(false, false);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Unable to shutdown.");
        }
    }

    private boolean setZenModeInternal(int mode, long durationMillis) {
        ContentResolver contentResolver = mContext.getContentResolver();
        int zenModeValue = -1;
        Condition zenModeCondition = null;
        switch(mode) {
            case PartnerInterface.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                zenModeValue = Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                if (durationMillis > -1) {
                    zenModeCondition = createZenModeCondition(durationMillis);
                }
                break;
            case PartnerInterface.ZEN_MODE_OFF:
                zenModeValue = Settings.Global.ZEN_MODE_OFF;
                // Leaving the condition to null signifies "indefinitely"
                // durationMillis is ignored
                break;
            case PartnerInterface.ZEN_MODE_NO_INTERRUPTIONS:
                zenModeValue = Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;
                if (durationMillis > -1) {
                    zenModeCondition = createZenModeCondition(durationMillis);
                }
                break;
            default:
                // Invalid mode parameter
                Log.w(TAG, "setZenModeInteral() called with invalid mode: " + mode);
                return false;
        }

        Settings.Global.putInt(contentResolver,
                Settings.Global.ZEN_MODE,
                zenModeValue);

        try {
            mNotificationManager.setZenModeCondition(zenModeCondition);
        } catch (RemoteException e) {
            // An error occurred, return false since the
            // condition failed to set.
            Log.e(TAG, "setZenMode() failed for mode: " + mode);
            return false;
        }
        return true;
    }

    // setZenModeInternal Helpers

    // Zen Mode constants
    private static final String URI_PREFIX_CONDITION_ANDROID_COUNTDOWN = "condition://android/countdown/";
    private static final SimpleDateFormat DURATION_TIMEDATE_FORMAT = new SimpleDateFormat("hh:mm a");
    private static final long DURATION_SECOND_MS = 1000;
    private static final long DURATION_MINUTE_MS = 60 * DURATION_SECOND_MS;
    private static final long DURATION_HOUR_MS = 60 * DURATION_MINUTE_MS;
    private static final long DURATION_DAY_MS = 24 * DURATION_HOUR_MS;

    // createZenModeCondition for specifying duration of zen mode
    private Condition createZenModeCondition(long durationMillis) {
        final long endTimeMillis = System.currentTimeMillis() + durationMillis;
        final Uri conditionId = Uri.parse(URI_PREFIX_CONDITION_ANDROID_COUNTDOWN + endTimeMillis);
        final String line1 = formatZenModeDuration(durationMillis);
        final String line2_content = formatZenModeEndtime(endTimeMillis);
        return new Condition(conditionId,
                // summary: a concatenation of line1 + line2_content
                mContext.getString(R.string.format_summary, line1, line2_content),
                // line 1: the "For 15 minutes" duration info
                line1,
                // line 2: the "Until 11:54 AM" end time info
                mContext.getString(R.string.format_line2, line2_content),
                //0 for no icon
                0,
                //TRUE state for valid condition
                Condition.STATE_TRUE,
                //flag = 1
                Condition.FLAG_RELEVANT_NOW);
    }

    // formatZenModeEndtime helper for the "11:54 AM" line 2 content
    private String formatZenModeEndtime(long endTimeMillis) {
        return mContext.getString(R.string.format_line1,
                DURATION_TIMEDATE_FORMAT.format(new java.util.Date(endTimeMillis)));
    }

    // formatZenModeDuration helper for "For 15 minutes" line 1
    // per Google volume setting UI: Unless the duration is
    // less than a minute, always return the duration rounded
    // to the nearest minute. And disregard the hour minutes detail > 1 day
    private String formatZenModeDuration(long durationMillis) {
        // Use "0 seconds" and avoid all other zero plurals
        if (durationMillis < DURATION_MINUTE_MS) {
            // round to nearest second
            return mContext.getResources().getQuantityString(R.plurals.seconds, (int)(durationMillis / DURATION_SECOND_MS));
        }
        if (durationMillis < DURATION_HOUR_MS) {
            // round to nearest minute
            return mContext.getResources().getQuantityString(R.plurals.minutes, (int)(durationMillis / DURATION_MINUTE_MS));
        }
        if (durationMillis < DURATION_DAY_MS) {
            // round to the nearest hour & minute
            return formatHourMinutes(durationMillis);
        }

        // limit the days to MAX_INT on purpose
        final int days = (int) (durationMillis / DURATION_DAY_MS);
        if (days == 1) {
            // only print hour minutes if it's < 2 days
            return mContext.getResources().getQuantityString(R.plurals.days, days) +
                    mContext.getString(R.string.duration_separator) +
                    formatHourMinutes(durationMillis % DURATION_DAY_MS);
        }
        // return "xx days" or "many days"
        return mContext.getResources().getQuantityString(R.plurals.days, days);
    }

    // formatHourMinutes helper to formatZenModeDuration
    private String formatHourMinutes(long hourMinutesMillis) {
        final int hours = (int) (hourMinutesMillis / DURATION_HOUR_MS);
        final int minutes = (int) ((hourMinutesMillis % DURATION_HOUR_MS) / DURATION_MINUTE_MS);

        // avoid printing "0 minutes" when hour is non-zero
        if (minutes == 0) {
            return mContext.getResources().getQuantityString(R.plurals.hours, hours);
        }
        //hour(s) - "xx hours" + separator + "xx minutes"
        return mContext.getResources().getQuantityString(R.plurals.hours, hours) +
                mContext.getString(R.string.duration_separator) +
                mContext.getResources().getQuantityString(R.plurals.minutes, minutes);
    }

    public String getHotwordPackageNameInternal() {
        String packageName = null;
        try {
            packageName = mAudioService.getCurrentHotwordInputPackageName();
        } catch (RemoteException e) {
            Log.e(TAG, "getHotwordPackageName() failed.");
        }
        return packageName;
    }
}

