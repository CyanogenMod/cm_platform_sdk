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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.R;
import android.util.AttributeSet;
import android.util.Log;

import java.util.List;
import java.util.Objects;

/**
 * A RemotePreference is a view into preference logic which lives in another
 * process. The primary use case for this is at the platform level where
 * many applications may be contributing their preferences into the
 * Settings app.
 *
 * A RemotePreference appears as a PreferenceScreen and redirects to
 * the real application when clicked. The remote application can
 * send events back to the preference when data changes and the view
 * needs to be updated. See RemotePreferenceUpdater for a base class
 * to use on the application side which implements the listeners and
 * protocol.
 *
 * The interprocess communication is realized using BroadcastReceivers.
 * When the application wants to update the RemotePreference with
 * new data, it sends an ACTION_REFRESH_PREFERENCE with a particular
 * Uri. The RemotePreference listens while attached, and performs
 * an ordered broadcast with ACTION_UPDATE_PREFERENCE back to
 * the application, which is then returned to the preference after
 * being filled with new data.
 *
 * The external activity should include the META_REMOTE_RECEIVER
 * and (optionally) the META_REMOTE_KEY strings in it's metadata.
 * META_REMOTE_RECEIVER must contain the class name of the
 * RemotePreferenceUpdater which we should request updates from.
 * META_REMOTE_KEY must contain the key used by the preference
 * which should match on both sides.
 */
public class RemotePreference extends SelfRemovingPreference
        implements RemotePreferenceManager.OnRemoteUpdateListener {

    private static final String TAG = RemotePreference.class.getSimpleName();

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String ACTION_REFRESH_PREFERENCE =
            "cyanogenmod.intent.action.REFRESH_PREFERENCE";

    public static final String ACTION_UPDATE_PREFERENCE =
            "cyanogenmod.intent.action.UPDATE_PREFERENCE";

    public static final String META_REMOTE_RECEIVER =
            "org.cyanogenmod.settings.summary.receiver";

    public static final String META_REMOTE_KEY =
            "org.cyanogenmod.settings.summary.key";

    public static final String EXTRA_ENABLED = ":cm:pref_enabled";
    public static final String EXTRA_KEY = ":cm:pref_key";
    public static final String EXTRA_SUMMARY = ":cm:pref_summary";

    protected final Context mContext;

    public RemotePreference(Context context, AttributeSet attrs,
                                  int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        mContext = context;
    }

    public RemotePreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public RemotePreference(Context context, AttributeSet attrs) {
        this(context, attrs, ConstraintsHelper.getAttr(
                context, R.attr.preferenceScreenStyle, android.R.attr.preferenceScreenStyle));
    }

    @Override
    public void onRemoteUpdated(Bundle bundle) {
        if (DEBUG) Log.d(TAG, "onRemoteUpdated: " + bundle.toString());

        if (bundle.containsKey(EXTRA_ENABLED)) {
            boolean available = bundle.getBoolean(EXTRA_ENABLED, true);
            if (available != isAvailable()) {
                setAvailable(available);
            }
        }
        if (isAvailable()) {
            setSummary(bundle.getString(EXTRA_SUMMARY));
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (isAvailable()) {
            RemotePreferenceManager.get(mContext).attach(getKey(), this);
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        RemotePreferenceManager.get(mContext).detach(getKey());
    }

    protected String getRemoteKey(Bundle metaData) {
        String remoteKey = metaData.getString(META_REMOTE_KEY);
        return (remoteKey == null || !remoteKey.equals(getKey())) ? null : remoteKey;
    }

    @Override
    public Intent getReceiverIntent() {
        final Intent i = getIntent();
        if (i == null) {
            Log.w(TAG, "No target intent specified in preference!");
            return null;
        }

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(i,
                PackageManager.MATCH_SYSTEM_ONLY | PackageManager.GET_META_DATA,
                UserHandle.myUserId());


        if (results.size() == 0) {
            Log.w(TAG, "No activity found for: " + Objects.toString(i));
        }

        for (ResolveInfo resolved : results) {
            ActivityInfo info = resolved.activityInfo;
            Log.d(TAG, "ResolveInfo " + Objects.toString(resolved));

            Bundle meta = info.metaData;
            if (meta == null || !meta.containsKey(META_REMOTE_RECEIVER)) {
                continue;
            }

            String receiverClass = meta.getString(META_REMOTE_RECEIVER);
            String receiverPackage = info.packageName;
            String remoteKey = getRemoteKey(meta);

            if (DEBUG) Log.d(TAG, "getReceiverIntent class=" + receiverClass +
                                  " package=" + receiverPackage + " key=" + remoteKey);

            if (remoteKey == null) {
                continue;
            }

            Intent ri = new Intent(ACTION_UPDATE_PREFERENCE);
            ri.setComponent(new ComponentName(receiverPackage, receiverClass));
            ri.putExtra(EXTRA_KEY, remoteKey);
            return ri;
        }
        return null;
    }
}
