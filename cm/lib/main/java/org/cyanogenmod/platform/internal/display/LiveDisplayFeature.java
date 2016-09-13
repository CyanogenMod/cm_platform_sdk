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

import org.cyanogenmod.platform.internal.common.UserContentObserver;
import org.cyanogenmod.platform.internal.display.LiveDisplayService.State;
import org.cyanogenmod.platform.internal.display.TwilightTracker.TwilightState;

import java.io.PrintWriter;
import java.util.BitSet;

import cyanogenmod.providers.CMSettings;

import static org.cyanogenmod.platform.internal.display.LiveDisplayService.ALL_CHANGED;
import static org.cyanogenmod.platform.internal.display.LiveDisplayService.DISPLAY_CHANGED;
import static org.cyanogenmod.platform.internal.display.LiveDisplayService.MODE_CHANGED;
import static org.cyanogenmod.platform.internal.display.LiveDisplayService.TWILIGHT_CHANGED;

public abstract class LiveDisplayFeature {

    protected static final String TAG = "LiveDisplay";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected final Context mContext;
    protected final Handler mHandler;

    private SettingsObserver mSettingsObserver;
    private State mState;

    public LiveDisplayFeature(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    public abstract void onStart();

    protected abstract void onSettingsChanged(Uri uri);

    public abstract void dump(PrintWriter pw);

    public abstract boolean getCapabilities(final BitSet caps);

    protected abstract void onUpdate();

    void update(final int flags, final State state) {
        mState = state;
        if ((flags & DISPLAY_CHANGED) != 0) {
            onScreenStateChanged();
        }
        if (((flags & TWILIGHT_CHANGED) != 0) && mState.mTwilight != null) {
            onTwilightUpdated();
        }
        if ((flags & MODE_CHANGED) != 0) {
            onUpdate();
        }
        if (flags == ALL_CHANGED) {
            onSettingsChanged(null);
        }
    }

    void start() {
        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver(mHandler);
            onStart();
        }
    }

    public void onDestroy() {
        mSettingsObserver.unregister();
    }

    protected void onScreenStateChanged() { }

    protected void onTwilightUpdated() { }

    protected final void registerSettings(Uri... settings) {
        mSettingsObserver.register(settings);
    }

    protected final boolean getBoolean(String setting, boolean defaultValue) {
        return CMSettings.System.getIntForUser(mContext.getContentResolver(),
                setting, (defaultValue ? 1 : 0), UserHandle.USER_CURRENT) == 1;
    }

    protected final void putBoolean(String setting, boolean value) {
        CMSettings.System.putIntForUser(mContext.getContentResolver(),
                setting, (value ? 1 : 0), UserHandle.USER_CURRENT);
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
        return mState.mLowPowerMode;
    }

    protected final int getMode() {
        return mState.mMode;
    }

    protected final boolean isScreenOn() {
        return mState.mScreenOn;
    }

    protected final TwilightState getTwilight() {
        return mState.mTwilight;
    }

    protected final boolean isNight() {
        return mState.mTwilight != null && mState.mTwilight.isNight();
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
