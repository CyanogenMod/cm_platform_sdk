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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import cyanogenmod.providers.CMSettings;

/**
 * The CMSettingsProvider serves as a {@link ContentProvider} for CM specific settings
 */
public class CMSettingsProvider extends ContentProvider {
    private static final String TAG = "CMSettingsProvider";
    private static final boolean LOCAL_LOGV = false;

    private static final boolean USER_CHECK_THROWS = true;

    // Each defined user has their own settings
    protected final SparseArray<CMDatabaseHelper> mDbHelpers = new SparseArray<CMDatabaseHelper>();

    private static final int SYSTEM = 1;
    private static final int SECURE = 2;
    private static final int GLOBAL = 3;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_SYSTEM,
                SYSTEM);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_SECURE,
                SECURE);
        sUriMatcher.addURI(CMSettings.AUTHORITY, CMDatabaseHelper.CMTableNames.TABLE_GLOBAL,
                GLOBAL);
        // TODO add other paths for getting specific items
    }

    private UserManager mUserManager;
    private Uri.Builder mUriBuilder;

    @Override
    public boolean onCreate() {
        if (LOCAL_LOGV) Log.d(TAG, "Creating CMSettingsProvider");

        mUserManager = UserManager.get(getContext());

        establishDbTracking(UserHandle.USER_OWNER);

        mUriBuilder = new Uri.Builder();
        mUriBuilder.scheme(ContentResolver.SCHEME_CONTENT);
        mUriBuilder.authority(CMSettings.AUTHORITY);

        // TODO Add migration for cm settings

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        String tableName = getTableNameFromUri(uri);
        checkWritePermissions(tableName);

        int callingUserId = UserHandle.getCallingUserId();
        CMDatabaseHelper dbHelper = getOrEstablishDatabase(getUserIdForTable(tableName,
                callingUserId));
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(tableName);

        Cursor returnCursor = queryBuilder.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        // the default Cursor interface does not support per-user observation
        try {
            AbstractCursor abstractCursor = (AbstractCursor) returnCursor;
            abstractCursor.setNotificationUri(getContext().getContentResolver(), uri,
                    callingUserId);
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
        // TODO: Implement
        return null;
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

                    if (LOCAL_LOGV) Log.d(TAG, tableName + " <- " + values);
                } else {
                    return 0;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (numRowsAffected > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            notifyChange(uri, tableName, userId);
            if (LOCAL_LOGV) Log.d(TAG, tableName + ": " + numRowsAffected + " row(s) inserted");
        }

        return numRowsAffected;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
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
        long rowId = db.insert(tableName, null, values);

        Uri returnUri = null;
        if (rowId > -1) {
            returnUri = ContentUris.withAppendedId(uri, rowId);
            notifyChange(returnUri, tableName, callingUserId);
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
            getContext().getContentResolver().notifyChange(uri, null);
            if (LOCAL_LOGV) Log.d(TAG, tableName + ": " + numRowsAffected + " row(s) updated");
        }

        return numRowsAffected;
    }

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
     * Utilizes an {@link UriMatcher} to check for a valid combination of scheme, authority, and
     * path and returns the corresponding table name
     * @param uri
     * @return Table name
     */
    private String getTableNameFromUri(Uri uri) {
        int code = sUriMatcher.match(uri);

        switch (code) {
            case SYSTEM:
                return CMDatabaseHelper.CMTableNames.TABLE_SYSTEM;
            case SECURE:
                return CMDatabaseHelper.CMTableNames.TABLE_SECURE;
            case GLOBAL:
                return CMDatabaseHelper.CMTableNames.TABLE_GLOBAL;
            default:
                throw new IllegalArgumentException("Invalid uri: " + uri);
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
