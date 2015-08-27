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

package org.cyanogenmod.cmsettings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import cyanogenmod.providers.CMSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The CMSettingsProvider serves as a {@link ContentProvider} for CM specific settings
 */
public class CMSettingsProvider extends ContentProvider {
    private static final String TAG = "CMSettingsProvider";
    private static final boolean LOCAL_LOGV = false;

    private static final boolean USER_CHECK_THROWS = true;

    private static final String PREF_HAS_MIGRATED_CM_SETTINGS = "has_migrated_cm_settings";

    private static final Bundle NULL_SETTING = Bundle.forPair("value", null);

    // Each defined user has their own settings
    protected final SparseArray<CMDatabaseHelper> mDbHelpers = new SparseArray<CMDatabaseHelper>();

    private static final int SYSTEM = 1;
    private static final int SECURE = 2;
    private static final int GLOBAL = 3;

    private static final int SYSTEM_ITEM_NAME = 4;
    private static final int SECURE_ITEM_NAME = 5;
    private static final int GLOBAL_ITEM_NAME = 6;

    private static final String ITEM_MATCHER = "/*";
    private static final String NAME_SELECTION = Settings.NameValueTable.NAME + " = ?";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_SYSTEM,
                SYSTEM);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_SECURE,
                SECURE);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_GLOBAL,
                GLOBAL);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_SYSTEM +
                ITEM_MATCHER, SYSTEM_ITEM_NAME);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_SECURE +
                ITEM_MATCHER, SECURE_ITEM_NAME);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_GLOBAL +
                ITEM_MATCHER, GLOBAL_ITEM_NAME);
    }

    private UserManager mUserManager;
    private Uri.Builder mUriBuilder;
    private SharedPreferences mSharedPrefs;

    @Override
    public boolean onCreate() {
        if (LOCAL_LOGV) Log.d(TAG, "Creating CMSettingsProvider");

        mUserManager = UserManager.get(getContext());

        establishDbTracking(UserHandle.USER_OWNER);

        mUriBuilder = new Uri.Builder();
        mUriBuilder.scheme(ContentResolver.SCHEME_CONTENT);
        mUriBuilder.authority(CMSettings.AUTHORITY);

        mSharedPrefs = getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE);

        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction(Intent.ACTION_USER_REMOVED);
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE,
                        UserHandle.USER_OWNER);
                String action = intent.getAction();

                if (LOCAL_LOGV) Log.d(TAG, "Received intent: " + action + " for user: " + userId);

                if (action.equals(Intent.ACTION_USER_REMOVED)) {
                    onUserRemoved(userId);
                }
            }
        }, userFilter);

        return true;
    }

    // region Migration Methods

    /**
     * Migrates CM settings for all existing users if this has not been run before.
     */
    private void migrateCMSettingsForExistingUsersIfNeeded() {
        boolean hasMigratedCMSettings = mSharedPrefs.getBoolean(PREF_HAS_MIGRATED_CM_SETTINGS,
                false);

        if (!hasMigratedCMSettings) {
            long startTime = System.currentTimeMillis();

            for (UserInfo user : mUserManager.getUsers()) {
                migrateCMSettingsForUser(user.id);
            }

            mSharedPrefs.edit().putBoolean(PREF_HAS_MIGRATED_CM_SETTINGS, true).commit();

            // TODO: Add this as part of a boot message to the UI
            long timeDiffMillis = System.currentTimeMillis() - startTime;
            if (LOCAL_LOGV) Log.d(TAG, "Migration finished in " + timeDiffMillis + " milliseconds");
        }
    }

    /**
     * Migrates CM settings for a specific user.
     * @param userId The id of the user to run CM settings migration for.
     */
    private void migrateCMSettingsForUser(int userId) {
        synchronized (this) {
            if (LOCAL_LOGV) Log.d(TAG, "CM settings will be migrated for user id: " + userId);

            // Migrate system settings
            HashMap<String, String> systemToCmSettingsMap = new HashMap<String, String>();
            systemToCmSettingsMap.put(Settings.System.QS_QUICK_PULLDOWN,
                    CMSettings.System.QS_QUICK_PULLDOWN);

            int rowsMigrated = migrateCMSettingsForTable(userId,
                    CMDatabaseHelper.CMTableNames.TABLE_SYSTEM, systemToCmSettingsMap);
            if (LOCAL_LOGV) Log.d(TAG, "Migrated " + rowsMigrated + " to CM system table");

            // Migrate secure settings
            HashMap<String, String> secureToCmSettingsMap = new HashMap<String, String>();
            secureToCmSettingsMap.put(Settings.Secure.ADVANCED_MODE,
                    CMSettings.Secure.ADVANCED_MODE);
            secureToCmSettingsMap.put(Settings.Secure.BUTTON_BACKLIGHT_TIMEOUT,
                    CMSettings.Secure.BUTTON_BACKLIGHT_TIMEOUT);
            secureToCmSettingsMap.put(Settings.Secure.BUTTON_BRIGHTNESS,
                    CMSettings.Secure.BUTTON_BRIGHTNESS);
            secureToCmSettingsMap.put(Settings.Secure.DEFAULT_THEME_COMPONENTS,
                    CMSettings.Secure.DEFAULT_THEME_COMPONENTS);
            secureToCmSettingsMap.put(Settings.Secure.DEFAULT_THEME_PACKAGE,
                    CMSettings.Secure.DEFAULT_THEME_PACKAGE);
            secureToCmSettingsMap.put(Settings.Secure.DEV_FORCE_SHOW_NAVBAR,
                    CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR);
            secureToCmSettingsMap.put(
                    Configuration.THEME_PKG_CONFIGURATION_PERSISTENCE_PROPERTY,
                            CMSettings.Secure.NAME_THEME_CONFIG);
            secureToCmSettingsMap.put(Settings.Secure.KEYBOARD_BRIGHTNESS,
                    CMSettings.Secure.KEYBOARD_BRIGHTNESS);
            secureToCmSettingsMap.put(Settings.Secure.POWER_MENU_ACTIONS,
                    CMSettings.Secure.POWER_MENU_ACTIONS);
            secureToCmSettingsMap.put(Settings.Secure.STATS_COLLECTION,
                    CMSettings.Secure.STATS_COLLECTION);
            secureToCmSettingsMap.put(Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                    CMSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER);
            secureToCmSettingsMap.put(Settings.Secure.QS_TILES,
                    CMSettings.Secure.QS_TILES);
            secureToCmSettingsMap.put(Settings.Secure.QS_USE_MAIN_TILES,
                    CMSettings.Secure.QS_USE_MAIN_TILES);
            secureToCmSettingsMap.put(Settings.Secure.VOLUME_LINK_NOTIFICATION,
                    CMSettings.Secure.VOLUME_LINK_NOTIFICATION);

            int navRingTargetsLength = Settings.Secure.NAVIGATION_RING_TARGETS.length;
            int cmNavRingTargetsLength = CMSettings.Secure.NAVIGATION_RING_TARGETS.length;
            int minNavRingTargetsLength = navRingTargetsLength <= cmNavRingTargetsLength ?
                    navRingTargetsLength : cmNavRingTargetsLength;

            for (int i = 0; i < minNavRingTargetsLength; i++) {
                systemToCmSettingsMap.put(Settings.Secure.NAVIGATION_RING_TARGETS[i],
                        CMSettings.Secure.NAVIGATION_RING_TARGETS[i]);
            }

            rowsMigrated = migrateCMSettingsForTable(userId,
                    CMDatabaseHelper.CMTableNames.TABLE_SECURE, secureToCmSettingsMap);
            if (LOCAL_LOGV) Log.d(TAG, "Migrated " + rowsMigrated + " to CM secure table");

            // Migrate global settings
            if (userId == UserHandle.USER_OWNER) {
                HashMap<String, String> globalToCmSettingsMap = new HashMap<String, String>();
                globalToCmSettingsMap.put(Settings.Global.DEVICE_NAME,
                        CMSettings.Global.DEVICE_NAME);
                globalToCmSettingsMap.put(Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                        CMSettings.Global.HEADS_UP_NOTIFICATIONS_ENABLED);

                rowsMigrated = migrateCMSettingsForTable(userId,
                        CMDatabaseHelper.CMTableNames.TABLE_GLOBAL, globalToCmSettingsMap);
                if (LOCAL_LOGV) Log.d(TAG, "Migrated " + rowsMigrated + " to CM global table");
            }
        }
    }

    /**
     * Migrates CM settings for a specific table and user id.
     * @param userId The id of the user to run CM settings migration for.
     * @param tableName The name of the table to run CM settings migration on.
     * @param settingsMap A mapping between key names in {@link Settings} and {@link CMSettings}
     * @return Number of rows migrated.
     */
    private int migrateCMSettingsForTable(int userId, String tableName, HashMap<String,
            String> settingsMap) {
        ContentResolver contentResolver = getContext().getContentResolver();
        Set<Map.Entry<String, String>> entrySet = settingsMap.entrySet();
        ContentValues[] contentValues = new ContentValues[settingsMap.size()];

        int migrateSettingsCount = 0;
        for (Map.Entry<String, String> keyPair : entrySet) {
            String settingsKey = keyPair.getKey();
            String cmSettingsKey = keyPair.getValue();
            String settingsValue = null;

            if (tableName.equals(CMDatabaseHelper.CMTableNames.TABLE_SYSTEM)) {
                settingsValue = Settings.System.getStringForUser(contentResolver, settingsKey,
                        userId);
            }
            else if (tableName.equals(CMDatabaseHelper.CMTableNames.TABLE_SECURE)) {
                settingsValue = Settings.Secure.getStringForUser(contentResolver, settingsKey,
                        userId);
            }
            else if (tableName.equals(CMDatabaseHelper.CMTableNames.TABLE_GLOBAL)) {
                settingsValue = Settings.Global.getStringForUser(contentResolver, settingsKey,
                        userId);
            }

            if (LOCAL_LOGV) Log.d(TAG, "Table: " + tableName + ", Key: " + settingsKey + ", Value: "
                    + settingsValue);

            ContentValues contentValue = new ContentValues();
            contentValue.put(Settings.NameValueTable.NAME, cmSettingsKey);
            contentValue.put(Settings.NameValueTable.VALUE, settingsValue);
            contentValues[migrateSettingsCount++] = contentValue;
        }

        int rowsInserted = 0;
        if (contentValues.length > 0) {
            Uri uri = mUriBuilder.build();
            uri = uri.buildUpon().appendPath(tableName).build();
            rowsInserted = bulkInsertForUser(userId, uri, contentValues);
        }

        return rowsInserted;
    }

    /**
     * Performs cleanup for the removed user.
     * @param userId The id of the user that is removed.
     */
    private void onUserRemoved(int userId) {
        synchronized (this) {
            // the db file itself will be deleted automatically, but we need to tear down
            // our helpers and other internal bookkeeping.

            mDbHelpers.delete(userId);

            if (LOCAL_LOGV) Log.d(TAG, "User " + userId + " is removed");
        }
    }

    // endregion Migration Methods

    // region Content Provider Methods

    @Override
    public Bundle call(String method, String request, Bundle args) {
        if (LOCAL_LOGV) Log.d(TAG, "Call method: " + method);

        int callingUserId = UserHandle.getCallingUserId();
        if (args != null) {
            int reqUser = args.getInt(CMSettings.CALL_METHOD_USER_KEY, callingUserId);
            if (reqUser != callingUserId) {
                callingUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(),
                        Binder.getCallingUid(), reqUser, false, true,
                        "get/set setting for user", null);
                if (LOCAL_LOGV) Log.v(TAG, "   access setting for user " + callingUserId);
            }
        }

        // Migrate methods
        if (CMSettings.CALL_METHOD_MIGRATE_SETTINGS.equals(method)) {
            migrateCMSettingsForExistingUsersIfNeeded();

            return null;
        } else if (CMSettings.CALL_METHOD_MIGRATE_SETTINGS_FOR_USER.equals(method)) {
            migrateCMSettingsForUser(callingUserId);

            return null;
        }

        // Get methods
        if (CMSettings.CALL_METHOD_GET_SYSTEM.equals(method)) {
            return lookupSingleValue(callingUserId, CMSettings.System.CONTENT_URI, request);
        }
        else if (CMSettings.CALL_METHOD_GET_SECURE.equals(method)) {
            return lookupSingleValue(callingUserId, CMSettings.Secure.CONTENT_URI, request);
        }
        else if (CMSettings.CALL_METHOD_GET_GLOBAL.equals(method)) {
            return lookupSingleValue(callingUserId, CMSettings.Global.CONTENT_URI, request);
        }

        // Put methods - new value is in the args bundle under the key named by
        // the Settings.NameValueTable.VALUE static.
        final String newValue = (args == null)
                ? null : args.getString(Settings.NameValueTable.VALUE);

        // Framework can't do automatic permission checking for calls, so we need
        // to do it here.
        if (getContext().checkCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.WRITE_SETTINGS) !=
                PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    String.format("Permission denial: writing to settings requires %1$s",
                            cyanogenmod.platform.Manifest.permission.WRITE_SETTINGS));
        }

        // Put methods
        final ContentValues values = new ContentValues();
        values.put(Settings.NameValueTable.NAME, request);
        values.put(Settings.NameValueTable.VALUE, newValue);

        if (CMSettings.CALL_METHOD_PUT_SYSTEM.equals(method)) {
            insertForUser(callingUserId, CMSettings.System.CONTENT_URI, values);
        }
        else if (CMSettings.CALL_METHOD_PUT_SECURE.equals(method)) {
            insertForUser(callingUserId, CMSettings.Secure.CONTENT_URI, values);
        }
        else if (CMSettings.CALL_METHOD_PUT_GLOBAL.equals(method)) {
            insertForUser(callingUserId, CMSettings.Global.CONTENT_URI, values);
        }

        return null;
    }

    /**
     * Looks up a single value for a specific user, uri, and key.
     * @param userId The id of the user to perform the lookup for.
     * @param uri The uri for which table to perform the lookup in.
     * @param key The key to perform the lookup with.
     * @return A single value stored in a {@link Bundle}.
     */
    private Bundle lookupSingleValue(int userId, Uri uri, String key) {
        Cursor cursor = null;
        try {
            cursor = queryForUser(userId, uri, new String[]{ Settings.NameValueTable.VALUE },
                    Settings.NameValueTable.NAME + " = ?", new String[]{ key }, null);

            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                String value = cursor.getString(0);
                return value == null ? NULL_SETTING : Bundle.forPair(Settings.NameValueTable.VALUE,
                        value);
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "settings lookup error", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return NULL_SETTING;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return queryForUser(UserHandle.getCallingUserId(), uri, projection, selection,
                selectionArgs, sortOrder);
    }

    /**
     * Performs a query for a specific user.
     * @param userId The id of the user to perform the query for.
     * @param uri The uri for which table to perform the query on. Optionally, the uri can end in
     *     the name of a specific element to query for.
     * @param projection The columns that are returned in the {@link Cursor}.
     * @param selection The column names that the selection criteria applies to.
     * @param selectionArgs The column values that the selection criteria applies to.
     * @param sortOrder The ordering of how the values should be returned in the {@link Cursor}.
     * @return {@link Cursor} of the results from the query.
     */
    private Cursor queryForUser(int userId, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        int code = sUriMatcher.match(uri);
        String tableName = getTableNameFromUriMatchCode(code);
        checkWritePermissions(tableName);

        CMDatabaseHelper dbHelper = getOrEstablishDatabase(getUserIdForTable(tableName, userId));
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(tableName);

        Cursor returnCursor;
        if (isItemUri(code)) {
            // The uri is looking for an element with a specific name
            returnCursor = queryBuilder.query(db, projection, NAME_SELECTION,
                    new String[] { uri.getLastPathSegment() }, null, null, sortOrder);
        } else {
            returnCursor = queryBuilder.query(db, projection, selection, selectionArgs, null,
                    null, sortOrder);
        }

        // the default Cursor interface does not support per-user observation
        try {
            AbstractCursor abstractCursor = (AbstractCursor) returnCursor;
            abstractCursor.setNotificationUri(getContext().getContentResolver(), uri, userId);
        } catch (ClassCastException e) {
            // details of the concrete Cursor implementation have changed and this code has
            // not been updated to match -- complain and fail hard.
            Log.wtf(TAG, "Incompatible cursor derivation");
            throw e;
        }

        return returnCursor;
    }

    @Override
    public String getType(Uri uri) {
        int code = sUriMatcher.match(uri);
        String tableName = getTableNameFromUriMatchCode(code);

        if (isItemUri(code)) {
            return "vnd.android.cursor.item/" + tableName;
        } else {
            return "vnd.android.cursor.dir/" + tableName;
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return bulkInsertForUser(UserHandle.getCallingUserId(), uri, values);
    }

    /**
     * Performs a bulk insert for a specific user.
     * @param userId The user id to perform the bulk insert for.
     * @param uri The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database.
     *    This must not be {@code null}.
     * @return Number of rows inserted.
     */
    int bulkInsertForUser(int userId, Uri uri, ContentValues[] values) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        if (values == null) {
            throw new IllegalArgumentException("ContentValues cannot be null");
        }

        int numRowsAffected = 0;

        String tableName = getTableNameFromUri(uri);
        checkWritePermissions(tableName);

        CMDatabaseHelper dbHelper = getOrEstablishDatabase(getUserIdForTable(tableName, userId));
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                if (value == null) {
                    continue;
                }

                long rowId = db.insert(tableName, null, value);

                if (rowId >= 0) {
                    numRowsAffected++;
                } else {
                    return 0;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (numRowsAffected > 0) {
            notifyChange(uri, tableName, userId);
            if (LOCAL_LOGV) Log.d(TAG, tableName + ": " + numRowsAffected + " row(s) inserted");
        }

        return numRowsAffected;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return insertForUser(UserHandle.getCallingUserId(), uri, values);
    }

    /**
     * Performs insert for a specific user.
     * @param userId The user id to perform the insert for.
     * @param uri The content:// URI of the insertion request.
     * @param values A sets of column_name/value pairs to add to the database.
     *    This must not be {@code null}.
     * @return
     */
    private Uri insertForUser(int userId, Uri uri, ContentValues values) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        if (values == null) {
            throw new IllegalArgumentException("ContentValues cannot be null");
        }

        String tableName = getTableNameFromUri(uri);
        checkWritePermissions(tableName);

        CMDatabaseHelper dbHelper = getOrEstablishDatabase(getUserIdForTable(tableName, userId));

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(tableName, null, values);

        Uri returnUri = null;
        if (rowId > -1) {
            String name = values.getAsString(Settings.NameValueTable.NAME);
            returnUri = Uri.withAppendedPath(uri, name);
            notifyChange(returnUri, tableName, userId);
            if (LOCAL_LOGV) Log.d(TAG, "Inserted row id: " + rowId + " into tableName: " +
                    tableName);
        }

        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        int numRowsAffected = 0;

        // Allow only selection by key; a null/empty selection string will cause all rows in the
        // table to be deleted
        if (!TextUtils.isEmpty(selection) && selectionArgs.length > 0) {
            String tableName = getTableNameFromUri(uri);
            checkWritePermissions(tableName);

            int callingUserId = UserHandle.getCallingUserId();
            CMDatabaseHelper dbHelper = getOrEstablishDatabase(getUserIdForTable(tableName,
                    callingUserId));

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            numRowsAffected = db.delete(tableName, selection, selectionArgs);

            if (numRowsAffected > 0) {
                notifyChange(uri, tableName, callingUserId);
                if (LOCAL_LOGV) Log.d(TAG, tableName + ": " + numRowsAffected + " row(s) deleted");
            }
        }

        return numRowsAffected;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // NOTE: update() is never called by the front-end CMSettings API, and updates that
        // wind up affecting rows in Secure that are globally shared will not have the
        // intended effect (the update will be invisible to the rest of the system).
        // This should have no practical effect, since writes to the Secure db can only
        // be done by system code, and that code should be using the correct API up front.
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        if (values == null) {
            throw new IllegalArgumentException("ContentValues cannot be null");
        }

        String tableName = getTableNameFromUri(uri);
        checkWritePermissions(tableName);

        int callingUserId = UserHandle.getCallingUserId();
        CMDatabaseHelper dbHelper = getOrEstablishDatabase(getUserIdForTable(tableName,
                callingUserId));

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int numRowsAffected = db.update(tableName, values, selection, selectionArgs);

        if (numRowsAffected > 0) {
            notifyChange(uri, tableName, callingUserId);
            if (LOCAL_LOGV) Log.d(TAG, tableName + ": " + numRowsAffected + " row(s) updated");
        }

        return numRowsAffected;
    }

    // endregion Content Provider Methods

    /**
     * Tries to get a {@link CMDatabaseHelper} for the specified user and if it does not exist, a
     * new instance of {@link CMDatabaseHelper} is created for the specified user and returned.
     * @param callingUser
     * @return
     */
    private CMDatabaseHelper getOrEstablishDatabase(int callingUser) {
        if (callingUser >= android.os.Process.SYSTEM_UID) {
            if (USER_CHECK_THROWS) {
                throw new IllegalArgumentException("Uid rather than user handle: " + callingUser);
            } else {
                Log.wtf(TAG, "Establish db for uid rather than user: " + callingUser);
            }
        }

        long oldId = Binder.clearCallingIdentity();
        try {
            CMDatabaseHelper dbHelper;
            synchronized (this) {
                dbHelper = mDbHelpers.get(callingUser);
            }
            if (null == dbHelper) {
                establishDbTracking(callingUser);
                synchronized (this) {
                    dbHelper = mDbHelpers.get(callingUser);
                }
            }
            return dbHelper;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    /**
     * Check if a {@link CMDatabaseHelper} exists for a user and if it doesn't, a new helper is
     * created and added to the list of tracked database helpers
     * @param userId
     */
    private void establishDbTracking(int userId) {
        CMDatabaseHelper dbHelper;

        synchronized (this) {
            dbHelper = mDbHelpers.get(userId);
            if (LOCAL_LOGV) {
                Log.i(TAG, "Checking cm settings db helper for user " + userId);
            }
            if (dbHelper == null) {
                if (LOCAL_LOGV) {
                    Log.i(TAG, "Installing new cm settings db helper for user " + userId);
                }
                dbHelper = new CMDatabaseHelper(getContext(), userId);
                mDbHelpers.append(userId, dbHelper);
            }
        }

        // Initialization of the db *outside* the locks.  It's possible that racing
        // threads might wind up here, the second having read the cache entries
        // written by the first, but that's benign: the SQLite helper implementation
        // manages concurrency itself, and it's important that we not run the db
        // initialization with any of our own locks held, so we're fine.
        dbHelper.getWritableDatabase();
    }

    /**
     * Makes sure the caller has permission to write this data.
     * @param tableName supplied by the caller
     * @throws SecurityException if the caller is forbidden to write.
     */
    private void checkWritePermissions(String tableName) {
        if ((CMDatabaseHelper.CMTableNames.TABLE_SECURE.equals(tableName) ||
                CMDatabaseHelper.CMTableNames.TABLE_GLOBAL.equals(tableName)) &&
                getContext().checkCallingOrSelfPermission(
                        cyanogenmod.platform.Manifest.permission.WRITE_SECURE_SETTINGS) !=
                        PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    String.format("Permission denial: writing to cm secure settings requires %1$s",
                            cyanogenmod.platform.Manifest.permission.WRITE_SECURE_SETTINGS));
        }
    }

    /**
     * Returns whether the matched uri code refers to an item in a table
     * @param code
     * @return
     */
    private boolean isItemUri(int code) {
        switch (code) {
            case SYSTEM:
            case SECURE:
            case GLOBAL:
                return false;
            case SYSTEM_ITEM_NAME:
            case SECURE_ITEM_NAME:
            case GLOBAL_ITEM_NAME:
                return true;
            default:
                throw new IllegalArgumentException("Invalid uri match code: " + code);
        }
    }

    /**
     * Utilizes an {@link UriMatcher} to check for a valid combination of scheme, authority, and
     * path and returns the corresponding table name
     * @param uri
     * @return Table name
     */
    private String getTableNameFromUri(Uri uri) {
        int code = sUriMatcher.match(uri);

        return getTableNameFromUriMatchCode(code);
    }

    /**
     * Returns the corresponding table name for the matched uri code
     * @param code
     * @return
     */
    private String getTableNameFromUriMatchCode(int code) {
        switch (code) {
            case SYSTEM:
            case SYSTEM_ITEM_NAME:
                return CMDatabaseHelper.CMTableNames.TABLE_SYSTEM;
            case SECURE:
            case SECURE_ITEM_NAME:
                return CMDatabaseHelper.CMTableNames.TABLE_SECURE;
            case GLOBAL:
            case GLOBAL_ITEM_NAME:
                return CMDatabaseHelper.CMTableNames.TABLE_GLOBAL;
            default:
                throw new IllegalArgumentException("Invalid uri match code: " + code);
        }
    }

    /**
     * If the table is Global, the owner's user id is returned. Otherwise, the original user id
     * is returned.
     * @param tableName
     * @param userId
     * @return User id
     */
    private int getUserIdForTable(String tableName, int userId) {
        return CMDatabaseHelper.CMTableNames.TABLE_GLOBAL.equals(tableName) ?
                UserHandle.USER_OWNER : userId;
    }

    /**
     * Modify setting version for an updated table before notifying of change. The
     * {@link CMSettings} class uses these to provide client-side caches.
     * @param uri to send notifications for
     * @param userId
     */
    private void notifyChange(Uri uri, String tableName, int userId) {
        String property = null;
        final boolean isGlobal = tableName.equals(CMDatabaseHelper.CMTableNames.TABLE_GLOBAL);
        if (tableName.equals(CMDatabaseHelper.CMTableNames.TABLE_SYSTEM)) {
            property = CMSettings.System.SYS_PROP_CM_SETTING_VERSION;
        } else if (tableName.equals(CMDatabaseHelper.CMTableNames.TABLE_SECURE)) {
            property = CMSettings.Secure.SYS_PROP_CM_SETTING_VERSION;
        } else if (isGlobal) {
            property = CMSettings.Global.SYS_PROP_CM_SETTING_VERSION;
        }

        if (property != null) {
            long version = SystemProperties.getLong(property, 0) + 1;
            if (LOCAL_LOGV) Log.v(TAG, "property: " + property + "=" + version);
            SystemProperties.set(property, Long.toString(version));
        }

        final int notifyTarget = isGlobal ? UserHandle.USER_ALL : userId;
        final long oldId = Binder.clearCallingIdentity();
        try {
            getContext().getContentResolver().notifyChange(uri, null, true, notifyTarget);
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
        if (LOCAL_LOGV) Log.v(TAG, "notifying for " + notifyTarget + ": " + uri);
    }

    // TODO Add caching
}
