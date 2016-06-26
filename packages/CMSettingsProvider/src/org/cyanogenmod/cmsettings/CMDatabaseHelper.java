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
    private static final int DATABASE_VERSION = 6;

    public static class CMTableNames {
        public static final String TABLE_SYSTEM = "system";
        public static final String TABLE_SECURE = "secure";
        public static final String TABLE_GLOBAL = "global";
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
        if (LOCAL_LOGV) Log.d(TAG, "Upgrading from version: " + oldVersion + " to " + newVersion);
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

        if (upgradeVersion < 3) {
            db.beginTransaction();
            SQLiteStatement stmt = null;
            try {
                stmt = db.compileStatement("INSERT INTO secure(name,value)"
                        + " VALUES(?,?);");
                loadStringSetting(stmt, CMSettings.Secure.PROTECTED_COMPONENT_MANAGERS,
                        R.string.def_protected_component_managers);
                db.setTransactionSuccessful();
            } finally {
                if (stmt != null) stmt.close();
                db.endTransaction();
            }
            upgradeVersion = 3;
        }

        if (upgradeVersion < 4) {
            if (mUserHandle == UserHandle.USER_OWNER) {
                db.beginTransaction();
                SQLiteStatement stmt = null;
                try {
                    stmt = db.compileStatement("INSERT INTO secure(name,value)"
                            + " VALUES(?,?);");
                    final String provisionedFlag = Settings.Global.getString(
                            mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED);
                    loadSetting(stmt, CMSettings.Secure.CM_SETUP_WIZARD_COMPLETED, provisionedFlag);
                    db.setTransactionSuccessful();
                } finally {
                    if (stmt != null) stmt.close();
                    db.endTransaction();
                }
            }
            upgradeVersion = 4;
        }

        if (upgradeVersion < 5) {
            if (mUserHandle == UserHandle.USER_OWNER) {
                db.beginTransaction();
                SQLiteStatement stmt = null;
                try {
                    stmt = db.compileStatement("INSERT INTO global(name,value)"
                            + " VALUES(?,?);");
                    loadIntegerSetting(stmt, CMSettings.Global.WEATHER_TEMPERATURE_UNIT,
                            R.integer.def_temperature_unit);
                    db.setTransactionSuccessful();
                } finally {
                    if (stmt != null) stmt.close();
                    db.endTransaction();
                }
            }
            upgradeVersion = 5;
        }

        if (upgradeVersion < 6) {
            // Move force_show_navbar to global
            if (mUserHandle == UserHandle.USER_OWNER) {
                moveSettingsToNewTable(db, CMTableNames.TABLE_SECURE,
                        CMTableNames.TABLE_GLOBAL, new String[] {
                        CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR
                }, true);
            }
            upgradeVersion = 6;
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

    private void moveSettingsToNewTable(SQLiteDatabase db,
                                        String sourceTable, String destTable,
                                        String[] settingsToMove, boolean doIgnore) {
        // Copy settings values from the source table to the dest, and remove from the source
        SQLiteStatement insertStmt = null;
        SQLiteStatement deleteStmt = null;

        db.beginTransaction();
        try {
            insertStmt = db.compileStatement("INSERT "
                    + (doIgnore ? " OR IGNORE " : "")
                    + " INTO " + destTable + " (name,value) SELECT name,value FROM "
                    + sourceTable + " WHERE name=?");
            deleteStmt = db.compileStatement("DELETE FROM " + sourceTable + " WHERE name=?");

            for (String setting : settingsToMove) {
                insertStmt.bindString(1, setting);
                insertStmt.execute();

                deleteStmt.bindString(1, setting);
                deleteStmt.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (insertStmt != null) {
                insertStmt.close();
            }
            if (deleteStmt != null) {
                deleteStmt.close();
            }
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
        loadSystemSettings(db);
        loadSecureSettings(db);
        // The global table only exists for the 'owner' user
        if (mUserHandle == UserHandle.USER_OWNER) {
            loadGlobalSettings(db);
        }
    }

    private void loadSecureSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO secure(name,value)"
                    + " VALUES(?,?);");
            // Secure
            loadBooleanSetting(stmt, CMSettings.Secure.ADVANCED_MODE,
                    R.bool.def_advanced_mode);

            loadRegionLockedStringSetting(stmt,
                    CMSettings.Secure.DEFAULT_THEME_COMPONENTS, R.string.def_theme_components);

            loadRegionLockedStringSetting(stmt,
                    CMSettings.Secure.DEFAULT_THEME_PACKAGE, R.string.def_theme_package);

            loadIntegerSetting(stmt, CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR,
                    R.integer.def_force_show_navbar);

            loadStringSetting(stmt, CMSettings.Secure.QS_TILES,
                    org.cyanogenmod.platform.internal.
                            R.string.config_defaultQuickSettingsTiles);

            loadBooleanSetting(stmt, CMSettings.Secure.QS_USE_MAIN_TILES,
                    R.bool.def_sysui_qs_main_tiles);

            loadBooleanSetting(stmt, CMSettings.Secure.STATS_COLLECTION,
                    R.bool.def_stats_collection);

            loadBooleanSetting(stmt, CMSettings.Secure.LOCKSCREEN_VISUALIZER_ENABLED,
                    R.bool.def_lockscreen_visualizer);

            loadStringSetting(stmt,
                    CMSettings.Secure.PROTECTED_COMPONENT_MANAGERS,
                    R.string.def_protected_component_managers);

            loadStringSetting(stmt,
                    CMSettings.Secure.ENABLED_EVENT_LIVE_LOCKS_KEY,
                    R.string.def_enabled_event_lls_components);

            final String provisionedFlag = Settings.Global.getString(mContext.getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED);
            loadSetting(stmt, CMSettings.Secure.CM_SETUP_WIZARD_COMPLETED, provisionedFlag);
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadSystemSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value)"
                    + " VALUES(?,?);");
            // System
            loadIntegerSetting(stmt, CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    R.integer.def_qs_quick_pulldown);

            loadIntegerSetting(stmt, CMSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                    R.integer.def_notification_brightness_level);

            loadBooleanSetting(stmt, CMSettings.System.NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                    R.bool.def_notification_multiple_leds);

            loadBooleanSetting(stmt, CMSettings.System.SYSTEM_PROFILES_ENABLED,
                    R.bool.def_profiles_enabled);

            loadIntegerSetting(stmt, CMSettings.System.ENABLE_PEOPLE_LOOKUP,
                    R.integer.def_people_lookup);

            loadBooleanSetting(stmt, CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                    R.bool.def_notification_pulse_custom_enable);

            loadBooleanSetting(stmt, CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                    R.bool.def_swap_volume_keys_on_rotation);

            loadIntegerSetting(stmt, CMSettings.System.STATUS_BAR_BATTERY_STYLE,
                    R.integer.def_battery_style);

            if (mContext.getResources().getBoolean(R.bool.def_notification_pulse_custom_enable)) {
                loadStringSetting(stmt, CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                        R.string.def_notification_pulse_custom_value);
            }
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadGlobalSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO global(name,value)"
                    + " VALUES(?,?);");
            // Global
            loadBooleanSetting(stmt,
                    CMSettings.Global.POWER_NOTIFICATIONS_ENABLED,
                    R.bool.def_power_notifications_enabled);

            loadBooleanSetting(stmt,
                    CMSettings.Global.POWER_NOTIFICATIONS_VIBRATE,
                    R.bool.def_power_notifications_vibrate);

            loadStringSetting(stmt,
                    CMSettings.Global.POWER_NOTIFICATIONS_RINGTONE,
                    R.string.def_power_notifications_ringtone);

            loadIntegerSetting(stmt, CMSettings.Global.WEATHER_TEMPERATURE_UNIT,
                    R.integer.def_temperature_unit);
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Loads a region locked string setting into a database table. If the resource for the specific
     * mcc is not found, the setting is loaded from the default resources.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the string resource.
     */
    private void loadRegionLockedStringSetting(SQLiteStatement stmt, String name, int resId) {
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
        loadSetting(stmt, name, value);
    }

    /**
     * Loads a string resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the string resource.
     */
    private void loadStringSetting(SQLiteStatement stmt, String name, int resId) {
        loadSetting(stmt, name, mContext.getResources().getString(resId));
    }

    /**
     * Loads a boolean resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the boolean resource.
     */
    private void loadBooleanSetting(SQLiteStatement stmt, String name, int resId) {
        loadSetting(stmt, name,
                mContext.getResources().getBoolean(resId) ? "1" : "0");
    }

    /**
     * Loads an integer resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the integer resource.
     */
    private void loadIntegerSetting(SQLiteStatement stmt, String name, int resId) {
        loadSetting(stmt, name,
                Integer.toString(mContext.getResources().getInteger(resId)));
    }

    private void loadSetting(SQLiteStatement stmt, String key, Object value) {
        stmt.bindString(1, key);
        stmt.bindString(2, value.toString());
        stmt.execute();
    }
}
