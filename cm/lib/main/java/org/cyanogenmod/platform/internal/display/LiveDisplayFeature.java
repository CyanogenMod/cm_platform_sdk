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
package org.cyanogenmod.platform.internal.display;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

import com.android.server.pm.UserContentObserver;
import com.android.server.twilight.TwilightState;

import java.io.PrintWriter;
import java.util.BitSet;

import cyanogenmod.providers.CMSettings;

public abstract class LiveDisplayFeature {

    protected static final String TAG = "LiveDisplay";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected final Context mContext;
    protected final Handler mHandler;

    private TwilightState mTwilight;
    private boolean mLowPowerMode = false;
    private boolean mScreenOn = false;
    private int mMode = 0;

    private final SettingsObserver mSettingsObserver;

    public LiveDisplayFeature(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mSettingsObserver = new SettingsObserver(handler);
    }

    public abstract boolean onStart();

    public abstract void onSettingsChanged(Uri uri);

    public void onModeChanged(int mode) {
        mMode = mode;
    }

    public void onDisplayStateChanged(boolean screenOn) {
        mScreenOn = screenOn;
    }

    public void onLowPowerModeChanged(boolean lowPowerMode) {
        mLowPowerMode = lowPowerMode;
    }

    public void onTwilightUpdated(TwilightState twilight) {
        mTwilight = twilight;
    }

    public void onDestroy() {
        mSettingsObserver.unregister();
    }

    public abstract void dump(PrintWriter pw);

    abstract void getCapabilities(final BitSet caps);

    protected final void registerSettings(Uri... settings) {
        mSettingsObserver.register(settings);
        onSettingsChanged(null);
    }

    protected final int getInt(String setting, int defaultValue) {
        return CMSettings.System.getIntForUser(mContext.getContentResolver(),
                setting, defaultValue, UserHandle.USER_CURRENT);
    }

    protected final void putInt(String setting, int value) {
        CMSettings.System.putIntForUser(mContext.getContentResolver(),
                setting, value, UserHandle.USER_CURRENT);
    }

    protected final String getString(String setting) {
        return CMSettings.System.getStringForUser(mContext.getContentResolver(),
                setting, UserHandle.USER_CURRENT);
    }

    protected final void putString(String setting, String value) {
        CMSettings.System.putStringForUser(mContext.getContentResolver(),
                setting, value, UserHandle.USER_CURRENT);
    }

    protected final boolean isLowPowerMode() {
        return mLowPowerMode;
    }

    protected final int getMode() {
        return mMode;
    }

    protected final boolean isScreenOn() {
        return mScreenOn;
    }

    protected final TwilightState getTwilight() {
        return mTwilight;
    }

    protected final boolean isNight() {
        return mTwilight != null && mTwilight.isNight();
    }

    final class SettingsObserver extends UserContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void register(Uri... uris) {
            final ContentResolver cr = mContext.getContentResolver();
            for (Uri uri : uris) {
                cr.registerContentObserver(uri, false, this, UserHandle.USER_ALL);
            }

            observe();
        }

        public void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
            unobserve();
        }

        @Override
        protected void update() {
            onSettingsChanged(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            onSettingsChanged(uri);
        }
    }

}
