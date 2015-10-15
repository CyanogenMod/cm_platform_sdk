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

package cyanogenmod.providers;

import com.android.internal.util.ArrayUtils;

import android.content.ContentResolver;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CMSettings contains CM specific preferences in System, Secure, and Global.
 */
public final class CMSettings {
    private static final String TAG = "CMSettings";
    private static final boolean LOCAL_LOGV = false;

    public static final String AUTHORITY = "cmsettings";

    public static class CMSettingNotFoundException extends AndroidException {
        public CMSettingNotFoundException(String msg) {
            super(msg);
        }
    }

    // region Call Methods

    /**
     * @hide - User handle argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_USER_KEY = "_user";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'system' table.
     */
    public static final String CALL_METHOD_GET_SYSTEM = "GET_system";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'secure' table.
     */
    public static final String CALL_METHOD_GET_SECURE = "GET_secure";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'global' table.
     */
    public static final String CALL_METHOD_GET_GLOBAL = "GET_global";

    /**
     * @hide - Private call() method to write to 'system' table
     */
    public static final String CALL_METHOD_PUT_SYSTEM = "PUT_system";

    /**
     * @hide - Private call() method to write to 'secure' table
     */
    public static final String CALL_METHOD_PUT_SECURE = "PUT_secure";

    /**
     * @hide - Private call() method to write to 'global' table
     */
    public static final String CALL_METHOD_PUT_GLOBAL= "PUT_global";

    /**
     * @hide - Private call() method on CMSettingsProvider to migrate CM settings
     */
    public static final String CALL_METHOD_MIGRATE_SETTINGS = "migrate_settings";

    /**
     * @hide - Private call() method on CMSettingsProvider to migrate CM settings for a user
     */
    public static final String CALL_METHOD_MIGRATE_SETTINGS_FOR_USER = "migrate_settings_for_user";

    // endregion

    // Thread-safe.
    private static class NameValueCache {
        private final String mVersionSystemProperty;
        private final Uri mUri;

        private static final String[] SELECT_VALUE =
                new String[] { Settings.NameValueTable.VALUE };
        private static final String NAME_EQ_PLACEHOLDER = "name=?";

        // Must synchronize on 'this' to access mValues and mValuesVersion.
        private final HashMap<String, String> mValues = new HashMap<String, String>();
        private long mValuesVersion = 0;

        // Initially null; set lazily and held forever.  Synchronized on 'this'.
        private IContentProvider mContentProvider = null;

        // The method we'll call (or null, to not use) on the provider
        // for the fast path of retrieving settings.
        private final String mCallGetCommand;
        private final String mCallSetCommand;

        public NameValueCache(String versionSystemProperty, Uri uri,
                String getCommand, String setCommand) {
            mVersionSystemProperty = versionSystemProperty;
            mUri = uri;
            mCallGetCommand = getCommand;
            mCallSetCommand = setCommand;
        }

        private IContentProvider lazyGetProvider(ContentResolver cr) {
            IContentProvider cp;
            synchronized (this) {
                cp = mContentProvider;
                if (cp == null) {
                    cp = mContentProvider = cr.acquireProvider(mUri.getAuthority());
                }
            }
            return cp;
        }

        /**
         * Puts a string name/value pair into the content provider for the specified user.
         * @param cr The content resolver to use.
         * @param name The name of the key to put into the content provider.
         * @param value The value to put into the content provider.
         * @param userId The user id to use for the content provider.
         * @return Whether the put was successful.
         */
        public boolean putStringForUser(ContentResolver cr, String name, String value,
                final int userId) {
            try {
                Bundle arg = new Bundle();
                arg.putString(Settings.NameValueTable.VALUE, value);
                arg.putInt(CALL_METHOD_USER_KEY, userId);
                IContentProvider cp = lazyGetProvider(cr);
                cp.call(cr.getPackageName(), mCallSetCommand, name, arg);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't set key " + name + " in " + mUri, e);
                return false;
            }
            return true;
        }

        /**
         * Gets a string value with the specified name from the name/value cache if possible. If
         * not, it will use the content resolver and perform a query.
         * @param cr Content resolver to use if name/value cache does not contain the name or if
         *           the cache version is older than the current version.
         * @param name The name of the key to search for.
         * @param userId The user id of the cache to look in.
         * @return The string value of the specified key.
         */
        public String getStringForUser(ContentResolver cr, String name, final int userId) {
            final boolean isSelf = (userId == UserHandle.myUserId());
            if (isSelf) {
                if (LOCAL_LOGV) Log.d(TAG, "get setting for self");
                long newValuesVersion = SystemProperties.getLong(mVersionSystemProperty, 0);

                // Our own user's settings data uses a client-side cache
                synchronized (this) {
                    if (mValuesVersion != newValuesVersion) {
                        if (LOCAL_LOGV || false) {
                            Log.v(TAG, "invalidate [" + mUri.getLastPathSegment() + "]: current "
                                    + newValuesVersion + " != cached " + mValuesVersion);
                        }

                        mValues.clear();
                        mValuesVersion = newValuesVersion;
                    }

                    if (mValues.containsKey(name)) {
                        return mValues.get(name);  // Could be null, that's OK -- negative caching
                    }
                }
            } else {
                if (LOCAL_LOGV) Log.v(TAG, "get setting for user " + userId
                        + " by user " + UserHandle.myUserId() + " so skipping cache");
            }

            IContentProvider cp = lazyGetProvider(cr);

            // Try the fast path first, not using query().  If this
            // fails (alternate Settings provider that doesn't support
            // this interface?) then we fall back to the query/table
            // interface.
            if (mCallGetCommand != null) {
                try {
                    Bundle args = null;
                    if (!isSelf) {
                        args = new Bundle();
                        args.putInt(CALL_METHOD_USER_KEY, userId);
                    }
                    Bundle b = cp.call(cr.getPackageName(), mCallGetCommand, name, args);
                    if (b != null) {
                        String value = b.getPairValue();
                        // Don't update our cache for reads of other users' data
                        if (isSelf) {
                            synchronized (this) {
                                mValues.put(name, value);
                            }
                        } else {
                            if (LOCAL_LOGV) Log.i(TAG, "call-query of user " + userId
                                    + " by " + UserHandle.myUserId()
                                    + " so not updating cache");
                        }
                        return value;
                    }
                    // If the response Bundle is null, we fall through
                    // to the query interface below.
                } catch (RemoteException e) {
                    // Not supported by the remote side?  Fall through
                    // to query().
                }
            }

            Cursor c = null;
            try {
                c = cp.query(cr.getPackageName(), mUri, SELECT_VALUE, NAME_EQ_PLACEHOLDER,
                        new String[]{name}, null, null);
                if (c == null) {
                    Log.w(TAG, "Can't get key " + name + " from " + mUri);
                    return null;
                }

                String value = c.moveToNext() ? c.getString(0) : null;
                synchronized (this) {
                    mValues.put(name, value);
                }
                if (LOCAL_LOGV) {
                    Log.v(TAG, "cache miss [" + mUri.getLastPathSegment() + "]: " +
                            name + " = " + (value == null ? "(null)" : value));
                }
                return value;
            } catch (RemoteException e) {
                Log.w(TAG, "Can't get key " + name + " from " + mUri, e);
                return null;  // Return null, but don't cache it.
            } finally {
                if (c != null) c.close();
            }
        }
    }

    /**
     * System settings, containing miscellaneous CM system preferences. This table holds simple
     * name/value pairs. There are convenience functions for accessing individual settings entries.
     */
    public static final class System extends Settings.NameValueTable {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/system");

        public static final String SYS_PROP_CM_SETTING_VERSION = "sys.cm_settings_system_version";

        /** @hide */
        public static interface Validator {
            public boolean validate(String value);
        }

        private static final NameValueCache sNameValueCache = new NameValueCache(
                SYS_PROP_CM_SETTING_VERSION,
                CONTENT_URI,
                CALL_METHOD_GET_SYSTEM,
                CALL_METHOD_PUT_SYSTEM);

        private static final Validator sBooleanValidator =
                new DiscreteValueValidator(new String[] {"0", "1"});

        private static final Validator sNonNegativeIntegerValidator = new Validator() {
            @Override
            public boolean validate(String value) {
                try {
                    return Integer.parseInt(value) >= 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        };

        private static final Validator sUriValidator = new Validator() {
            @Override
            public boolean validate(String value) {
                try {
                    Uri.decode(value);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        };

        private static final Validator sColorValidator =
                new InclusiveIntegerRangeValidator(Integer.MIN_VALUE, Integer.MAX_VALUE);

        private static final Validator sAlwaysTrueValidator = new Validator() {
            @Override
            public boolean validate(String value) {
                return true;
            }
        };

        // region Methods

        /**
         * Put a delimited list as a string
         * @param resolver to access the database with
         * @param name to store
         * @param delimiter to split
         * @param list to join and store
         * @hide
         */
        public static void putListAsDelimitedString(ContentResolver resolver, String name,
                                                    String delimiter, List<String> list) {
            String store = TextUtils.join(delimiter, list);
            putString(resolver, name, store);
        }

        /**
         * Get a delimited string returned as a list
         * @param resolver to access the database with
         * @param name to store
         * @param delimiter to split the list with
         * @return list of strings for a specific Settings.Secure item
         * @hide
         */
        public static List<String> getDelimitedStringAsList(ContentResolver resolver, String name,
                                                            String delimiter) {
            String baseString = getString(resolver, name);
            List<String> list = new ArrayList<String>();
            if (!TextUtils.isEmpty(baseString)) {
                final String[] array = TextUtils.split(baseString, Pattern.quote(delimiter));
                for (String item : array) {
                    if (TextUtils.isEmpty(item)) {
                        continue;
                    }
                    list.add(item);
                }
            }
            return list;
        }

        /**
         * Construct the content URI for a particular name/value pair, useful for monitoring changes
         * with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI
         */
        public static Uri getUriFor(String name) {
            return Settings.NameValueTable.getUriFor(CONTENT_URI, name);
        }

        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public static String getString(ContentResolver resolver, String name) {
            return getStringForUser(resolver, name, UserHandle.myUserId());
        }

        /** @hide */
        public static String getStringForUser(ContentResolver resolver, String name,
                int userId) {
            return sNameValueCache.getStringForUser(resolver, name, userId);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver, String name, String value) {
            return putStringForUser(resolver, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putStringForUser(ContentResolver resolver, String name, String value,
               int userId) {
            return sNameValueCache.putStringForUser(resolver, name, value, userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.  The default value will be returned if the setting is
         * not defined or not an integer.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return getIntForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int def, int userId) {
            String v = getStringForUser(cr, name, userId);
            try {
                return v != null ? Integer.parseInt(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for retrieving a single settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         *
         * @return The setting's current value.
         */
        public static int getInt(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getIntForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String v = getStringForUser(cr, name, userId);
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as an
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver cr, String name, int value) {
            return putIntForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putIntForUser(ContentResolver cr, String name, int value,
                int userId) {
            return putStringForUser(cr, name, Integer.toString(value), userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.  The default value will be returned if the setting is
         * not defined or not a {@code long}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            return getLongForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, long def,
                int userId) {
            String valString = getStringForUser(cr, name, userId);
            long value;
            try {
                value = valString != null ? Long.parseLong(valString) : def;
            } catch (NumberFormatException e) {
                value = def;
            }
            return value;
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @return The setting's current value.
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         */
        public static long getLong(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getLongForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String valString = getStringForUser(cr, name, userId);
            try {
                return Long.parseLong(valString);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as a long
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putLong(ContentResolver cr, String name, long value) {
            return putLongForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putLongForUser(ContentResolver cr, String name, long value,
                int userId) {
            return putStringForUser(cr, name, Long.toString(value), userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a floating point number.  Note that internally setting values are
         * always stored as strings; this function converts the string to an
         * float for you. The default value will be returned if the setting
         * is not defined or not a valid float.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid float.
         */
        public static float getFloat(ContentResolver cr, String name, float def) {
            return getFloatForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, float def,
                int userId) {
            String v = getStringForUser(cr, name, userId);
            try {
                return v != null ? Float.parseFloat(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a float.  Note that internally setting values are always
         * stored as strings; this function converts the string to a float
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not a float.
         *
         * @return The setting's current value.
         */
        public static float getFloat(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getFloatForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String v = getStringForUser(cr, name, userId);
            if (v == null) {
                throw new CMSettingNotFoundException(name);
            }
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as a
         * floating point number. This will either create a new entry in the
         * table if the given name does not exist, or modify the value of the
         * existing row with that name.  Note that internally setting values
         * are always stored as strings, so this function converts the given
         * value to a string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putFloat(ContentResolver cr, String name, float value) {
            return putFloatForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putFloatForUser(ContentResolver cr, String name, float value,
                int userId) {
            return putStringForUser(cr, name, Float.toString(value), userId);
        }

        // endregion

        private static final class DiscreteValueValidator implements Validator {
            private final String[] mValues;

            public DiscreteValueValidator(String[] values) {
                mValues = values;
            }

            @Override
            public boolean validate(String value) {
                return ArrayUtils.contains(mValues, value);
            }
        }

        private static final class InclusiveIntegerRangeValidator implements Validator {
            private final int mMin;
            private final int mMax;

            public InclusiveIntegerRangeValidator(int min, int max) {
                mMin = min;
                mMax = max;
            }

            @Override
            public boolean validate(String value) {
                try {
                    final int intValue = Integer.parseInt(value);
                    return intValue >= mMin && intValue <= mMax;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        private static final class InclusiveFloatRangeValidator implements Validator {
            private final float mMin;
            private final float mMax;

            public InclusiveFloatRangeValidator(float min, float max) {
                mMin = min;
                mMax = max;
            }

            @Override
            public boolean validate(String value) {
                try {
                    final float floatValue = Float.parseFloat(value);
                    return floatValue >= mMin && floatValue <= mMax;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        private static final class DelimitedListValidator implements Validator {
            private final ArraySet<String> mValidValueSet;
            private final String mDelimiter;
            private final boolean mAllowEmptyList;

            public DelimitedListValidator(String[] validValues, String delimiter,
                    boolean allowEmptyList) {
                mValidValueSet = new ArraySet<String>(Arrays.asList(validValues));
                mDelimiter = delimiter;
                mAllowEmptyList = allowEmptyList;
            }

            @Override
            public boolean validate(String value) {
                ArraySet<String> values = new ArraySet<String>();
                if (!TextUtils.isEmpty(value)) {
                    final String[] array = TextUtils.split(value, Pattern.quote(mDelimiter));
                    for (String item : array) {
                        if (TextUtils.isEmpty(item)) {
                            continue;
                        }
                        values.add(item);
                    }
                }
                if (values.size() > 0) {
                    values.removeAll(mValidValueSet);
                    // values.size() will be non-zero if it contains any values not in
                    // mValidValueSet
                    return values.size() == 0;
                } else if (mAllowEmptyList) {
                    return true;
                }

                return false;
            }
        }

        // region System Settings

        /**
         * Quick Settings Quick Pulldown
         * 0 = off, 1 = right, 2 = left
         * @hide
         */
        public static final String QS_QUICK_PULLDOWN = "qs_quick_pulldown";

        /** @hide */
        public static final Validator QS_QUICK_PULLDOWN_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 2);

        /**
         * Whether to attach a queue to media notifications.
         * 0 = 0ff, 1 = on
         * @hide
         */
        public static final String NOTIFICATION_PLAY_QUEUE = "notification_play_queue";

        /** @hide */
        public static final Validator NOTIFICATION_PLAY_QUEUE_VALIDATOR = sBooleanValidator;

        /**
         * Whether the HighTouchSensitivity is activated or not.
         * 0 = off, 1 = on
         * @hide
         */
        public static final String HIGH_TOUCH_SENSITIVITY_ENABLE =
                "high_touch_sensitivity_enable";

        /** @hide */
        public static final Validator HIGH_TOUCH_SENSITIVITY_ENABLE_VALIDATOR =
                sBooleanValidator;

        /**
         * Show the pending notification counts as overlays on the status bar
         * @hide
         */
        public static final String SYSTEM_PROFILES_ENABLED = "system_profiles_enabled";

        /** @hide */
        public static final Validator SYSTEM_PROFILES_ENABLED_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to hide the clock, show it in the right or left
         * position or show it in the center
         * 0: don't show the clock
         * 1: show the clock in the right position (LTR)
         * 2: show the clock in the center
         * 3: show the clock in the left position (LTR)
         * default: 1
         * @hide
         */
        public static final String STATUS_BAR_CLOCK = "status_bar_clock";

        /** @hide */
        public static final Validator STATUS_BAR_CLOCK_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 3);

        /**
         * Display style of AM/PM next to clock in status bar
         * 0: Normal display (Eclair stock)
         * 1: Small display (Froyo stock)
         * 2: No display (Gingerbread/ICS stock)
         * default: 2
         * @hide
         */
        public static final String STATUS_BAR_AM_PM = "status_bar_am_pm";

        /** @hide */
        public static final Validator STATUS_BAR_AM_PM_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 2);

        /**
         * Display style of the status bar battery information
         * 0: Display the battery an icon in portrait mode
         * 2: Display the battery as a circle
         * 4: Hide the battery status information
         * 5: Display the battery an icon in landscape mode
         * 6: Display the battery as plain text
         * default: 0
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

        /** @hide */
        public static final Validator STATUS_BAR_BATTERY_STYLE_VALIDATOR =
                new DiscreteValueValidator(new String[] {"0", "2", "4", "5", "6"});

        /**
         * Status bar battery %
         * 0: Hide the battery percentage
         * 1: Display the battery percentage inside the icon
         * 2: Display the battery percentage next to the icon
         * @hide
         */
        public static final String STATUS_BAR_SHOW_BATTERY_PERCENT =
                "status_bar_show_battery_percent";

        /** @hide */
        public static final Validator STATUS_BAR_SHOW_BATTERY_PERCENT_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 2);

        /**
         * Whether the phone ringtone should be played in an increasing manner
         * @hide
         */
        public static final String INCREASING_RING = "increasing_ring";

        /** @hide */
        public static final Validator INCREASING_RING_VALIDATOR = sBooleanValidator;

        /**
         * Start volume fraction for increasing ring volume
         * @hide
         */
        public static final String INCREASING_RING_START_VOLUME = "increasing_ring_start_vol";

        /** @hide */
        public static final Validator INCREASING_RING_START_VOLUME_VALIDATOR =
                new InclusiveFloatRangeValidator(0, 1);

        /**
         * Ramp up time (seconds) for increasing ring
         * @hide
         */
        public static final String INCREASING_RING_RAMP_UP_TIME = "increasing_ring_ramp_up_time";

        /** @hide */
        public static final Validator INCREASING_RING_RAMP_UP_TIME_VALIDATOR =
                new InclusiveIntegerRangeValidator(5, 60);

        /**
         * Volume Adjust Sounds Enable, This is the noise made when using volume hard buttons
         * Defaults to 1 - sounds enabled
         * @hide
         */
        public static final String VOLUME_ADJUST_SOUNDS_ENABLED = "volume_adjust_sounds_enabled";

        /** @hide */
        public static final Validator VOLUME_ADJUST_SOUNDS_ENABLED_VALIDATOR =
                sBooleanValidator;

        /**
         * Navigation controls to Use
         * @hide
         */
        public static final String NAV_BUTTONS = "nav_buttons";

        /** @hide */
        public static final Validator NAV_BUTTONS_VALIDATOR =
                new DelimitedListValidator(new String[] {"empty", "home", "back", "search",
                        "recent", "menu0", "menu1", "menu2", "dpad_left", "dpad_right"}, "|", true);

        /**
         * Volume key controls ringtone or media sound stream
         * @hide
         */
        public static final String VOLUME_KEYS_CONTROL_RING_STREAM =
                "volume_keys_control_ring_stream";

        /** @hide */
        public static final Validator VOLUME_KEYS_CONTROL_RING_STREAM_VALIDATOR =
                sBooleanValidator;

        /**
         * boolean value. toggles using arrow key locations on nav bar
         * as left and right dpad keys
         * @hide
         */
        public static final String NAVIGATION_BAR_MENU_ARROW_KEYS = "navigation_bar_menu_arrow_keys";

        /** @hide */
        public static final Validator NAVIGATION_BAR_MENU_ARROW_KEYS_VALIDATOR =
                sBooleanValidator;

        /**
         * Action to perform when the home key is long-pressed.
         * (Default can be configured via config_longPressOnHomeBehavior)
         * 0 - Nothing
         * 1 - Menu
         * 2 - App-switch
         * 3 - Search
         * 4 - Voice search
         * 5 - In-app search
         * 6 - Launch Camera
         * 7 - Action Sleep
         * 8 - Last app
         * @hide
         */
        public static final String KEY_HOME_LONG_PRESS_ACTION = "key_home_long_press_action";

        /** @hide */
        public static final Validator KEY_HOME_LONG_PRESS_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Action to perform when the home key is double-tapped.
         * (Default can be configured via config_doubleTapOnHomeBehavior)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_HOME_DOUBLE_TAP_ACTION = "key_home_double_tap_action";

        /** @hide */
        public static final Validator KEY_HOME_DOUBLE_TAP_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Whether to wake the screen with the back key, the value is boolean.
         * @hide
         */
        public static final String BACK_WAKE_SCREEN = "back_wake_screen";

        /** @hide */
        public static final Validator BACK_WAKE_SCREEN_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to wake the screen with the menu key, the value is boolean.
         * @hide
         */
        public static final String MENU_WAKE_SCREEN = "menu_wake_screen";

        /** @hide */
        public static final Validator MENU_WAKE_SCREENN_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to wake the screen with the volume keys, the value is boolean.
         * @hide
         */
        public static final String VOLUME_WAKE_SCREEN = "volume_wake_screen";

        /** @hide */
        public static final Validator VOLUME_WAKE_SCREEN_VALIDATOR =
                sBooleanValidator;

        /**
         * Action to perform when the menu key is pressed. (Default is 1)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_MENU_ACTION = "key_menu_action";

        /** @hide */
        public static final Validator KEY_MENU_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Action to perform when the menu key is long-pressed.
         * (Default is 0 on devices with a search key, 3 on devices without)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_MENU_LONG_PRESS_ACTION = "key_menu_long_press_action";

        /** @hide */
        public static final Validator KEY_MENU_LONG_PRESS_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Action to perform when the assistant (search) key is pressed. (Default is 3)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_ACTION = "key_assist_action";

        /** @hide */
        public static final Validator KEY_ASSIST_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Action to perform when the assistant (search) key is long-pressed. (Default is 4)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_LONG_PRESS_ACTION = "key_assist_long_press_action";

        /** @hide */
        public static final Validator KEY_ASSIST_LONG_PRESS_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Action to perform when the app switch key is pressed. (Default is 2)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_ACTION = "key_app_switch_action";

        /** @hide */
        public static final Validator KEY_APP_SWITCH_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Action to perform when the app switch key is long-pressed. (Default is 0)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_LONG_PRESS_ACTION = "key_app_switch_long_press_action";

        /** @hide */
        public static final Validator KEY_APP_SWITCH_LONG_PRESS_ACTION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 8);

        /**
         * Whether to wake the screen with the home key, the value is boolean.
         * @hide
         */
        public static final String HOME_WAKE_SCREEN = "home_wake_screen";

        /** @hide */
        public static final Validator HOME_WAKE_SCREEN_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to wake the screen with the assist key, the value is boolean.
         * @hide
         */
        public static final String ASSIST_WAKE_SCREEN = "assist_wake_screen";

        /** @hide */
        public static final Validator ASSIST_WAKE_SCREEN_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to wake the screen with the app switch key, the value is boolean.
         * @hide
         */
        public static final String APP_SWITCH_WAKE_SCREEN = "app_switch_wake_screen";

        /** @hide */
        public static final Validator APP_SWITCH_WAKE_SCREEN_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to wake the screen with the camera key half-press.
         * @hide
         */
        public static final String CAMERA_WAKE_SCREEN = "camera_wake_screen";

        /** @hide */
        public static final Validator CAMERA_WAKE_SCREEN_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether or not to send device back to sleep if Camera button is released ("Peek")
         * @hide
         */
        public static final String CAMERA_SLEEP_ON_RELEASE = "camera_sleep_on_release";

        /** @hide */
        public static final Validator CAMERA_SLEEP_ON_RELEASE_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to launch secure camera app when key is longpressed
         * @hide
         */
        public static final String CAMERA_LAUNCH = "camera_launch";

        /** @hide */
        public static final Validator CAMERA_LAUNCH_VALIDATOR =
                sBooleanValidator;

        /**
         * Swap volume buttons when the screen is rotated
         * 0 - Disabled
         * 1 - Enabled (screen is rotated by 90 or 180 degrees: phone, hybrid)
         * 2 - Enabled (screen is rotated by 180 or 270 degrees: tablet)
         * @hide
         */
        public static final String SWAP_VOLUME_KEYS_ON_ROTATION = "swap_volume_keys_on_rotation";

        /** @hide */
        public static final Validator SWAP_VOLUME_KEYS_ON_ROTATION_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 2);

        /**
         * Whether the battery light should be enabled (if hardware supports it)
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String BATTERY_LIGHT_ENABLED = "battery_light_enabled";

        /** @hide */
        public static final Validator BATTERY_LIGHT_ENABLED_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether the battery LED should repeatedly flash when the battery is low
         * on charge. The value is boolean (1 or 0).
         * @hide
         */
        public static final String BATTERY_LIGHT_PULSE = "battery_light_pulse";

        /** @hide */
        public static final Validator BATTERY_LIGHT_PULSE_VALIDATOR =
                sBooleanValidator;

        /**
         * What color to use for the battery LED while charging - low
         * @hide
         */
        public static final String BATTERY_LIGHT_LOW_COLOR = "battery_light_low_color";

        /** @hide */
        public static final Validator BATTERY_LIGHT_LOW_COLOR_VALIDATOR =
                sColorValidator;

        /**
         * What color to use for the battery LED while charging - medium
         * @hide
         */
        public static final String BATTERY_LIGHT_MEDIUM_COLOR = "battery_light_medium_color";

        /** @hide */
        public static final Validator BATTERY_LIGHT_MEDIUM_COLOR_VALIDATOR =
                sColorValidator;

        /**
         * What color to use for the battery LED while charging - full
         * @hide
         */
        public static final String BATTERY_LIGHT_FULL_COLOR = "battery_light_full_color";

        /** @hide */
        public static final Validator BATTERY_LIGHT_FULL_COLOR_VALIDATOR =
                sColorValidator;

        /**
         * Sprint MWI Quirk: Show message wait indicator notifications
         * @hide
         */
        public static final String ENABLE_MWI_NOTIFICATION = "enable_mwi_notification";

        /** @hide */
        public static final Validator ENABLE_MWI_NOTIFICATION_VALIDATOR =
                sBooleanValidator;

        /**
         * Check the proximity sensor during wakeup
         * @hide
         */
        public static final String PROXIMITY_ON_WAKE = "proximity_on_wake";

        /** @hide */
        public static final Validator PROXIMITY_ON_WAKE_VALIDATOR =
                sBooleanValidator;

        /**
         * Enable looking up of phone numbers of nearby places
         *
         * @hide
         */
        public static final String ENABLE_FORWARD_LOOKUP = "enable_forward_lookup";

        /** @hide */
        public static final Validator ENABLE_FORWARD_LOOKUP_VALIDATOR =
                sBooleanValidator;

        /**
         * Enable looking up of phone numbers of people
         *
         * @hide
         */
        public static final String ENABLE_PEOPLE_LOOKUP = "enable_people_lookup";

        /** @hide */
        public static final Validator ENABLE_PEOPLE_LOOKUP_VALIDATOR =
                sBooleanValidator;

        /**
         * Enable looking up of information of phone numbers not in the contacts
         *
         * @hide
         */
        public static final String ENABLE_REVERSE_LOOKUP = "enable_reverse_lookup";

        /** @hide */
        public static final Validator ENABLE_REVERSE_LOOKUP_VALIDATOR =
                sBooleanValidator;

        /**
         * The forward lookup provider
         *
         * @hide
         */
        public static final String FORWARD_LOOKUP_PROVIDER = "forward_lookup_provider";

        /** @hide */
        public static final Validator FORWARD_LOOKUP_PROVIDER_VALIDATOR = sAlwaysTrueValidator;

        /**
         * The people lookup provider
         *
         * @hide
         */
        public static final String PEOPLE_LOOKUP_PROVIDER = "people_lookup_provider";

        /** @hide */
        public static final Validator PEOPLE_LOOKUP_PROVIDER_VALIDATOR = sAlwaysTrueValidator;

        /**
         * The reverse lookup provider
         *
         * @hide
         */
        public static final String REVERSE_LOOKUP_PROVIDER = "reverse_lookup_provider";

        /** @hide */
        public static final Validator REVERSE_LOOKUP_PROVIDER_VALIDATOR = sAlwaysTrueValidator;

        /**
         * The OpenCNAM paid account ID
         *
         * @hide
         */
        public static final String DIALER_OPENCNAM_ACCOUNT_SID = "dialer_opencnam_account_sid";

        /** @hide */
        public static final Validator DIALER_OPENCNAM_ACCOUNT_SID_VALIDATOR =
                sAlwaysTrueValidator;

        /**
         * The OpenCNAM authentication token
         *
         * @hide
         */
        public static final String DIALER_OPENCNAM_AUTH_TOKEN = "dialer_opencnam_auth_token";

        /** @hide */
        public static final Validator DIALER_OPENCNAM_AUTH_TOKEN_VALIDATOR =
                sAlwaysTrueValidator;

        /**
         * Color temperature of the display during the day
         * @hide
         */
        public static final String DISPLAY_TEMPERATURE_DAY = "display_temperature_day";

        /** @hide */
        public static final Validator DISPLAY_TEMPERATURE_DAY_VALIDATOR =
                new InclusiveIntegerRangeValidator(1000, 10000);

        /**
         * Color temperature of the display at night
         * @hide
         */
        public static final String DISPLAY_TEMPERATURE_NIGHT = "display_temperature_night";

        /** @hide */
        public static final Validator DISPLAY_TEMPERATURE_NIGHT_VALIDATOR =
                new InclusiveIntegerRangeValidator(1000, 10000);

        /**
         * Display color temperature adjustment mode, one of DAY (default), NIGHT, or AUTO.
         * @hide
         */
        public static final String DISPLAY_TEMPERATURE_MODE = "display_temperature_mode";

        /** @hide */
        public static final Validator DISPLAY_TEMPERATURE_MODE_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 4);

        /**
         * Automatic outdoor mode
         * @hide
         */
        public static final String DISPLAY_AUTO_OUTDOOR_MODE = "display_auto_outdoor_mode";

        /** @hide */
        public static final Validator DISPLAY_AUTO_OUTDOOR_MODE_VALIDATOR =
                sBooleanValidator;

        /**
         * Use display power saving features such as CABC or CABL
         * @hide
         */
        public static final String DISPLAY_LOW_POWER = "display_low_power";

        /** @hide */
        public static final Validator DISPLAY_LOW_POWER_VALIDATOR =
                sBooleanValidator;

        /**
         * Use color enhancement feature of display
         * @hide
         */
        public static final String DISPLAY_COLOR_ENHANCE = "display_color_enhance";

        /** @hide */
        public static final Validator DISPLAY_COLOR_ENHANCE_VALIDATOR =
                sBooleanValidator;

        /**
         * Manual display color adjustments (RGB values as floats, separated by spaces)
         * @hide
         */
        public static final String DISPLAY_COLOR_ADJUSTMENT = "display_color_adjustment";

        /** @hide */
        public static final Validator DISPLAY_COLOR_ADJUSTMENT_VALIDATOR =
                new Validator() {
                    @Override
                    public boolean validate(String value) {
                        String[] colorAdjustment = value == null ?
                                null : value.split(" ");
                        if (colorAdjustment != null && colorAdjustment.length != 3) {
                            return false;
                        }
                        Validator floatValidator = new InclusiveFloatRangeValidator(0, 1);
                        return colorAdjustment == null ||
                                floatValidator.validate(colorAdjustment[0]) &&
                                floatValidator.validate(colorAdjustment[1]) &&
                                floatValidator.validate(colorAdjustment[2]);
                    }
                };

        /**
         * Did we tell about how they can stop breaking their eyes?
         * @hide
         */
        public static final String LIVE_DISPLAY_HINTED = "live_display_hinted";

        /** @hide */
        public static final Validator LIVE_DISPLAY_HINTED_VALIDATOR =
                sBooleanValidator;

        /**
         *  Enable statusbar double tap gesture on to put device to sleep
         * @hide
         */
        public static final String DOUBLE_TAP_SLEEP_GESTURE = "double_tap_sleep_gesture";

        /** @hide */
        public static final Validator DOUBLE_TAP_SLEEP_GESTURE_VALIDATOR =
                sBooleanValidator;

        /**
         * Boolean value on whether to show weather in the statusbar
         * @hide
         */
        public static final String STATUS_BAR_SHOW_WEATHER = "status_bar_show_weather";

        /** @hide */
        public static final Validator STATUS_BAR_SHOW_WEATHER_VALIDATOR =
                sBooleanValidator;

        /**
         * Show search bar in recents
         * @hide
         */
        public static final String RECENTS_SHOW_SEARCH_BAR = "recents_show_search_bar";

        /** @hide */
        public static final Validator RECENTS_SHOW_SEARCH_BAR_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether navigation bar is placed on the left side in landscape mode
         * @hide
         */
        public static final String NAVBAR_LEFT_IN_LANDSCAPE = "navigation_bar_left";

        /** @hide */
        public static final Validator NAVBAR_LEFT_IN_LANDSCAPE_VALIDATOR =
                sBooleanValidator;

        /**
         * Locale for secondary overlay on dialer for t9 search input
         * @hide
         */
        public static final String T9_SEARCH_INPUT_LOCALE = "t9_search_input_locale";

        /** @hide */
        public static final Validator T9_SEARCH_INPUT_LOCALE_VALIDATOR =
                new Validator() {
                    @Override
                    public boolean validate(String value) {
                        final Locale locale = new Locale(value);
                        return ArrayUtils.contains(Locale.getAvailableLocales(), locale);
                    }
                };

        /**
         * If all file types can be accepted over Bluetooth OBEX.
         * @hide
         */
        public static final String BLUETOOTH_ACCEPT_ALL_FILES =
                "bluetooth_accept_all_files";

        /** @hide */
        public static final Validator BLUETOOTH_ACCEPT_ALL_FILES_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to scramble a pin unlock layout
         * @hide
         */
        public static final String LOCKSCREEN_PIN_SCRAMBLE_LAYOUT =
                "lockscreen_scramble_pin_layout";

        /** @hide */
        public static final Validator LOCKSCREEN_PIN_SCRAMBLE_LAYOUT_VALIDATOR =
                sBooleanValidator;

        /**
         * @hide
         */
        public static final String SHOW_ALARM_ICON = "show_alarm_icon";

        /** @hide */
        public static final Validator SHOW_ALARM_ICON_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to show the IME switcher in the status bar
         * @hide
         */
        public static final String STATUS_BAR_IME_SWITCHER = "status_bar_ime_switcher";

        /** @hide */
        public static final Validator STATUS_BAR_IME_SWITCHER_VALIDATOR =
                sBooleanValidator;

        /** Whether to allow one finger quick settings expansion on the right side of the statusbar.
         *
         * @hide
         */
        public static final String STATUS_BAR_QUICK_QS_PULLDOWN = "status_bar_quick_qs_pulldown";

        /** @hide */
        public static final Validator STATUS_BAR_QUICK_QS_PULLDOWN_VALIDATOR =
                sBooleanValidator;

        /** Whether to show the brightness slider in quick settings panel.
         *
         * @hide
         */
        public static final String QS_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";

        /** @hide */
        public static final Validator QS_SHOW_BRIGHTNESS_SLIDER_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to control brightness from status bar
         *
         * @hide
         */
        public static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";

        /** @hide */
        public static final Validator STATUS_BAR_BRIGHTNESS_CONTROL_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether or not volume button music controls should be enabled to seek media tracks
         * @hide
         */
        public static final String VOLBTN_MUSIC_CONTROLS = "volbtn_music_controls";

        /** @hide */
        public static final Validator VOLBTN_MUSIC_CONTROLS_VALIDATOR =
                sBooleanValidator;

        /**
         * Use EdgeGesture Service for system gestures in PhoneWindowManager
         * @hide
         */
        public static final String USE_EDGE_SERVICE_FOR_GESTURES = "edge_service_for_gestures";

        /** @hide */
        public static final Validator USE_EDGE_SERVICE_FOR_GESTURES_VALIDATOR =
                sBooleanValidator;

        /**
         * Show the pending notification counts as overlays on the status bar
         * @hide
         */
        public static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";

        /** @hide */
        public static final Validator STATUS_BAR_NOTIF_COUNT_VALIDATOR =
                sBooleanValidator;

        /**
         * Call recording format value
         * 0: AMR_WB
         * 1: MPEG_4
         * Default: 0
         * @hide
         */
        public static final String CALL_RECORDING_FORMAT = "call_recording_format";

        /** @hide */
        public static final Validator CALL_RECORDING_FORMAT_VALIDATOR =
                new InclusiveIntegerRangeValidator(0, 1);

        /**
         * Contains the notifications light maximum brightness to use.
         * Values range from 1 to 255
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL =
                "notification_light_brightness_level";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_VALIDATOR =
                new InclusiveIntegerRangeValidator(1, 255);

        /**
         * Whether to use the all the LEDs for the notifications or just one.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE =
                "notification_light_multiple_leds_enable";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE_VALIDATOR =
                sBooleanValidator;

        /**
         * Whether to allow notifications with the screen on or DayDreams.
         * The value is boolean (1 or 0). Default will always be false.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_SCREEN_ON =
                "notification_light_screen_on_enable";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_SCREEN_ON_VALIDATOR =
                sBooleanValidator;

        /**
         * What color to use for the notification LED by default
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR =
                "notification_light_pulse_default_color";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR_VALIDATOR =
                sColorValidator;

        /**
         * How long to flash the notification LED by default
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON =
                "notification_light_pulse_default_led_on";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON_VALIDATOR =
                sNonNegativeIntegerValidator;

        /**
         * How long to wait between flashes for the notification LED by default
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF =
                "notification_light_pulse_default_led_off";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF_VALIDATOR =
                sNonNegativeIntegerValidator;

        /**
         * What color to use for the missed call notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CALL_COLOR =
                "notification_light_pulse_call_color";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_CALL_COLOR_VALIDATOR =
                sColorValidator;

        /**
         * How long to flash the missed call notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_ON =
                "notification_light_pulse_call_led_on";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_CALL_LED_ON_VALIDATOR =
                sNonNegativeIntegerValidator;

        /**
         * How long to wait between flashes for the missed call notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF =
                "notification_light_pulse_call_led_off";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF_VALIDATOR =
                sNonNegativeIntegerValidator;

        /**
         * What color to use for the voicemail notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR =
                "notification_light_pulse_vmail_color";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR_VALIDATOR =
                sColorValidator;

        /**
         * How long to flash the voicemail notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON =
                "notification_light_pulse_vmail_led_on";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON_VALIDATOR =
                sNonNegativeIntegerValidator;

        /**
         * How long to wait between flashes for the voicemail notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF =
                "notification_light_pulse_vmail_led_off";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF_VALIDATOR =
                sNonNegativeIntegerValidator;

        /**
         * Whether to use the custom LED values for the notification pulse LED.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE =
                "notification_light_pulse_custom_enable";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE_VALIDATOR =
                sBooleanValidator;

        /**
         * Which custom LED values to use for the notification pulse LED.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES =
                "notification_light_pulse_custom_values";

        /** @hide */
        public static final Validator NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES_VALIDATOR =
                new Validator() {
                    @Override
                    public boolean validate(String value) {
                        if (TextUtils.isEmpty(value)) {
                            return true;
                        }

                        for (String packageValuesString : value.split("\\|")) {
                            String[] packageValues = packageValuesString.split("=");
                            if (packageValues.length != 2) {
                                if (LOCAL_LOGV) {
                                    Log.d(TAG, "Incorrect number of package values: "
                                            + packageValues.length);
                                }
                                return false;
                            }
                            String packageName = packageValues[0];
                            if (TextUtils.isEmpty(packageName)) {
                                if (LOCAL_LOGV)  Log.d(TAG, "Empty package name");
                                return false;
                            }
                            String[] values = packageValues[1].split(";");
                            if (values.length != 3) {
                                if (LOCAL_LOGV) {
                                    Log.d(TAG, "Incorrect number of values: " + values.length);
                                }
                                return false;
                            }
                            try {
                                // values[0] is LED color
                                if (!sColorValidator.validate(values[0])) {
                                    if (LOCAL_LOGV) {
                                        Log.d(TAG, "Invalid LED color (" + values[0] + ") for "
                                                + packageName);
                                    }
                                    return false;
                                }
                                // values[1] is the LED on time and should be non-negative
                                if (!sNonNegativeIntegerValidator.validate(values[1])) {
                                    if (LOCAL_LOGV) {
                                        Log.d(TAG, "Invalid LED on time (" + values[1] + ") for "
                                                + packageName);
                                    }
                                    return false;
                                }
                                // values[1] is the LED off time and should be non-negative
                                if (!sNonNegativeIntegerValidator.validate(values[2])) {
                                    if (LOCAL_LOGV) {
                                        Log.d(TAG, "Invalid LED off time (" + values[2] + ") for "
                                                + packageName);
                                    }
                                    return false;
                                }
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        }
                        // if we make it all the way through then the data is considered valid
                        return true;
                    }
                };

        /**
         * @hide
         */
        public static final String[] LEGACY_SYSTEM_SETTINGS = new String[]{
                CMSettings.System.QS_QUICK_PULLDOWN,
                CMSettings.System.NAV_BUTTONS,
                CMSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                CMSettings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                CMSettings.System.BACK_WAKE_SCREEN,
                CMSettings.System.MENU_WAKE_SCREEN,
                CMSettings.System.VOLUME_WAKE_SCREEN,
                CMSettings.System.KEY_MENU_ACTION,
                CMSettings.System.KEY_MENU_LONG_PRESS_ACTION,
                CMSettings.System.KEY_ASSIST_ACTION,
                CMSettings.System.KEY_ASSIST_LONG_PRESS_ACTION,
                CMSettings.System.KEY_APP_SWITCH_ACTION,
                CMSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                CMSettings.System.HOME_WAKE_SCREEN,
                CMSettings.System.ASSIST_WAKE_SCREEN,
                CMSettings.System.APP_SWITCH_WAKE_SCREEN,
                CMSettings.System.CAMERA_WAKE_SCREEN,
                CMSettings.System.CAMERA_SLEEP_ON_RELEASE,
                CMSettings.System.CAMERA_LAUNCH,
                CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                CMSettings.System.BATTERY_LIGHT_ENABLED,
                CMSettings.System.BATTERY_LIGHT_PULSE,
                CMSettings.System.BATTERY_LIGHT_LOW_COLOR,
                CMSettings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                CMSettings.System.BATTERY_LIGHT_FULL_COLOR,
                CMSettings.System.ENABLE_MWI_NOTIFICATION,
                CMSettings.System.PROXIMITY_ON_WAKE,
                CMSettings.System.ENABLE_FORWARD_LOOKUP,
                CMSettings.System.ENABLE_PEOPLE_LOOKUP,
                CMSettings.System.ENABLE_REVERSE_LOOKUP,
                CMSettings.System.FORWARD_LOOKUP_PROVIDER,
                CMSettings.System.PEOPLE_LOOKUP_PROVIDER,
                CMSettings.System.REVERSE_LOOKUP_PROVIDER,
                CMSettings.System.DIALER_OPENCNAM_ACCOUNT_SID,
                CMSettings.System.DIALER_OPENCNAM_AUTH_TOKEN,
                CMSettings.System.DISPLAY_TEMPERATURE_DAY,
                CMSettings.System.DISPLAY_TEMPERATURE_NIGHT,
                CMSettings.System.DISPLAY_TEMPERATURE_MODE,
                CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE,
                CMSettings.System.DISPLAY_LOW_POWER,
                CMSettings.System.DISPLAY_COLOR_ENHANCE,
                CMSettings.System.DISPLAY_COLOR_ADJUSTMENT,
                CMSettings.System.LIVE_DISPLAY_HINTED,
                CMSettings.System.DOUBLE_TAP_SLEEP_GESTURE,
                CMSettings.System.STATUS_BAR_SHOW_WEATHER,
                CMSettings.System.RECENTS_SHOW_SEARCH_BAR,
                CMSettings.System.NAVBAR_LEFT_IN_LANDSCAPE,
                CMSettings.System.T9_SEARCH_INPUT_LOCALE,
                CMSettings.System.BLUETOOTH_ACCEPT_ALL_FILES,
                CMSettings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                CMSettings.System.SHOW_ALARM_ICON,
                CMSettings.System.STATUS_BAR_IME_SWITCHER,
                CMSettings.System.QS_SHOW_BRIGHTNESS_SLIDER,
                CMSettings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                CMSettings.System.VOLBTN_MUSIC_CONTROLS,
                CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                CMSettings.System.USE_EDGE_SERVICE_FOR_GESTURES,
                CMSettings.System.STATUS_BAR_NOTIF_COUNT,
                CMSettings.System.CALL_RECORDING_FORMAT,
                CMSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                CMSettings.System.NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                CMSettings.System.NOTIFICATION_LIGHT_SCREEN_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                CMSettings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
                CMSettings.System.SYSTEM_PROFILES_ENABLED,
                CMSettings.System.INCREASING_RING,
                CMSettings.System.INCREASING_RING_START_VOLUME,
                CMSettings.System.INCREASING_RING_RAMP_UP_TIME,
                CMSettings.System.STATUS_BAR_CLOCK,
                CMSettings.System.STATUS_BAR_AM_PM,
                CMSettings.System.STATUS_BAR_BATTERY_STYLE,
                CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                CMSettings.System.VOLUME_KEYS_CONTROL_RING_STREAM,
                CMSettings.System.NAVIGATION_BAR_MENU_ARROW_KEYS,
        };

        /**
         * @hide
         */
        public static boolean isLegacySetting(String key) {
            return ArrayUtils.contains(LEGACY_SYSTEM_SETTINGS, key);
        }

        /**
         * Mapping of validators for all system settings.  This map is used to validate both valid
         * keys as well as validating the values for those keys.
         *
         * Note: Make sure if you add a new System setting you create a Validator for it and add
         *       it to this map.
         *
         * @hide
         */
        public static final Map<String, Validator> VALIDATORS =
                new ArrayMap<String, Validator>();
        static {
            VALIDATORS.put(QS_QUICK_PULLDOWN, QS_QUICK_PULLDOWN_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_PLAY_QUEUE, NOTIFICATION_PLAY_QUEUE_VALIDATOR);
            VALIDATORS.put(HIGH_TOUCH_SENSITIVITY_ENABLE,
                    HIGH_TOUCH_SENSITIVITY_ENABLE_VALIDATOR);
            VALIDATORS.put(SYSTEM_PROFILES_ENABLED, SYSTEM_PROFILES_ENABLED_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_CLOCK, STATUS_BAR_CLOCK_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_AM_PM, STATUS_BAR_AM_PM_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_BATTERY_STYLE, STATUS_BAR_BATTERY_STYLE_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_SHOW_BATTERY_PERCENT,
                    STATUS_BAR_SHOW_BATTERY_PERCENT_VALIDATOR);
            VALIDATORS.put(INCREASING_RING, INCREASING_RING_VALIDATOR);
            VALIDATORS.put(INCREASING_RING_START_VOLUME,
                    INCREASING_RING_START_VOLUME_VALIDATOR);
            VALIDATORS.put(INCREASING_RING_RAMP_UP_TIME,
                    INCREASING_RING_RAMP_UP_TIME_VALIDATOR);
            VALIDATORS.put(VOLUME_ADJUST_SOUNDS_ENABLED,
                    VOLUME_ADJUST_SOUNDS_ENABLED_VALIDATOR);
            VALIDATORS.put(NAV_BUTTONS, NAV_BUTTONS_VALIDATOR);
            VALIDATORS.put(VOLUME_KEYS_CONTROL_RING_STREAM,
                    VOLUME_KEYS_CONTROL_RING_STREAM_VALIDATOR);
            VALIDATORS.put(NAVIGATION_BAR_MENU_ARROW_KEYS,
                    NAVIGATION_BAR_MENU_ARROW_KEYS_VALIDATOR);
            VALIDATORS.put(KEY_HOME_LONG_PRESS_ACTION, KEY_HOME_LONG_PRESS_ACTION_VALIDATOR);
            VALIDATORS.put(KEY_HOME_DOUBLE_TAP_ACTION, KEY_HOME_DOUBLE_TAP_ACTION_VALIDATOR);
            VALIDATORS.put(BACK_WAKE_SCREEN, BACK_WAKE_SCREEN_VALIDATOR);
            VALIDATORS.put(MENU_WAKE_SCREEN, MENU_WAKE_SCREENN_VALIDATOR);
            VALIDATORS.put(VOLUME_WAKE_SCREEN, VOLUME_WAKE_SCREEN_VALIDATOR);
            VALIDATORS.put(KEY_MENU_ACTION, KEY_MENU_ACTION_VALIDATOR);
            VALIDATORS.put(KEY_MENU_LONG_PRESS_ACTION, KEY_MENU_LONG_PRESS_ACTION_VALIDATOR);
            VALIDATORS.put(KEY_ASSIST_ACTION, KEY_ASSIST_ACTION_VALIDATOR);
            VALIDATORS.put(KEY_ASSIST_LONG_PRESS_ACTION,
                    KEY_ASSIST_LONG_PRESS_ACTION_VALIDATOR);
            VALIDATORS.put(KEY_APP_SWITCH_ACTION, KEY_APP_SWITCH_ACTION_VALIDATOR);
            VALIDATORS.put(KEY_APP_SWITCH_LONG_PRESS_ACTION,
                    KEY_APP_SWITCH_LONG_PRESS_ACTION_VALIDATOR);
            VALIDATORS.put(HOME_WAKE_SCREEN, HOME_WAKE_SCREEN_VALIDATOR);
            VALIDATORS.put(ASSIST_WAKE_SCREEN, ASSIST_WAKE_SCREEN_VALIDATOR);
            VALIDATORS.put(APP_SWITCH_WAKE_SCREEN, APP_SWITCH_WAKE_SCREEN_VALIDATOR);
            VALIDATORS.put(CAMERA_WAKE_SCREEN, CAMERA_WAKE_SCREEN_VALIDATOR);
            VALIDATORS.put(CAMERA_SLEEP_ON_RELEASE, CAMERA_SLEEP_ON_RELEASE_VALIDATOR);
            VALIDATORS.put(CAMERA_LAUNCH, CAMERA_LAUNCH_VALIDATOR);
            VALIDATORS.put(SWAP_VOLUME_KEYS_ON_ROTATION,
                    SWAP_VOLUME_KEYS_ON_ROTATION_VALIDATOR);
            VALIDATORS.put(BATTERY_LIGHT_ENABLED, BATTERY_LIGHT_ENABLED_VALIDATOR);
            VALIDATORS.put(BATTERY_LIGHT_PULSE, BATTERY_LIGHT_PULSE_VALIDATOR);
            VALIDATORS.put(BATTERY_LIGHT_LOW_COLOR, BATTERY_LIGHT_LOW_COLOR_VALIDATOR);
            VALIDATORS.put(BATTERY_LIGHT_MEDIUM_COLOR, BATTERY_LIGHT_MEDIUM_COLOR_VALIDATOR);
            VALIDATORS.put(BATTERY_LIGHT_FULL_COLOR, BATTERY_LIGHT_FULL_COLOR_VALIDATOR);
            VALIDATORS.put(ENABLE_MWI_NOTIFICATION, ENABLE_MWI_NOTIFICATION_VALIDATOR);
            VALIDATORS.put(PROXIMITY_ON_WAKE, PROXIMITY_ON_WAKE_VALIDATOR);
            VALIDATORS.put(ENABLE_FORWARD_LOOKUP, ENABLE_FORWARD_LOOKUP_VALIDATOR);
            VALIDATORS.put(ENABLE_PEOPLE_LOOKUP, ENABLE_PEOPLE_LOOKUP_VALIDATOR);
            VALIDATORS.put(ENABLE_REVERSE_LOOKUP, ENABLE_REVERSE_LOOKUP_VALIDATOR);
            VALIDATORS.put(FORWARD_LOOKUP_PROVIDER, FORWARD_LOOKUP_PROVIDER_VALIDATOR);
            VALIDATORS.put(PEOPLE_LOOKUP_PROVIDER, PEOPLE_LOOKUP_PROVIDER_VALIDATOR);
            VALIDATORS.put(REVERSE_LOOKUP_PROVIDER, REVERSE_LOOKUP_PROVIDER_VALIDATOR);
            VALIDATORS.put(DIALER_OPENCNAM_ACCOUNT_SID,
                    DIALER_OPENCNAM_ACCOUNT_SID_VALIDATOR);
            VALIDATORS.put(DIALER_OPENCNAM_AUTH_TOKEN, DIALER_OPENCNAM_AUTH_TOKEN_VALIDATOR);
            VALIDATORS.put(DISPLAY_TEMPERATURE_DAY, DISPLAY_TEMPERATURE_DAY_VALIDATOR);
            VALIDATORS.put(DISPLAY_TEMPERATURE_NIGHT, DISPLAY_TEMPERATURE_NIGHT_VALIDATOR);
            VALIDATORS.put(DISPLAY_TEMPERATURE_MODE, DISPLAY_TEMPERATURE_MODE_VALIDATOR);
            VALIDATORS.put(DISPLAY_AUTO_OUTDOOR_MODE, DISPLAY_AUTO_OUTDOOR_MODE_VALIDATOR);
            VALIDATORS.put(DISPLAY_LOW_POWER, DISPLAY_LOW_POWER_VALIDATOR);
            VALIDATORS.put(DISPLAY_COLOR_ENHANCE, DISPLAY_COLOR_ENHANCE_VALIDATOR);
            VALIDATORS.put(DISPLAY_COLOR_ADJUSTMENT, DISPLAY_COLOR_ADJUSTMENT_VALIDATOR);
            VALIDATORS.put(LIVE_DISPLAY_HINTED, LIVE_DISPLAY_HINTED_VALIDATOR);
            VALIDATORS.put(DOUBLE_TAP_SLEEP_GESTURE, DOUBLE_TAP_SLEEP_GESTURE_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_SHOW_WEATHER, STATUS_BAR_SHOW_WEATHER_VALIDATOR);
            VALIDATORS.put(RECENTS_SHOW_SEARCH_BAR, RECENTS_SHOW_SEARCH_BAR_VALIDATOR);
            VALIDATORS.put(NAVBAR_LEFT_IN_LANDSCAPE, NAVBAR_LEFT_IN_LANDSCAPE_VALIDATOR);
            VALIDATORS.put(T9_SEARCH_INPUT_LOCALE, T9_SEARCH_INPUT_LOCALE_VALIDATOR);
            VALIDATORS.put(BLUETOOTH_ACCEPT_ALL_FILES, BLUETOOTH_ACCEPT_ALL_FILES_VALIDATOR);
            VALIDATORS.put(LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                    LOCKSCREEN_PIN_SCRAMBLE_LAYOUT_VALIDATOR);
            VALIDATORS.put(SHOW_ALARM_ICON, SHOW_ALARM_ICON_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_IME_SWITCHER, STATUS_BAR_IME_SWITCHER_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_QUICK_QS_PULLDOWN,
                    STATUS_BAR_QUICK_QS_PULLDOWN_VALIDATOR);
            VALIDATORS.put(QS_SHOW_BRIGHTNESS_SLIDER, QS_SHOW_BRIGHTNESS_SLIDER_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_BRIGHTNESS_CONTROL,
                    STATUS_BAR_BRIGHTNESS_CONTROL_VALIDATOR);
            VALIDATORS.put(VOLBTN_MUSIC_CONTROLS, VOLBTN_MUSIC_CONTROLS_VALIDATOR);
            VALIDATORS.put(USE_EDGE_SERVICE_FOR_GESTURES,
                    USE_EDGE_SERVICE_FOR_GESTURES_VALIDATOR);
            VALIDATORS.put(STATUS_BAR_NOTIF_COUNT, STATUS_BAR_NOTIF_COUNT_VALIDATOR);
            VALIDATORS.put(CALL_RECORDING_FORMAT, CALL_RECORDING_FORMAT_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                    NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                    NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_SCREEN_ON,
                    NOTIFICATION_LIGHT_SCREEN_ON_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR,
                    NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON,
                    NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF,
                    NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_CALL_COLOR,
                    NOTIFICATION_LIGHT_PULSE_CALL_COLOR_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_CALL_LED_ON,
                    NOTIFICATION_LIGHT_PULSE_CALL_LED_ON_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF,
                    NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR,
                    NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON,
                    NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF,
                    NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                    NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE_VALIDATOR);
            VALIDATORS.put(NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                    NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES_VALIDATOR);
        };
        // endregion
    }

    /**
     * Secure settings, containing miscellaneous CM secure preferences. This
     * table holds simple name/value pairs. There are convenience
     * functions for accessing individual settings entries.
     */
    public static final class Secure extends Settings.NameValueTable {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/secure");

        public static final String SYS_PROP_CM_SETTING_VERSION = "sys.cm_settings_secure_version";

        private static final NameValueCache sNameValueCache = new NameValueCache(
                SYS_PROP_CM_SETTING_VERSION,
                CONTENT_URI,
                CALL_METHOD_GET_SECURE,
                CALL_METHOD_PUT_SECURE);

        // region Methods


        /**
         * Put a delimited list as a string
         * @param resolver to access the database with
         * @param name to store
         * @param delimiter to split
         * @param list to join and store
         * @hide
         */
        public static void putListAsDelimitedString(ContentResolver resolver, String name,
                                                    String delimiter, List<String> list) {
            String store = TextUtils.join(delimiter, list);
            putString(resolver, name, store);
        }

        /**
         * Get a delimited string returned as a list
         * @param resolver to access the database with
         * @param name to store
         * @param delimiter to split the list with
         * @return list of strings for a specific Settings.Secure item
         * @hide
         */
        public static List<String> getDelimitedStringAsList(ContentResolver resolver, String name,
                                                            String delimiter) {
            String baseString = getString(resolver, name);
            List<String> list = new ArrayList<String>();
            if (!TextUtils.isEmpty(baseString)) {
                final String[] array = TextUtils.split(baseString, Pattern.quote(delimiter));
                for (String item : array) {
                    if (TextUtils.isEmpty(item)) {
                        continue;
                    }
                    list.add(item);
                }
            }
            return list;
        }

        /**
         * Construct the content URI for a particular name/value pair, useful for monitoring changes
         * with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI
         */
        public static Uri getUriFor(String name) {
            return Settings.NameValueTable.getUriFor(CONTENT_URI, name);
        }

        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public static String getString(ContentResolver resolver, String name) {
            return getStringForUser(resolver, name, UserHandle.myUserId());
        }

        /** @hide */
        public static String getStringForUser(ContentResolver resolver, String name,
                int userId) {
            return sNameValueCache.getStringForUser(resolver, name, userId);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver, String name, String value) {
            return putStringForUser(resolver, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putStringForUser(ContentResolver resolver, String name, String value,
               int userId) {
            return sNameValueCache.putStringForUser(resolver, name, value, userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.  The default value will be returned if the setting is
         * not defined or not an integer.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return getIntForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int def, int userId) {
            String v = getStringForUser(cr, name, userId);
            try {
                return v != null ? Integer.parseInt(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for retrieving a single settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         *
         * @return The setting's current value.
         */
        public static int getInt(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getIntForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String v = getStringForUser(cr, name, userId);
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as an
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver cr, String name, int value) {
            return putIntForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putIntForUser(ContentResolver cr, String name, int value,
                int userId) {
            return putStringForUser(cr, name, Integer.toString(value), userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.  The default value will be returned if the setting is
         * not defined or not a {@code long}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            return getLongForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, long def,
                int userId) {
            String valString = getStringForUser(cr, name, userId);
            long value;
            try {
                value = valString != null ? Long.parseLong(valString) : def;
            } catch (NumberFormatException e) {
                value = def;
            }
            return value;
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @return The setting's current value.
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         */
        public static long getLong(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getLongForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String valString = getStringForUser(cr, name, userId);
            try {
                return Long.parseLong(valString);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as a long
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putLong(ContentResolver cr, String name, long value) {
            return putLongForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putLongForUser(ContentResolver cr, String name, long value,
                int userId) {
            return putStringForUser(cr, name, Long.toString(value), userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a floating point number.  Note that internally setting values are
         * always stored as strings; this function converts the string to an
         * float for you. The default value will be returned if the setting
         * is not defined or not a valid float.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid float.
         */
        public static float getFloat(ContentResolver cr, String name, float def) {
            return getFloatForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, float def,
                int userId) {
            String v = getStringForUser(cr, name, userId);
            try {
                return v != null ? Float.parseFloat(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a float.  Note that internally setting values are always
         * stored as strings; this function converts the string to a float
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not a float.
         *
         * @return The setting's current value.
         */
        public static float getFloat(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getFloatForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String v = getStringForUser(cr, name, userId);
            if (v == null) {
                throw new CMSettingNotFoundException(name);
            }
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as a
         * floating point number. This will either create a new entry in the
         * table if the given name does not exist, or modify the value of the
         * existing row with that name.  Note that internally setting values
         * are always stored as strings, so this function converts the given
         * value to a string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putFloat(ContentResolver cr, String name, float value) {
            return putFloatForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putFloatForUser(ContentResolver cr, String name, float value,
                int userId) {
            return putStringForUser(cr, name, Float.toString(value), userId);
        }

        // endregion

        // region Secure Settings

        /**
         * Whether to enable "advanced mode" for the current user.
         * Boolean setting. 0 = no, 1 = yes.
         * @hide
         */
        public static final String ADVANCED_MODE = "advanced_mode";

        /**
         * The time in ms to keep the button backlight on after pressing a button.
         * A value of 0 will keep the buttons on for as long as the screen is on.
         * @hide
         */
        public static final String BUTTON_BACKLIGHT_TIMEOUT = "button_backlight_timeout";

        /**
         * The button brightness to be used while the screen is on or after a button press,
         * depending on the value of {@link BUTTON_BACKLIGHT_TIMEOUT}.
         * Valid value range is between 0 and {@link PowerManager#getMaximumButtonBrightness()}
         * @hide
         */
        public static final String BUTTON_BRIGHTNESS = "button_brightness";

        /**
         * A '|' delimited list of theme components to apply from the default theme on first boot.
         * Components can be one or more of the "mods_XXXXXXX" found in
         * {@link ThemesContract$ThemesColumns}.  Leaving this field blank assumes all components
         * will be applied.
         *
         * ex: mods_icons|mods_overlays|mods_homescreen
         *
         * @hide
         */
        public static final String DEFAULT_THEME_COMPONENTS = "default_theme_components";

        /**
         * Default theme to use.  If empty, use holo.
         * @hide
         */
        public static final String DEFAULT_THEME_PACKAGE = "default_theme_package";

        /**
         * Developer options - Navigation Bar show switch
         * @hide
         */
        public static final String DEV_FORCE_SHOW_NAVBAR = "dev_force_show_navbar";

        /**
         * The keyboard brightness to be used while the screen is on.
         * Valid value range is between 0 and {@link PowerManager#getMaximumKeyboardBrightness()}
         * @hide
         */
        public static final String KEYBOARD_BRIGHTNESS = "keyboard_brightness";

        /**
         * Custom navring actions
         * @hide
         */
        public static final String[] NAVIGATION_RING_TARGETS = new String[] {
                "navigation_ring_targets_0",
                "navigation_ring_targets_1",
                "navigation_ring_targets_2",
        };

        /**
         * String to contain power menu actions
         * @hide
         */
        public static final String POWER_MENU_ACTIONS = "power_menu_actions";

        /**
         * Whether to show the brightness slider in quick settings panel.
         * @hide
         */
        public static final String QS_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";

        /**
         * List of QS tile names
         * @hide
         */
        public static final String QS_TILES = "sysui_qs_tiles";

        /**
         * Use "main" tiles on the first row of the quick settings panel
         * 0 = no, 1 = yes
         * @hide
         */
        public static final String QS_USE_MAIN_TILES = "sysui_qs_main_tiles";

        /**
         * Global stats collection
         * @hide
         */
        public static final String STATS_COLLECTION = "stats_collection";

        /**
         * Whether the global stats collection setting has been successfully reported to server
         * @hide
         */
        public static final String STATS_COLLECTION_REPORTED = "stats_collection_reported";

        /**
         * Boolean value whether to link ringtone and notification volume
         *
         * @hide
         */
        public static final String VOLUME_LINK_NOTIFICATION = "volume_link_notification";

        /**
         * Whether newly installed apps should run with privacy guard by default
         * @hide
         */
        public static final String PRIVACY_GUARD_DEFAULT = "privacy_guard_default";

        /**
         * The global recents long press activity chosen by the user.
         * This setting is stored as a flattened component name as
         * per {@link ComponentName#flattenToString()}.
         *
         * @hide
         */
        public static final String RECENTS_LONG_PRESS_ACTIVITY = "recents_long_press_activity";

        /**
         * What happens when the user presses the Home button when the
         * phone is ringing.<br/>
         * <b>Values:</b><br/>
         * 1 - Nothing happens. (Default behavior)<br/>
         * 2 - The Home button answer the current call.<br/>
         *
         * @hide
         */
        public static final String RING_HOME_BUTTON_BEHAVIOR = "ring_home_button_behavior";

        /**
         * RING_HOME_BUTTON_BEHAVIOR value for "do nothing".
         * @hide
         */
        public static final int RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING = 0x1;

        /**
         * RING_HOME_BUTTON_BEHAVIOR value for "answer".
         * @hide
         */
        public static final int RING_HOME_BUTTON_BEHAVIOR_ANSWER = 0x2;

        /**
         * RING_HOME_BUTTON_BEHAVIOR default value.
         * @hide
         */
        public static final int RING_HOME_BUTTON_BEHAVIOR_DEFAULT =
                RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING;

        /**
         * Performance profile
         * @hide
         */
        public static final String PERFORMANCE_PROFILE = "performance_profile";

        /**
         * App-based performance profile selection
         * @hide
         */
        public static final String APP_PERFORMANCE_PROFILES_ENABLED = "app_perf_profiles_enabled";

        /**
         * Launch actions for left/right lockscreen targets
         * @hide
         */
        public static final String LOCKSCREEN_TARGETS = "lockscreen_target_actions";

        /**
         * Whether to display a menu containing 'Wipe data', 'Force close' and other options
         * in the notification area and in the recent app list
         * @hide
         */
        public static final String DEVELOPMENT_SHORTCUT = "development_shortcut";

        /**
         * Whether to display the ADB notification.
         * @hide
         */
        public static final String ADB_NOTIFY = "adb_notify";

        /**
         * The TCP/IP port to run ADB on, or -1 for USB
         * @hide
         */
        public static final String ADB_PORT = "adb_port";

        /**
         * The hostname for this device
         * @hide
         */
        public static final String DEVICE_HOSTNAME = "device_hostname";

        /**
         * Whether to allow killing of the foreground app by long-pressing the Back button
         * @hide
         */
        public static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";

        /** Protected Components
         * @hide
         */
        public static final String PROTECTED_COMPONENTS = "protected_components";

        /**
         * Stored color matrix for LiveDisplay. This is used to allow co-existence with
         * display tuning done by DisplayAdjustmentUtils when hardware support isn't
         * available.
         * @hide
         */
        public static final String LIVE_DISPLAY_COLOR_MATRIX = "live_display_color_matrix";

        /**
         * Whether to include options in power menu for rebooting into recovery or bootloader
         * @hide
         */
        public static final String ADVANCED_REBOOT = "advanced_reboot";

        /**
         * This will be set to the system's current theme API version when ThemeService starts.
         * It is useful for when an upgrade from one version of CM to another occurs.
         * For example, after a user upgrades from CM11 to CM12, the value of this field
         * might be 19. ThemeService would then change the value to 21. This is useful
         * when an API change breaks a theme. Themeservice can identify old themes and
         * unapply them from the system.
         * @hide
         */
        public static final String THEME_PREV_BOOT_API_LEVEL = "theme_prev_boot_api_level";
        // endregion

        /**
         * @hide
         */
        public static final String[] LEGACY_SECURE_SETTINGS = new String[]{
                CMSettings.Secure.ADVANCED_MODE,
                CMSettings.Secure.BUTTON_BACKLIGHT_TIMEOUT,
                CMSettings.Secure.BUTTON_BRIGHTNESS,
                CMSettings.Secure.DEFAULT_THEME_COMPONENTS,
                CMSettings.Secure.DEFAULT_THEME_PACKAGE,
                CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR,
                CMSettings.Secure.KEYBOARD_BRIGHTNESS,
                CMSettings.Secure.POWER_MENU_ACTIONS,
                CMSettings.Secure.STATS_COLLECTION,
                CMSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                CMSettings.Secure.QS_TILES,
                CMSettings.Secure.QS_USE_MAIN_TILES,
                CMSettings.Secure.VOLUME_LINK_NOTIFICATION,
                CMSettings.Secure.NAVIGATION_RING_TARGETS[0],
                CMSettings.Secure.NAVIGATION_RING_TARGETS[1],
                CMSettings.Secure.NAVIGATION_RING_TARGETS[2],
                CMSettings.Secure.RECENTS_LONG_PRESS_ACTIVITY,
                CMSettings.Secure.ADB_NOTIFY,
                CMSettings.Secure.ADB_PORT,
                CMSettings.Secure.DEVICE_HOSTNAME,
                CMSettings.Secure.KILL_APP_LONGPRESS_BACK,
                CMSettings.Secure.PROTECTED_COMPONENTS,
                CMSettings.Secure.LIVE_DISPLAY_COLOR_MATRIX,
                CMSettings.Secure.ADVANCED_REBOOT,
                CMSettings.Secure.THEME_PREV_BOOT_API_LEVEL,
                CMSettings.Secure.LOCKSCREEN_TARGETS,
                CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                CMSettings.Secure.PRIVACY_GUARD_DEFAULT,
                CMSettings.Secure.DEVELOPMENT_SHORTCUT,
                CMSettings.Secure.PERFORMANCE_PROFILE,
                CMSettings.Secure.APP_PERFORMANCE_PROFILES_ENABLED};

        /**
         * @hide
         */
        public static boolean isLegacySetting(String key) {
            return ArrayUtils.contains(LEGACY_SECURE_SETTINGS, key);
        }
    }

    /**
     * Global settings, containing miscellaneous CM global preferences. This
     * table holds simple name/value pairs. There are convenience
     * functions for accessing individual settings entries.
     */
    public static final class Global extends Settings.NameValueTable {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/global");

        public static final String SYS_PROP_CM_SETTING_VERSION = "sys.cm_settings_global_version";

        private static final NameValueCache sNameValueCache = new NameValueCache(
                SYS_PROP_CM_SETTING_VERSION,
                CONTENT_URI,
                CALL_METHOD_GET_GLOBAL,
                CALL_METHOD_PUT_GLOBAL);

        // region Methods


        /**
         * Put a delimited list as a string
         * @param resolver to access the database with
         * @param name to store
         * @param delimiter to split
         * @param list to join and store
         * @hide
         */
        public static void putListAsDelimitedString(ContentResolver resolver, String name,
                                                    String delimiter, List<String> list) {
            String store = TextUtils.join(delimiter, list);
            putString(resolver, name, store);
        }

        /**
         * Get a delimited string returned as a list
         * @param resolver to access the database with
         * @param name to store
         * @param delimiter to split the list with
         * @return list of strings for a specific Settings.Secure item
         * @hide
         */
        public static List<String> getDelimitedStringAsList(ContentResolver resolver, String name,
                                                            String delimiter) {
            String baseString = getString(resolver, name);
            List<String> list = new ArrayList<String>();
            if (!TextUtils.isEmpty(baseString)) {
                final String[] array = TextUtils.split(baseString, Pattern.quote(delimiter));
                for (String item : array) {
                    if (TextUtils.isEmpty(item)) {
                        continue;
                    }
                    list.add(item);
                }
            }
            return list;
        }

        /**
         * Construct the content URI for a particular name/value pair, useful for monitoring changes
         * with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI
         */
        public static Uri getUriFor(String name) {
            return Settings.NameValueTable.getUriFor(CONTENT_URI, name);
        }

        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public static String getString(ContentResolver resolver, String name) {
            return getStringForUser(resolver, name, UserHandle.myUserId());
        }

        /** @hide */
        public static String getStringForUser(ContentResolver resolver, String name,
                int userId) {
            return sNameValueCache.getStringForUser(resolver, name, userId);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver, String name, String value) {
            return putStringForUser(resolver, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putStringForUser(ContentResolver resolver, String name, String value,
                int userId) {
            return sNameValueCache.putStringForUser(resolver, name, value, userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.  The default value will be returned if the setting is
         * not defined or not an integer.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return getIntForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int def, int userId) {
            String v = getStringForUser(cr, name, userId);
            try {
                return v != null ? Integer.parseInt(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for retrieving a single settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         *
         * @return The setting's current value.
         */
        public static int getInt(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getIntForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String v = getStringForUser(cr, name, userId);
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as an
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver cr, String name, int value) {
            return putIntForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putIntForUser(ContentResolver cr, String name, int value,
                int userId) {
            return putStringForUser(cr, name, Integer.toString(value), userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.  The default value will be returned if the setting is
         * not defined or not a {@code long}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            return getLongForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, long def,
                int userId) {
            String valString = getStringForUser(cr, name, userId);
            long value;
            try {
                value = valString != null ? Long.parseLong(valString) : def;
            } catch (NumberFormatException e) {
                value = def;
            }
            return value;
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @return The setting's current value.
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         */
        public static long getLong(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getLongForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String valString = getStringForUser(cr, name, userId);
            try {
                return Long.parseLong(valString);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as a long
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putLong(ContentResolver cr, String name, long value) {
            return putLongForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putLongForUser(ContentResolver cr, String name, long value,
                int userId) {
            return putStringForUser(cr, name, Long.toString(value), userId);
        }

        /**
         * Convenience function for retrieving a single settings value
         * as a floating point number.  Note that internally setting values are
         * always stored as strings; this function converts the string to an
         * float for you. The default value will be returned if the setting
         * is not defined or not a valid float.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid float.
         */
        public static float getFloat(ContentResolver cr, String name, float def) {
            return getFloatForUser(cr, name, def, UserHandle.myUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, float def,
                int userId) {
            String v = getStringForUser(cr, name, userId);
            try {
                return v != null ? Float.parseFloat(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a float.  Note that internally setting values are always
         * stored as strings; this function converts the string to a float
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link CMSettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws CMSettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not a float.
         *
         * @return The setting's current value.
         */
        public static float getFloat(ContentResolver cr, String name)
                throws CMSettingNotFoundException {
            return getFloatForUser(cr, name, UserHandle.myUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, int userId)
                throws CMSettingNotFoundException {
            String v = getStringForUser(cr, name, userId);
            if (v == null) {
                throw new CMSettingNotFoundException(name);
            }
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException e) {
                throw new CMSettingNotFoundException(name);
            }
        }

        /**
         * Convenience function for updating a single settings value as a
         * floating point number. This will either create a new entry in the
         * table if the given name does not exist, or modify the value of the
         * existing row with that name.  Note that internally setting values
         * are always stored as strings, so this function converts the given
         * value to a string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putFloat(ContentResolver cr, String name, float value) {
            return putFloatForUser(cr, name, value, UserHandle.myUserId());
        }

        /** @hide */
        public static boolean putFloatForUser(ContentResolver cr, String name, float value,
                int userId) {
            return putStringForUser(cr, name, Float.toString(value), userId);
        }

        // endregion

        // region Global Settings
        /**
         * Whether to wake the display when plugging or unplugging the charger
         *
         * @hide
         */
        public static final String WAKE_WHEN_PLUGGED_OR_UNPLUGGED =
                "wake_when_plugged_or_unplugged";

        /**
         * Whether to sound when charger power is connected/disconnected
         * @hide
         */
        public static final String POWER_NOTIFICATIONS_ENABLED = "power_notifications_enabled";

        /**
         * Whether to vibrate when charger power is connected/disconnected
         * @hide
         */
        public static final String POWER_NOTIFICATIONS_VIBRATE = "power_notifications_vibrate";

        /**
         * URI for power notification sounds
         * @hide
         */
        public static final String POWER_NOTIFICATIONS_RINGTONE = "power_notifications_ringtone";

        /**
         * @hide
         */
        public static final String ZEN_DISABLE_DUCKING_DURING_MEDIA_PLAYBACK =
                "zen_disable_ducking_during_media_playback";

        /**
         * Whether the system auto-configure the priority of the wifi ap's or use
         * the manual settings established by the user.
         * <> 0 to autoconfigure, 0 to manual settings. Default is <> 0.
         * @hide
         */
        public static final String WIFI_AUTO_PRIORITIES_CONFIGURATION = "wifi_auto_priority";
        // endregion

        /**
         * @hide
         */
        public static final String[] LEGACY_GLOBAL_SETTINGS = new String[]{
                CMSettings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                CMSettings.Global.POWER_NOTIFICATIONS_ENABLED,
                CMSettings.Global.POWER_NOTIFICATIONS_VIBRATE,
                CMSettings.Global.POWER_NOTIFICATIONS_RINGTONE,
                CMSettings.Global.ZEN_DISABLE_DUCKING_DURING_MEDIA_PLAYBACK,
                CMSettings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION};

        /**
         * @hide
         */
        public static boolean isLegacySetting(String key) {
            return ArrayUtils.contains(LEGACY_GLOBAL_SETTINGS, key);
        }
    }
}
