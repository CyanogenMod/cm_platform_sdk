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

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

import java.io.File;

/**
 * The CMDatabaseHelper allows creation of a database to store CM specific settings for a user
 * in System, Secure, and Global tables.
 */
public class CMDatabaseHelper extends SQLiteOpenHelper{
    private static final String TAG = "CMDatabaseHelper";
    private static final boolean LOCAL_LOGV = false;

    private static final String DATABASE_NAME = "cmsettings.db";
    private static final int DATABASE_VERSION = 2;

    static class CMTableNames {
        static final String TABLE_SYSTEM = "system";
        static final String TABLE_SECURE = "secure";
        static final String TABLE_GLOBAL = "global";
    }

    private static final String CREATE_TABLE_SQL_FORMAT = "CREATE TABLE %s (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT UNIQUE ON CONFLICT REPLACE," +
            "value TEXT" +
            ");)";

    private static final String CREATE_INDEX_SQL_FORMAT = "CREATE INDEX %sIndex%d ON %s (name);";

    private static final String DROP_TABLE_SQL_FORMAT = "DROP TABLE IF EXISTS %s;";

    private static final String DROP_INDEX_SQL_FORMAT = "DROP INDEX IF EXISTS %sIndex%d;";

    private static final String MCC_PROP_NAME = "ro.prebundled.mcc";

    private Context mContext;
    private int mUserHandle;
    private String mPublicSrcDir;

    /**
     * Gets the appropriate database path for a specific user
     * @param userId The database path for this user
     * @return The database path string
     */
    static String dbNameForUser(final int userId) {
        // The owner gets the unadorned db name;
        if (userId == UserHandle.USER_OWNER) {
            return DATABASE_NAME;
        } else {
            // Place the database in the user-specific data tree so that it's
            // cleaned up automatically when the user is deleted.
            File databaseFile = new File(
                    Environment.getUserSystemDirectory(userId), DATABASE_NAME);
            return databaseFile.getPath();
        }
    }

    /**
     * Creates an instance of {@link CMDatabaseHelper}
     * @param context
     * @param userId
     */
    public CMDatabaseHelper(Context context, int userId) {
        super(context, dbNameForUser(userId), null, DATABASE_VERSION);
        mContext = context;
        mUserHandle = userId;

        try {
            String packageName = mContext.getPackageName();
            mPublicSrcDir = mContext.getPackageManager().getApplicationInfo(packageName, 0)
                    .publicSourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates System, Secure, and Global tables in the specified {@link SQLiteDatabase} and loads
     * default values into the created tables.
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();

        try {
            createDbTable(db, CMTableNames.TABLE_SYSTEM);
            createDbTable(db, CMTableNames.TABLE_SECURE);

            if (mUserHandle == UserHandle.USER_OWNER) {
                createDbTable(db, CMTableNames.TABLE_GLOBAL);
            }

            loadSettings(db);

            db.setTransactionSuccessful();

            if (LOCAL_LOGV) Log.d(TAG, "Successfully created tables for cm settings db");
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Creates a table and index for the specified database and table name
     * @param db The {@link SQLiteDatabase} to create the table and index in.
     * @param tableName The name of the database table to create.
     */
    private void createDbTable(SQLiteDatabase db, String tableName) {
        if (LOCAL_LOGV) Log.d(TAG, "Creating table and index for: " + tableName);

        String createTableSql = String.format(CREATE_TABLE_SQL_FORMAT, tableName);
        db.execSQL(createTableSql);

        String createIndexSql = String.format(CREATE_INDEX_SQL_FORMAT, tableName, 1, tableName);
        db.execSQL(createIndexSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int upgradeVersion = oldVersion;

        if (upgradeVersion < 2) {
            db.beginTransaction();
            try {
                loadSettings(db);

                db.setTransactionSuccessful();

                upgradeVersion = 2;
            } finally {
                db.endTransaction();
            }
        }

        // *** Remember to update DATABASE_VERSION above!

        if (upgradeVersion < newVersion) {
            Log.w(TAG, "Got stuck trying to upgrade db. Old version: " + oldVersion
                    + ", version stuck at: " +  upgradeVersion + ", new version: "
                            + newVersion + ". Must wipe the cm settings provider.");

            dropDbTable(db, CMTableNames.TABLE_SYSTEM);
            dropDbTable(db, CMTableNames.TABLE_SECURE);

            if (mUserHandle == UserHandle.USER_OWNER) {
                dropDbTable(db, CMTableNames.TABLE_GLOBAL);
            }

            onCreate(db);
        }
    }

    /**
     * Drops the table and index for the specified database and table name
     * @param db The {@link SQLiteDatabase} to drop the table and index in.
     * @param tableName The name of the database table to drop.
     */
    private void dropDbTable(SQLiteDatabase db, String tableName) {
        if (LOCAL_LOGV) Log.d(TAG, "Dropping table and index for: " + tableName);

        String dropTableSql = String.format(DROP_TABLE_SQL_FORMAT, tableName);
        db.execSQL(dropTableSql);

        String dropIndexSql = String.format(DROP_INDEX_SQL_FORMAT, tableName, 1);
        db.execSQL(dropIndexSql);
    }

    /**
     * Loads default values for specific settings into the database.
     * @param db The {@link SQLiteDatabase} to insert into.
     */
    private void loadSettings(SQLiteDatabase db) {
        // System
        loadIntegerSetting(db, CMTableNames.TABLE_SYSTEM, CMSettings.System.QS_QUICK_PULLDOWN,
                R.integer.def_qs_quick_pulldown);

        // Secure
        loadBooleanSetting(db, CMTableNames.TABLE_SECURE, CMSettings.Secure.ADVANCED_MODE,
                R.bool.def_advanced_mode);

        loadRegionLockedStringSetting(db, CMTableNames.TABLE_SECURE,
                CMSettings.Secure.DEFAULT_THEME_COMPONENTS, R.string.def_theme_components);

        loadRegionLockedStringSetting(db, CMTableNames.TABLE_SECURE,
                CMSettings.Secure.DEFAULT_THEME_PACKAGE, R.string.def_theme_package);

        loadIntegerSetting(db, CMTableNames.TABLE_SECURE, CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR,
                R.integer.def_force_show_navbar);

        loadStringSetting(db, CMTableNames.TABLE_SECURE, CMSettings.Secure.QS_TILES,
                R.string.def_qs_tiles);

        loadBooleanSetting(db, CMTableNames.TABLE_SECURE, CMSettings.Secure.STATS_COLLECTION,
                R.bool.def_stats_collection);

        loadBooleanSetting(db, CMTableNames.TABLE_GLOBAL,
                CMSettings.Global.POWER_NOTIFICATIONS_ENABLED,
                R.bool.def_power_notifications_enabled);

        loadBooleanSetting(db, CMTableNames.TABLE_GLOBAL,
                CMSettings.Global.POWER_NOTIFICATIONS_VIBRATE,
                R.bool.def_power_notifications_vibrate);

        loadStringSetting(db, CMTableNames.TABLE_GLOBAL,
                CMSettings.Global.POWER_NOTIFICATIONS_RINGTONE,
                R.string.def_power_notifications_ringtone);

        loadIntegerSetting(db, CMTableNames.TABLE_SYSTEM, CMSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                R.integer.def_notification_brightness_level);

        loadBooleanSetting(db, CMTableNames.TABLE_SYSTEM, CMSettings.System.NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                R.bool.def_notification_multiple_leds);
    }

    /**
     * Loads a region locked string setting into a database table. If the resource for the specific
     * mcc is not found, the setting is loaded from the default resources.
     * @param db The {@link SQLiteDatabase} to insert into.
     * @param tableName The name of the table to insert into.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the string resource.
     */
    private void loadRegionLockedStringSetting(SQLiteDatabase db, String tableName, String name,
            int resId) {
        String mcc = SystemProperties.get(MCC_PROP_NAME);
        Resources customResources = null;

        if (!TextUtils.isEmpty(mcc)) {
            Configuration tempConfiguration = new Configuration();
            boolean useTempConfig = false;

            try {
                tempConfiguration.mcc = Integer.parseInt(mcc);
                useTempConfig = true;
            } catch (NumberFormatException e) {
                // not able to parse mcc, catch exception and exit out of this logic
                e.printStackTrace();
            }

            if (useTempConfig) {
                AssetManager assetManager = new AssetManager();

                if (!TextUtils.isEmpty(mPublicSrcDir)) {
                    assetManager.addAssetPath(mPublicSrcDir);
                }

                customResources = new Resources(assetManager, new DisplayMetrics(),
                        tempConfiguration);
            }
        }

        String value = customResources == null ? mContext.getResources().getString(resId)
                : customResources.getString(resId);
        loadSettingsForTable(db, tableName, name, value);
    }

    /**
     * Loads a string resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param db The {@link SQLiteDatabase} to insert into.
     * @param tableName The name of the table to insert into.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the string resource.
     */
    private void loadStringSetting(SQLiteDatabase db, String tableName, String name, int resId) {
        loadSettingsForTable(db, tableName, name, mContext.getResources().getString(resId));
    }

    /**
     * Loads a boolean resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param db The {@link SQLiteDatabase} to insert into.
     * @param tableName The name of the table to insert into.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the boolean resource.
     */
    private void loadBooleanSetting(SQLiteDatabase db, String tableName, String name, int resId) {
        loadSettingsForTable(db, tableName, name,
                mContext.getResources().getBoolean(resId) ? "1" : "0");
    }

    /**
     * Loads an integer resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param db The {@link SQLiteDatabase} to insert into.
     * @param tableName The name of the table to insert into.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the integer resource.
     */
    private void loadIntegerSetting(SQLiteDatabase db, String tableName, String name, int resId) {
        loadSettingsForTable(db, tableName, name,
                Integer.toString(mContext.getResources().getInteger(resId)));
    }

    /**
     * Loads a name/value pair into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param db The {@link SQLiteDatabase} to insert into.
     * @param tableName The name of the table to insert into.
     * @param name The name of the value to insert into the table.
     * @param value The value to insert into the table.
     */
    private void loadSettingsForTable(SQLiteDatabase db, String tableName, String name,
            String value) {
        if (LOCAL_LOGV) Log.d(TAG, "Loading key: " + name + ", value: " + value);

        ContentValues contentValues = new ContentValues();
        contentValues.put(Settings.NameValueTable.NAME, name);
        contentValues.put(Settings.NameValueTable.VALUE, value);

        db.insertWithOnConflict(tableName, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
