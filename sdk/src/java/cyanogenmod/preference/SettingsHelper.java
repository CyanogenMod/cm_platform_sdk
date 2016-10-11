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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cyanogenmod.providers.CMSettings;

public class SettingsHelper {

    private static final String SETTINGS_GLOBAL = Settings.Global.CONTENT_URI.toString();
    private static final String SETTINGS_SECURE = Settings.Secure.CONTENT_URI.toString();
    private static final String SETTINGS_SYSTEM = Settings.System.CONTENT_URI.toString();

    private static final String CMSETTINGS_GLOBAL = CMSettings.Global.CONTENT_URI.toString();
    private static final String CMSETTINGS_SECURE = CMSettings.Secure.CONTENT_URI.toString();
    private static final String CMSETTINGS_SYSTEM = CMSettings.System.CONTENT_URI.toString();

    private static SettingsHelper sInstance;

    private final Context mContext;
    private final Observatory mObservatory;

    private SettingsHelper(Context context) {
        mContext = context;
        mObservatory = new Observatory(context, new Handler());
    }

    public static synchronized SettingsHelper get(Context context) {
        if (sInstance == null) {
            sInstance = new SettingsHelper(context);
        }
        return sInstance;
    }

    public String getString(Uri settingsUri) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            return Settings.Secure.getString(resolver, uri.substring(SETTINGS_SECURE.length()));
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            return Settings.System.getString(resolver, uri.substring(SETTINGS_SYSTEM.length()));
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            return Settings.Global.getString(resolver, uri.substring(SETTINGS_GLOBAL.length()));
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            return CMSettings.Secure.getString(resolver, uri.substring(CMSETTINGS_SECURE.length()));
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            return CMSettings.System.getString(resolver, uri.substring(CMSETTINGS_SYSTEM.length()));
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            return CMSettings.Global.getString(resolver, uri.substring(CMSETTINGS_GLOBAL.length()));
        }
        return null;
    }

    public int getInt(Uri settingsUri, int def) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            return Settings.Secure.getInt(resolver, uri.substring(SETTINGS_SECURE.length()), def);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            return Settings.System.getInt(resolver, uri.substring(SETTINGS_SYSTEM.length()), def);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            return Settings.Global.getInt(resolver, uri.substring(SETTINGS_GLOBAL.length()), def);
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            return CMSettings.Secure.getInt(resolver, uri.substring(CMSETTINGS_SECURE.length()), def);
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            return CMSettings.System.getInt(resolver, uri.substring(CMSETTINGS_SYSTEM.length()), def);
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            return CMSettings.Global.getInt(resolver, uri.substring(CMSETTINGS_GLOBAL.length()), def);
        }
        return def;
    }

    public boolean getBoolean(Uri settingsUri, boolean def) {
        int value = getInt(settingsUri, def ? 1 : 0);
        return value == 1;
    }

    public void putString(Uri settingsUri, String value) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            Settings.Secure.putString(resolver, uri.substring(SETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            Settings.System.putString(resolver, uri.substring(SETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            Settings.Global.putString(resolver, uri.substring(SETTINGS_GLOBAL.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            CMSettings.Secure.putString(resolver, uri.substring(CMSETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            CMSettings.System.putString(resolver, uri.substring(CMSETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            CMSettings.Global.putString(resolver, uri.substring(CMSETTINGS_GLOBAL.length()), value);
        }
    }

    public void putInt(Uri settingsUri, int value) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            Settings.Secure.putInt(resolver, uri.substring(SETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            Settings.System.putInt(resolver, uri.substring(SETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            Settings.Global.putInt(resolver, uri.substring(SETTINGS_GLOBAL.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SECURE)) {
            CMSettings.Secure.putInt(resolver, uri.substring(CMSETTINGS_SECURE.length()), value);
        } else if (uri.startsWith(CMSETTINGS_SYSTEM)) {
            CMSettings.System.putInt(resolver, uri.substring(CMSETTINGS_SYSTEM.length()), value);
        } else if (uri.startsWith(CMSETTINGS_GLOBAL)) {
            CMSettings.Global.putInt(resolver, uri.substring(CMSETTINGS_GLOBAL.length()), value);
        }
    }

    public void putBoolean(Uri settingsUri, boolean value) {
        putInt(settingsUri, value ? 1 : 0);
    }

    public void startWatching(OnSettingsChangeListener listener, Uri... settingsUris) {
        mObservatory.register(listener, settingsUris);
    }

    public void stopWatching(OnSettingsChangeListener listener) {
        mObservatory.unregister(listener);
    }

    public interface OnSettingsChangeListener {
        public void onSettingsChanged(Uri settingsUri);
    }

    /**
     * A scalable ContentObserver that aggregates all listeners thru a single entrypoint.
     */
    private static class Observatory extends ContentObserver {

        private final Map<OnSettingsChangeListener, Set<Uri>> mTriggers = new ArrayMap<>();
        private final List<Uri> mRefs = new ArrayList<>();

        private final Context mContext;
        private final ContentResolver mResolver;

        public Observatory(Context context, Handler handler) {
            super(handler);
            mContext = context;
            mResolver = mContext.getContentResolver();
        }

        public void register(OnSettingsChangeListener listener, Uri... contentUris) {
            synchronized (mRefs) {
                Set<Uri> uris = mTriggers.get(listener);
                if (uris == null) {
                    uris = new ArraySet<Uri>();
                    mTriggers.put(listener, uris);
                }
                for (Uri contentUri : contentUris) {
                    uris.add(contentUri);
                    if (!mRefs.contains(contentUri)) {
                        mResolver.registerContentObserver(contentUri, false, this);
                        listener.onSettingsChanged(null);
                    }
                    mRefs.add(contentUri);
                }
            }
        }

        public void unregister(OnSettingsChangeListener listener) {
            synchronized (mRefs) {
                Set<Uri> uris = mTriggers.remove(listener);
                if (uris != null) {
                    for (Uri uri : uris) {
                        mRefs.remove(uri);
                    }
                }
                if (mRefs.size() == 0) {
                    mResolver.unregisterContentObserver(this);
                }
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mRefs) {
                super.onChange(selfChange, uri);

                final Set<OnSettingsChangeListener> notify = new ArraySet<>();
                for (Map.Entry<OnSettingsChangeListener, Set<Uri>> entry : mTriggers.entrySet()) {
                    if (entry.getValue().contains(uri)) {
                        notify.add(entry.getKey());
                    }
                }

                for (OnSettingsChangeListener listener : notify) {
                    listener.onSettingsChanged(uri);
                }
            }
        }
    }
}
