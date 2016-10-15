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
package cyanogenmod.preference;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import cyanogenmod.platform.Manifest;

public class RemotePreference extends SelfRemovingPreference {

    private static final String TAG = RemotePreference.class.getSimpleName();

    static final boolean DEBUG = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String ACTION_REFRESH_PREFERENCE =
            "cyanogenmod.intent.action.REFRESH_PREFERENCE";

    public static final String ACTION_UPDATE_PREFERENCE =
            "cyanogenmod.intent.action.UPDATE_PREFERENCE";

    private static final String META_REMOTE_RECEIVER =
            "org.cyanogenmod.settings.summary.receiver";

    private static final String META_REMOTE_KEY =
            "org.cyanogenmod.settings.summary.key";

    public static final String EXTRA_ENABLED = ":cm:pref_enabled";
    public static final String EXTRA_KEY = ":cm:pref_key";
    public static final String EXTRA_SUMMARY = ":cm:pref_summary";

    private static final int MSG_UPDATE = 1;
    private static final int MSG_DISABLE = 2;

    protected final Context mContext;

    private Intent mReceiverIntent;

    private boolean mInitialized = false;

    public RemotePreference(Context context, AttributeSet attrs,
                                  int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        mContext = context;

        setVerifyIntent(true);
    }

    public RemotePreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, com.android.internal.R.attr.preferenceScreenStyle, 0);
    }

    public RemotePreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.preferenceScreenStyle);
    }

    protected void onRemoteUpdated(Bundle bundle) {
        Log.d(TAG, "onRemoteUpdated: " + bundle.toString());
        setSummary(bundle.getString(EXTRA_SUMMARY));
    }

    @Override
    public void onAttached() {
        super.onAttached();

        if (isAvailable()) {
            if (!mInitialized) {
                mReceiverIntent = getReceiverIntent();
                mInitialized = true;

                if (DEBUG) Log.d(TAG, "key=" + getKey() +
                        " intent=" + Objects.toString(getIntent()) +
                        " receiver=" + Objects.toString(mReceiverIntent));
            }
            if (mReceiverIntent != null) {
                registerListener();
                requestUpdate();
            }
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();

        if (isAvailable()) {
            if (mReceiverIntent != null) {
                unregisterListener();
            }
        }
    }

    protected IntentFilter getRefreshComponentFilter(Context context) {
        IntentFilter fi = new IntentFilter(ACTION_REFRESH_PREFERENCE);
        fi.addDataScheme("parts");
        fi.addDataAuthority(mReceiverIntent.getComponent().getPackageName(), null);
        fi.addDataPath("/" + getKey(), PatternMatcher.PATTERN_LITERAL);
        return fi;
    }

    protected synchronized void registerListener() {
        mContext.registerReceiver(mListener, getRefreshComponentFilter(mContext),
                Manifest.permission.MANAGE_PARTS, null);
    }

    protected synchronized void unregisterListener() {
        mContext.unregisterReceiver(mListener);
    }

    private void requestUpdate() {
        // Send an ordered broadcast to request a refresh and receive the reply
        // on the BroadcastReceiver.
        if (mReceiverIntent == null) {
            Log.e(TAG, "Could not find receiver for key=" + getKey());
            return;
        }

        mContext.sendOrderedBroadcastAsUser(mReceiverIntent, UserHandle.CURRENT,
                Manifest.permission.MANAGE_PARTS,
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle bundle = getResultExtras(true);
                        if (DEBUG) Log.d(TAG, "onReceive intent=" + intent.toString() +
                                               " result=" + bundle.toString());

                        String key = bundle.getString(EXTRA_KEY);
                        if (key != null && key.equals(getKey())) {
                            // back to the UI thread
                            if (!bundle.getBoolean(EXTRA_ENABLED, true)) {
                                mHandler.sendEmptyMessage(MSG_DISABLE);
                            } else{
                                mHandler.obtainMessage(MSG_UPDATE, bundle).sendToTarget();
                            }
                        }
                    }
                },
                null, Activity.RESULT_OK, null, null);
    }

    private Intent getReceiverIntent() {
        if (getIntent() == null) {
            Log.w(TAG, "No target intent specified in preference!");
            return null;
        }

        if (IntentCache.instance(mContext).contains(getKey())) {
            return IntentCache.instance(mContext).get(getKey());
        }

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(getIntent(),
                PackageManager.MATCH_SYSTEM_ONLY | PackageManager.GET_META_DATA,
                UserHandle.myUserId());

        if (results.size() == 0) {
            Log.w(TAG, "No activity found for: " + getIntent());
        }

        for (ResolveInfo resolved : results) {
            if (!resolved.system) {
                continue;
            }

            ActivityInfo info = resolved.activityInfo;
            Log.d(TAG, "ResolveInfo " + Objects.toString(resolved));

            Bundle meta = info.metaData;
            if (meta == null || !meta.containsKey(META_REMOTE_RECEIVER)) {
                continue;
            }

            String receiverClass = meta.getString(META_REMOTE_RECEIVER);
            String receiverPackage = info.packageName;
            String prefKey = meta.getString(META_REMOTE_KEY);

            if (DEBUG) Log.d(TAG, "getReceiverIntent class=" + receiverClass +
                                  " package=" + receiverPackage + " key=" + prefKey);

            if (prefKey == null || !prefKey.equals(getKey())) {
                continue;
            }

            Intent i = new Intent(ACTION_UPDATE_PREFERENCE);
            i.setComponent(new ComponentName(receiverPackage, receiverClass));
            i.putExtra(EXTRA_KEY, prefKey);

            IntentCache.instance(mContext).put(getKey(), i);
            return i;
        }
        return null;
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE:
                    onRemoteUpdated((Bundle)msg.obj);
                    break;
                case MSG_DISABLE:
                    setAvailable(false);
                    break;
            }
        }
    };

    private final BroadcastReceiver mListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (RemotePreference.this) {
                if (DEBUG) Log.d(TAG, "onReceive: intent=" + Objects.toString(intent) +
                                      " extras=" + Objects.toString(intent.getExtras()));

                if (ACTION_REFRESH_PREFERENCE.equals(intent.getAction())) {
                    String key = intent.getStringExtra(EXTRA_KEY);
                    if (key != null && key.equals(getKey())) {
                        requestUpdate();
                    }
                }
            }
        }
    };

    private static class IntentCache {

        private static IntentCache sInstance;

        private final Context mContext;
        private final Map<String, Intent> mCache = new ArrayMap<>();

        private IntentCache(Context context) {
            mContext = context;
        }

        public synchronized static IntentCache instance(Context context) {
            if (sInstance == null) {
                sInstance = new IntentCache(context);
            }
            return sInstance;
        }

        public synchronized boolean contains(String key) {
            return mCache.containsKey(key);
        }

        public synchronized void put(String key, Intent i) {
            mCache.put(key, i);
        }

        public synchronized Intent get(String key) {
            return mCache.get(key);
        }
    }
}
