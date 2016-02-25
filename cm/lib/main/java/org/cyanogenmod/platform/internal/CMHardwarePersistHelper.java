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

import com.android.internal.util.XmlUtils;

import org.cyanogenmod.platform.internal.CMHardwareService.CMHardwareInterface;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cyanogenmod.hardware.DisplayMode;
import libcore.io.IoUtils;

public class CMHardwarePersistHelper extends BroadcastReceiver {
    private static final String TAG = "CMHardwarePersistHelper";
    private static final String FILE_NAME = "cm_hardware_manager.xml";
    private static final int BUFFER_TIME = 1000;

    // Settings to backup/restore
    private static final String VIBRATOR_INTENSITY_KEY = "vibrator_intensity";
    private static final String DISPLAY_COLOR_KEY = "display_color";
    private static final String DISPLAY_GAMMA_KEY = "display_gamma";
    private static final String DISPLAY_MODE_KEY = "display_mode";

    private static final int RESTORE_SETTINGS = 0;
    private static final int WRITE_SETTINGS = 1;

    private final Handler mHandler;
    private final CMHardwareInterface mCMHardwareInterface;
    private final Map<String, Object> mMap;

    public CMHardwarePersistHelper(Context context, CMHardwareInterface cmHwImpl) {
        mCMHardwareInterface = cmHwImpl;
        mMap = Collections.synchronizedMap(new HashMap<String, Object>());

        // Initialize thread used for reading/writing settings
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new PersistHandler(thread.getLooper());

        // Register receiver for user changes, so we can
        // restore settings per user
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_SWITCHED);
        context.registerReceiver(this, intentFilter);

        restoreSettingsForCurrentUser();
    }

    public void restoreSettingsForCurrentUser() {
        mHandler.removeMessages(RESTORE_SETTINGS);
        mHandler.sendEmptyMessage(RESTORE_SETTINGS);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        restoreSettingsForCurrentUser();
    }

    private File getCurrentUserFile(int userId) {
        File systemDir = Environment.getUserSystemDirectory(userId);
        return new File(systemDir, FILE_NAME);
    }

    private void commitTransaction() {
        mHandler.removeMessages(WRITE_SETTINGS);
        Message message = Message.obtain(mHandler, WRITE_SETTINGS);
        message.arg1 = ActivityManager.getCurrentUser();
        message.obj = new HashMap<String, Object>(mMap);
        // Ensure we do buffer of some sort
        mHandler.sendMessageDelayed(message, BUFFER_TIME);
    }

    private Parcel decodeToParcel(String s) {
        byte[] array = Base64.decode(s, Base64.DEFAULT);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(array, 0, array.length);
        parcel.setDataPosition(0);
        return parcel;
    }

    private String encodeParcelable(Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        parcelable.writeToParcel(parcel, 0);
        return Base64.encodeToString(parcel.marshall(), Base64.DEFAULT);
    }

    // Vibrator intensity
    public void persistVibratorIntensity(int intensity) {
        if (!Objects.equals(intensity, mMap.get(VIBRATOR_INTENSITY_KEY))) {
            mMap.put(VIBRATOR_INTENSITY_KEY, intensity);
            commitTransaction();
        }
    }

    public void restoreVibratorIntesity() {
        if (mMap.containsKey(VIBRATOR_INTENSITY_KEY)) {
            mCMHardwareInterface.setVibratorIntensity((Integer) mMap.get(VIBRATOR_INTENSITY_KEY));
        }
    }

    // Color calibration
    public void persistDisplayColorCalibration(int[] rgb) {
        if (!Objects.equals(rgb, mMap.get(DISPLAY_COLOR_KEY))) {
            mMap.put(DISPLAY_COLOR_KEY, rgb);
            commitTransaction();
        }
    }

    private void restoreDisplayColorCalibration() {
        if (mMap.containsKey(DISPLAY_COLOR_KEY)) {
            mCMHardwareInterface.setDisplayColorCalibration((int[]) mMap.get(DISPLAY_COLOR_KEY));
        }
    }

    // Gamma calibration
    public void persistDisplayGammaCalibration(int[] rgb) {
        if (!Objects.equals(rgb, mMap.get(DISPLAY_GAMMA_KEY))) {
            mMap.put(DISPLAY_GAMMA_KEY, rgb);
            commitTransaction();
        }
    }

    private void restoreDisplayGammaCalibration() {
        if (mMap.containsKey(DISPLAY_GAMMA_KEY)) {
            mCMHardwareInterface.setDisplayColorCalibration((int[]) mMap.get(DISPLAY_GAMMA_KEY));
        }
    }

    // Display mode
    public void persistDisplayMode(DisplayMode mode) {
        String currentValue = (String) mMap.get(DISPLAY_MODE_KEY);
        String newValue = encodeParcelable(mode);
        if (!Objects.equals(currentValue, newValue)) {
            mMap.put(DISPLAY_MODE_KEY, newValue);
            commitTransaction();
        }
    }

    public void restoreDisplayMode() {
        if (mMap.containsKey(DISPLAY_MODE_KEY)) {
            Parcel parcel = decodeToParcel((String) mMap.get(DISPLAY_MODE_KEY));
            DisplayMode mode = DisplayMode.CREATOR.createFromParcel(parcel);
            mCMHardwareInterface.setDisplayMode(mode, true);
        }
    }

    private class PersistHandler extends Handler {
        PersistHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESTORE_SETTINGS:
                    FileInputStream fileInputStream = null;
                    Map readMap = null;
                    try {
                        int userId = ActivityManager.getCurrentUser();
                        fileInputStream = new FileInputStream(getCurrentUserFile(userId));
                        readMap = XmlUtils.readMapXml(fileInputStream);
                    } catch (XmlPullParserException | IOException e) {
                        Log.e(TAG, e.getMessage());
                    } finally {
                        IoUtils.closeQuietly(fileInputStream);
                    }
                    if (readMap != null) {
                        synchronized (mMap) {
                            mMap.clear();
                            mMap.putAll(readMap);
                        }
                        restoreSettings();
                    }
                    break;
                case WRITE_SETTINGS:
                    FileOutputStream fileOutputStream = null;
                    try {
                        int userId = msg.arg1;
                        Map map = (Map) msg.obj;
                        fileOutputStream = new FileOutputStream(getCurrentUserFile(userId));
                        XmlUtils.writeMapXml(map, fileOutputStream);
                    } catch (XmlPullParserException | IOException e) {
                        Log.e(TAG, e.getMessage());
                    } finally {
                        IoUtils.closeQuietly(fileOutputStream);
                    }
                    break;
            }
        }

        private void restoreSettings() {
            restoreVibratorIntesity();
            restoreDisplayColorCalibration();
            restoreDisplayGammaCalibration();
            restoreDisplayMode();
        }
    }
}
