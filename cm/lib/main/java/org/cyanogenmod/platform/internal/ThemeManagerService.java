/*
 * Copyright (C) 2014-2016 The CyanogenMod Project
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

package org.cyanogenmod.platform.internal;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.ThemeConfig;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.ThemesContract.MixnMatchColumns;
import cyanogenmod.providers.ThemesContract.ThemesColumns;
import cyanogenmod.themes.IThemeChangeListener;
import cyanogenmod.themes.IThemeProcessingListener;
import cyanogenmod.themes.IThemeService;
import cyanogenmod.themes.ThemeChangeRequest;

import org.cyanogenmod.internal.util.ImageUtils;
import org.cyanogenmod.internal.util.QSConstants;
import org.cyanogenmod.internal.util.QSUtils;
import org.cyanogenmod.internal.util.ThemeUtils;
import org.cyanogenmod.platform.internal.AppsFailureReceiver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import libcore.io.IoUtils;

import static android.content.res.ThemeConfig.SYSTEM_DEFAULT;
import static cyanogenmod.platform.Manifest.permission.ACCESS_THEME_MANAGER;
import static org.cyanogenmod.internal.util.ThemeUtils.SYSTEM_THEME_PATH;
import static org.cyanogenmod.internal.util.ThemeUtils.THEME_BOOTANIMATION_PATH;

public class ThemeManagerService extends CMSystemService {

    private static final String TAG = ThemeManagerService.class.getName();

    private static final boolean DEBUG = false;

    private static final String GOOGLE_SETUPWIZARD_PACKAGE = "com.google.android.setupwizard";
    private static final String CM_SETUPWIZARD_PACKAGE = "com.cyanogenmod.setupwizard";
    private static final String MANAGED_PROVISIONING_PACKAGE = "com.android.managedprovisioning";

    private static final String CATEGORY_THEME_CHOOSER = "cyanogenmod.intent.category.APP_THEMES";

    // Defines a min and max compatible api level for themes on this system.
    private static final int MIN_COMPATIBLE_VERSION = 21;

    private HandlerThread mWorker;
    private ThemeWorkerHandler mHandler;
    private ResourceProcessingHandler mResourceProcessingHandler;
    private Context mContext;
    private PackageManager mPM;
    private int mProgress;
    private boolean mWallpaperChangedByUs = false;
    private int mCurrentUserId = UserHandle.USER_OWNER;

    private boolean mIsThemeApplying = false;

    private final RemoteCallbackList<IThemeChangeListener> mClients = new RemoteCallbackList<>();

    private final RemoteCallbackList<IThemeProcessingListener> mProcessingListeners =
            new RemoteCallbackList<>();

    final private ArrayList<String> mThemesToProcessQueue = new ArrayList<>();

    private long mLastThemeChangeTime = 0;
    private int mLastThemeChangeRequestType;

    private class ThemeWorkerHandler extends Handler {
        private static final int MESSAGE_CHANGE_THEME = 1;
        private static final int MESSAGE_APPLY_DEFAULT_THEME = 2;
        private static final int MESSAGE_REBUILD_RESOURCE_CACHE = 3;

        public ThemeWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CHANGE_THEME:
                    final ThemeChangeRequest request = (ThemeChangeRequest) msg.obj;
                    doApplyTheme(request, msg.arg1 == 1);
                    break;
                case MESSAGE_APPLY_DEFAULT_THEME:
                    doApplyDefaultTheme();
                    break;
                case MESSAGE_REBUILD_RESOURCE_CACHE:
                    doRebuildResourceCache();
                    break;
                default:
                    Log.w(TAG, "Unknown message " + msg.what);
                    break;
            }
        }
    }

    private class ResourceProcessingHandler extends Handler {
        private static final int MESSAGE_QUEUE_THEME_FOR_PROCESSING = 3;
        private static final int MESSAGE_DEQUEUE_AND_PROCESS_THEME = 4;

        public ResourceProcessingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_QUEUE_THEME_FOR_PROCESSING:
                    String pkgName = (String) msg.obj;
                    synchronized (mThemesToProcessQueue) {
                        if (!mThemesToProcessQueue.contains(pkgName)) {
                            if (DEBUG) Log.d(TAG, "Adding " + pkgName + " for processing");
                            mThemesToProcessQueue.add(pkgName);
                            if (mThemesToProcessQueue.size() == 1) {
                                this.sendEmptyMessage(MESSAGE_DEQUEUE_AND_PROCESS_THEME);
                            }
                        }
                    }
                    break;
                case MESSAGE_DEQUEUE_AND_PROCESS_THEME:
                    synchronized (mThemesToProcessQueue) {
                        pkgName = mThemesToProcessQueue.get(0);
                    }
                    if (pkgName != null) {
                        if (DEBUG) Log.d(TAG, "Processing " + pkgName);
                        String name;
                        try {
                            PackageInfo pi = mPM.getPackageInfo(pkgName, 0);
                            name = getThemeName(pi);
                        } catch (PackageManager.NameNotFoundException e) {
                            name = null;
                        }

                        int result = mPM.processThemeResources(pkgName);
                        if (result < 0) {
                            postFailedThemeInstallNotification(name != null ? name : pkgName);
                        }
                        sendThemeResourcesCachedBroadcast(pkgName, result);

                        synchronized (mThemesToProcessQueue) {
                            mThemesToProcessQueue.remove(0);
                            if (mThemesToProcessQueue.size() > 0 &&
                                    !hasMessages(MESSAGE_DEQUEUE_AND_PROCESS_THEME)) {
                                this.sendEmptyMessage(MESSAGE_DEQUEUE_AND_PROCESS_THEME);
                            }
                        }
                        postFinishedProcessing(pkgName);
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown message " + msg.what);
                    break;
            }
        }
    }


    public ThemeManagerService(Context context) {
        super(context);
        mContext = context;
        mWorker = new HandlerThread("ThemeServiceWorker", Process.THREAD_PRIORITY_BACKGROUND);
        mWorker.start();
        mHandler = new ThemeWorkerHandler(mWorker.getLooper());
        Log.i(TAG, "Spawned worker thread");

        HandlerThread processingThread = new HandlerThread("ResourceProcessingThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        processingThread.start();
        mResourceProcessingHandler =
                new ResourceProcessingHandler(processingThread.getLooper());

        // create the theme directories if they do not exist
        ThemeUtils.createThemeDirIfNotExists();
        ThemeUtils.createFontDirIfNotExists();
        ThemeUtils.createAlarmDirIfNotExists();
        ThemeUtils.createNotificationDirIfNotExists();
        ThemeUtils.createRingtoneDirIfNotExists();
        ThemeUtils.createIconCacheDirIfNotExists();
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.THEMES;
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_THEME_SERVICE, mService);
        // listen for wallpaper changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        mContext.registerReceiver(mWallpaperChangeReceiver, filter);

        filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(mUserChangeReceiver, filter);

        mPM = mContext.getPackageManager();
    }

    @Override
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == SystemService.PHASE_ACTIVITY_MANAGER_READY) {
            if (!isThemeApiUpToDate()) {
                Log.d(TAG, "The system has been upgraded to a theme new api, " +
                        "checking if currently set theme is compatible");
                removeObsoleteThemeOverlayIfExists();
                updateThemeApi();
            }
            registerAppsFailureReceiver();
            processInstalledThemes();
        } else if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            publishThemesTile();
        }
    }

    private void registerAppsFailureReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(cyanogenmod.content.Intent.ACTION_APP_FAILURE);
        filter.addAction(ThemeUtils.ACTION_THEME_CHANGED);
        mContext.registerReceiver(new AppsFailureReceiver(), filter);
    }

    private void removeObsoleteThemeOverlayIfExists() {
        // Get the current overlay theme so we can see it it's overlay should be unapplied
        final IActivityManager am = ActivityManagerNative.getDefault();
        ThemeConfig config = null;
        try {
            if (am != null) {
                config = am.getConfiguration().themeConfig;
            } else {
                Log.e(TAG, "ActivityManager getDefault() " +
                        "returned null, cannot remove obsolete theme");
            }
        } catch(RemoteException e) {
            Log.e(TAG, "Failed to get the theme config ", e);
        }
        if (config == null) return; // No need to unapply a theme if one isn't set

        // Populate the currentTheme map for the components we care about, we'll look
        // at the compatibility of each pkg below.
        HashMap<String, String> currentThemeMap = new HashMap<>();
        currentThemeMap.put(ThemesColumns.MODIFIES_STATUS_BAR, config.getOverlayForStatusBar());
        currentThemeMap.put(ThemesColumns.MODIFIES_NAVIGATION_BAR,
                config.getOverlayForNavBar());
        currentThemeMap.put(ThemesColumns.MODIFIES_OVERLAYS, config.getOverlayPkgName());

        // Look at each component's theme (that we care about at least) and check compatibility
        // of the pkg with the system. If it is not compatible then we will add it to a theme
        // change request.
        Map<String, String> defaults = ThemeUtils.getDefaultComponents(mContext);
        ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
        for(Map.Entry<String, String> entry : currentThemeMap.entrySet()) {
            String component = entry.getKey();
            String pkgName = entry.getValue();
            String defaultPkg = defaults.get(component);

            if (defaultPkg == null) {
                Log.d(TAG, "Default package is null, skipping " + component);
                continue;
            }

            // Check that the default overlay theme is not currently set
            if (defaultPkg.equals(pkgName)) {
                Log.d(TAG, "Current overlay theme is same as default. " +
                        "Not doing anything for " + component);
                continue;
            }

            // No need to unapply a system theme since it is always compatible
            if (ThemeConfig.SYSTEM_DEFAULT.equals(pkgName)) {
                Log.d(TAG, "Current overlay theme for "
                        + component + " was system. no need to unapply");
                continue;
            }

            if (!isThemeCompatibleWithUpgradedApi(pkgName)) {
                Log.d(TAG, pkgName + "is incompatible with latest theme api for component " +
                        component + ", Applying " + defaultPkg);
                builder.setComponent(component, pkgName);
            }
        }

        // Now actually unapply the incompatible themes
        ThemeChangeRequest request = builder.build();
        if (!request.getThemeComponentsMap().isEmpty()) {
            try {
                ((IThemeService) mService).requestThemeChange(request, true);
            } catch(RemoteException e) {
                // This cannot happen
            }
        } else {
            Log.d(TAG, "Current theme is compatible with the system. Not unapplying anything");
        }
    }

    private boolean isThemeCompatibleWithUpgradedApi(String pkgName) {
        // Note this function does not cover the case of a downgrade. That case is out of scope and
        // would require predicting whether the future API levels will be compatible or not.
        boolean compatible = false;
        try {
            PackageInfo pi = mPM.getPackageInfo(pkgName, 0);
            Log.d(TAG, "Comparing theme target: " + pi.applicationInfo.targetSdkVersion +
                    "to " + android.os.Build.VERSION.SDK_INT);
            compatible = pi.applicationInfo.targetSdkVersion >= MIN_COMPATIBLE_VERSION;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get package info for " + pkgName, e);
        }
        return compatible;
    }

    private boolean isThemeApiUpToDate() {
        // We can't be 100% sure its an upgrade. If the field is undefined it
        // could have been a factory reset.
        final ContentResolver resolver = mContext.getContentResolver();
        int recordedApiLevel = android.os.Build.VERSION.SDK_INT;
        try {
            recordedApiLevel = CMSettings.Secure.getInt(resolver,
                    CMSettings.Secure.THEME_PREV_BOOT_API_LEVEL);
        } catch (CMSettings.CMSettingNotFoundException e) {
            recordedApiLevel = -1;
            Log.d(TAG, "Previous api level not found. First time booting?");
        }
        Log.d(TAG, "Prev api level was: " + recordedApiLevel
                + ", api is now: " + android.os.Build.VERSION.SDK_INT);

        return recordedApiLevel == android.os.Build.VERSION.SDK_INT;
    }

    private void updateThemeApi() {
        final ContentResolver resolver = mContext.getContentResolver();
        boolean success = CMSettings.Secure.putInt(resolver,
                CMSettings.Secure.THEME_PREV_BOOT_API_LEVEL, android.os.Build.VERSION.SDK_INT);
        if (!success) {
            Log.e(TAG, "Unable to store latest API level to secure settings");
        }
    }

    private void doApplyTheme(ThemeChangeRequest request, boolean removePerAppTheme) {
        synchronized(this) {
            mProgress = 0;
        }

        if (request == null || request.getNumChangesRequested() == 0) {
            postFinish(true, request, 0);
            return;
        }
        mIsThemeApplying = true;
        mLastThemeChangeTime = System.currentTimeMillis();
        mLastThemeChangeRequestType = request.getReqeustType().ordinal();

        incrementProgress(5);

        // TODO: provide progress updates that reflect the time needed for each component
        final int progressIncrement = 75 / request.getNumChangesRequested();

        if (request.getIconsThemePackageName() != null) {
            updateIcons(request.getIconsThemePackageName());
            incrementProgress(progressIncrement);
        }

        if (request.getWallpaperThemePackageName() != null) {
            if (updateWallpaper(request.getWallpaperThemePackageName(),
                    request.getWallpaperId())) {
                mWallpaperChangedByUs = true;
            }
            incrementProgress(progressIncrement);
        }

        if (request.getLockWallpaperThemePackageName() != null) {
            updateLockscreen(request.getLockWallpaperThemePackageName());
            incrementProgress(progressIncrement);
        }

        Environment.setUserRequired(false);
        if (request.getNotificationThemePackageName() != null) {
            updateNotifications(request.getNotificationThemePackageName());
            incrementProgress(progressIncrement);
        }

        if (request.getAlarmThemePackageName() != null) {
            updateAlarms(request.getAlarmThemePackageName());
            incrementProgress(progressIncrement);
        }

        if (request.getRingtoneThemePackageName() != null) {
            updateRingtones(request.getRingtoneThemePackageName());
            incrementProgress(progressIncrement);
        }
        Environment.setUserRequired(true);

        if (request.getBootanimationThemePackageName() != null) {
            updateBootAnim(request.getBootanimationThemePackageName());
            incrementProgress(progressIncrement);
        }

        if (request.getFontThemePackageName() != null) {
            updateFonts(request.getFontThemePackageName());
            incrementProgress(progressIncrement);
        }

        if (request.getLiveLockScreenThemePackageName() != null) {
            updateLiveLockScreen(request.getLiveLockScreenThemePackageName());
            incrementProgress(progressIncrement);
        }

        try {
            updateProvider(request, mLastThemeChangeTime);
        } catch(IllegalArgumentException e) {
            // Safeguard against provider not being ready yet.
            Log.e(TAG, "Not updating the theme provider since it is unavailable");
        }

        if (shouldUpdateConfiguration(request)) {
            updateConfiguration(request, removePerAppTheme);
        }

        killLaunchers(request);

        postFinish(true, request, mLastThemeChangeTime);
        mIsThemeApplying = false;
    }

    private void doApplyDefaultTheme() {
        final ContentResolver resolver = mContext.getContentResolver();
        final String defaultThemePkg = ThemeUtils.getDefaultThemePackageName(mContext);
        if (!TextUtils.isEmpty(defaultThemePkg)) {
            String defaultThemeComponents = CMSettings.Secure.getString(resolver,
                    CMSettings.Secure.DEFAULT_THEME_COMPONENTS);
            List<String> components;
            if (TextUtils.isEmpty(defaultThemeComponents)) {
                components = ThemeUtils.getAllComponents();
            } else {
                components = new ArrayList<String>(
                        Arrays.asList(defaultThemeComponents.split("\\|")));
            }
            ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
            for (String component : components) {
                builder.setComponent(component, defaultThemePkg);
            }
            try {
                ((IThemeService) mService).requestThemeChange(builder.build(), true);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to set default theme", e);
            }
        }
    }

    private void doRebuildResourceCache() {
        FileUtils.deleteContents(new File(ThemeUtils.RESOURCE_CACHE_DIR));
        processInstalledThemes();
    }

    private void updateProvider(ThemeChangeRequest request, long updateTime) {
        ContentValues values = new ContentValues();
        values.put(MixnMatchColumns.COL_UPDATE_TIME, updateTime);
        Map<String, String> componentMap = request.getThemeComponentsMap();
        for (String component : componentMap.keySet()) {
            values.put(MixnMatchColumns.COL_VALUE, componentMap.get(component));
            String where = MixnMatchColumns.COL_KEY + "=?";
            String[] selectionArgs = { MixnMatchColumns.componentToMixNMatchKey(component) };
            if (selectionArgs[0] == null) {
                continue; // No equivalence between mixnmatch and theme
            }

            // Add component ID for multiwallpaper
            if (ThemesColumns.MODIFIES_LAUNCHER.equals(component)) {
                values.put(MixnMatchColumns.COL_COMPONENT_ID, request.getWallpaperId());
            }

            mContext.getContentResolver().update(MixnMatchColumns.CONTENT_URI, values, where,
                    selectionArgs);
        }
    }

    private boolean updateIcons(String pkgName) {
        ThemeUtils.clearIconCache();
        try {
            if (pkgName.equals(SYSTEM_DEFAULT)) {
                mPM.updateIconMaps(null);
            } else {
                mPM.updateIconMaps(pkgName);
            }
        } catch (Exception e) {
            Log.w(TAG, "Changing icons failed", e);
            return false;
        }
        return true;
    }

    private boolean updateFonts(String pkgName) {
        //Clear the font dir
        FileUtils.deleteContents(new File(ThemeUtils.SYSTEM_THEME_FONT_PATH));

        if (!pkgName.equals(SYSTEM_DEFAULT)) {
            //Get Font Assets
            Context themeCtx;
            String[] assetList;
            try {
                themeCtx = mContext.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY);
                AssetManager assetManager = themeCtx.getAssets();
                assetList = assetManager.list("fonts");
            } catch (Exception e) {
                Log.e(TAG, "There was an error getting assets  for pkg " + pkgName, e);
                return false;
            }
            if (assetList == null || assetList.length == 0) {
                Log.e(TAG, "Could not find any font assets");
                return false;
            }

            //Copy font assets to font dir
            for(String asset : assetList) {
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = ThemeUtils.getInputStreamFromAsset(themeCtx,
                            "file:///android_asset/fonts/" + asset);
                    File outFile = new File(ThemeUtils.SYSTEM_THEME_FONT_PATH, asset);
                    FileUtils.copyToFile(is, outFile);
                    FileUtils.setPermissions(outFile,
                            FileUtils.S_IRWXU|FileUtils.S_IRGRP|FileUtils.S_IRWXO, -1, -1);
                } catch (Exception e) {
                    Log.e(TAG, "There was an error installing the new fonts for pkg " + pkgName, e);
                    return false;
                } finally {
                    IoUtils.closeQuietly(is);
                    IoUtils.closeQuietly(os);
                }
            }
        }

        //Notify zygote that themes need a refresh
        SystemProperties.set("sys.refresh_theme", "1");
        return true;
    }

    private boolean updateBootAnim(String pkgName) {
        if (TextUtils.isEmpty(pkgName) || SYSTEM_DEFAULT.equals(pkgName)) {
            clearBootAnimation();
            return true;
        }

        try {
            final ApplicationInfo ai = mPM.getApplicationInfo(pkgName, 0);
            applyBootAnimation(ai.sourceDir);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Changing boot animation failed", e);
            return false;
        }
        return true;
    }

    private boolean updateAlarms(String pkgName) {
        return updateAudible(ThemeUtils.SYSTEM_THEME_ALARM_PATH, "alarms",
                RingtoneManager.TYPE_ALARM, pkgName);
    }

    private boolean updateNotifications(String pkgName) {
        return updateAudible(ThemeUtils.SYSTEM_THEME_NOTIFICATION_PATH, "notifications",
                RingtoneManager.TYPE_NOTIFICATION, pkgName);
    }

    private boolean updateRingtones(String pkgName) {
        return updateAudible(ThemeUtils.SYSTEM_THEME_RINGTONE_PATH, "ringtones",
                RingtoneManager.TYPE_RINGTONE, pkgName);
    }

    private boolean updateAudible(String dirPath, String assetPath, int type, String pkgName) {
        //Clear the dir
        ThemeUtils.clearAudibles(mContext, dirPath);
        if (TextUtils.isEmpty(pkgName) || pkgName.equals(SYSTEM_DEFAULT)) {
            if (!ThemeUtils.setDefaultAudible(mContext, type)) {
                Log.e(TAG, "There was an error installing the default audio file");
                return false;
            }
            return true;
        }

        PackageInfo pi = null;
        try {
            pi = mPM.getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to update audible " + dirPath, e);
            return false;
        }

        //Get theme Assets
        Context themeCtx;
        String[] assetList;
        try {
            themeCtx = mContext.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY);
            AssetManager assetManager = themeCtx.getAssets();
            assetList = assetManager.list(assetPath);
        } catch (Exception e) {
            Log.e(TAG, "There was an error getting assets for pkg " + pkgName, e);
            return false;
        }
        if (assetList == null || assetList.length == 0) {
            Log.e(TAG, "Could not find any audio assets");
            return false;
        }

        // TODO: right now we just load the first file but this will need to be changed
        // in the future if multiple audio files are supported.
        final String asset = assetList[0];
        if (!ThemeUtils.isValidAudible(asset)) return false;

        InputStream is = null;
        OutputStream os = null;
        try {
            is = ThemeUtils.getInputStreamFromAsset(themeCtx, "file:///android_asset/"
                    + assetPath + File.separator + asset);
            File outFile = new File(dirPath, asset);
            FileUtils.copyToFile(is, outFile);
            FileUtils.setPermissions(outFile,
                    FileUtils.S_IRWXU|FileUtils.S_IRGRP|FileUtils.S_IRWXO,-1, -1);
            ThemeUtils.setAudible(mContext, outFile, type, pi.themeInfo.name);
        } catch (Exception e) {
            Log.e(TAG, "There was an error installing the new audio file for pkg " + pkgName, e);
            return false;
        } finally {
            IoUtils.closeQuietly(is);
            IoUtils.closeQuietly(os);
        }
        return true;
    }

    private boolean updateLockscreen(String pkgName) {
        boolean success;
        success = setCustomLockScreenWallpaper(pkgName);

        if (success) {
            mContext.sendBroadcastAsUser(new Intent(Intent.ACTION_KEYGUARD_WALLPAPER_CHANGED),
                    UserHandle.ALL);
        }
        return success;
    }

    private boolean setCustomLockScreenWallpaper(String pkgName) {
        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        try {
            if (SYSTEM_DEFAULT.equals(pkgName) || TextUtils.isEmpty(pkgName)) {
                wm.clearKeyguardWallpaper();
            } else {
                InputStream in = ImageUtils.getCroppedKeyguardStream(pkgName, mContext);
                if (in != null) {
                    wm.setKeyguardStream(in);
                    IoUtils.closeQuietly(in);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "There was an error setting lockscreen wp for pkg " + pkgName, e);
            return false;
        }
        return true;
    }

    private boolean updateWallpaper(String pkgName, long id) {
        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        if (SYSTEM_DEFAULT.equals(pkgName)) {
            try {
                wm.clear();
            } catch (IOException e) {
                return false;
            }
        } else if (TextUtils.isEmpty(pkgName)) {
            try {
                wm.clear(false);
            } catch (IOException e) {
                return false;
            }
        } else {
            InputStream in = null;
            try {
                in = ImageUtils.getCroppedWallpaperStream(pkgName, id, mContext);
                if (in != null)
                    wm.setStream(in);
            } catch (Exception e) {
                return false;
            } finally {
                IoUtils.closeQuietly(in);
            }
        }
        return true;
    }

    private boolean updateLiveLockScreen(String pkgName) {
        // TODO: do something meaningful here once ready
        return true;
    }

    private boolean updateConfiguration(ThemeChangeRequest request,
            boolean removePerAppThemes) {
        final IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            final long token = Binder.clearCallingIdentity();
            try {
                Configuration config = am.getConfiguration();
                ThemeConfig.Builder themeBuilder = createBuilderFrom(config, request, null,
                        removePerAppThemes);
                ThemeConfig newConfig = themeBuilder.build();

                config.themeConfig = newConfig;
                am.updateConfiguration(config);
            } catch (RemoteException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return true;
    }

    private boolean updateConfiguration(ThemeConfig themeConfig) {
        final IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            final long token = Binder.clearCallingIdentity();
            try {
                Configuration config = am.getConfiguration();

                config.themeConfig = themeConfig;
                am.updateConfiguration(config);
            } catch (RemoteException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return true;
    }

    private boolean shouldUpdateConfiguration(ThemeChangeRequest request) {
        return request.getOverlayThemePackageName() != null ||
                request.getFontThemePackageName() != null ||
                request.getIconsThemePackageName() != null ||
                request.getStatusBarThemePackageName() != null ||
                request.getNavBarThemePackageName() != null ||
                request.getPerAppOverlays().size() > 0;
    }

    private static ThemeConfig.Builder createBuilderFrom(Configuration config,
            ThemeChangeRequest request, String pkgName, boolean removePerAppThemes) {
        ThemeConfig.Builder builder = new ThemeConfig.Builder(config.themeConfig);

        if (removePerAppThemes) removePerAppThemesFromConfig(builder, config.themeConfig);

        if (request.getIconsThemePackageName() != null) {
            builder.defaultIcon(pkgName == null ? request.getIconsThemePackageName() : pkgName);
        }

        if (request.getOverlayThemePackageName() != null) {
            builder.defaultOverlay(pkgName == null ?
                    request.getOverlayThemePackageName() : pkgName);
        }

        if (request.getFontThemePackageName() != null) {
            builder.defaultFont(pkgName == null ? request.getFontThemePackageName() : pkgName);
        }

        if (request.getStatusBarThemePackageName() != null) {
            builder.overlay(ThemeConfig.SYSTEMUI_STATUS_BAR_PKG, pkgName == null ?
                    request.getStatusBarThemePackageName() : pkgName);
        }

        if (request.getNavBarThemePackageName() != null) {
            builder.overlay(ThemeConfig.SYSTEMUI_NAVBAR_PKG, pkgName == null ?
                    request.getNavBarThemePackageName() : pkgName);
        }

        // check for any per app overlays being applied
        Map<String, String> appOverlays = request.getPerAppOverlays();
        for (String appPkgName : appOverlays.keySet()) {
            if (appPkgName != null) {
                builder.overlay(appPkgName, appOverlays.get(appPkgName));
            }
        }

        return builder;
    }

    private static void removePerAppThemesFromConfig(ThemeConfig.Builder builder,
            ThemeConfig themeConfig) {
        if (themeConfig != null) {
            Map<String, ThemeConfig.AppTheme> themes = themeConfig.getAppThemes();
            for (String appPkgName : themes.keySet()) {
                if (ThemeUtils.isPerAppThemeComponent(appPkgName)) {
                    builder.overlay(appPkgName, null);
                }
            }
        }
    }

    // Kill the current Home process, they tend to be evil and cache
    // drawable references in all apps
    private void killLaunchers(ThemeChangeRequest request) {
        if (request.getOverlayThemePackageName() == null
                && request.getIconsThemePackageName() == null) {
            return;
        }

        final ActivityManager am =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        Intent homeIntent = new Intent();
        homeIntent.setAction(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        List<ResolveInfo> infos = mPM.queryIntentActivities(homeIntent, 0);
        List<ResolveInfo> themeChangeInfos = mPM.queryBroadcastReceivers(
                new Intent(ThemeUtils.ACTION_THEME_CHANGED), 0);
        for(ResolveInfo info : infos) {
            if (info.activityInfo != null && info.activityInfo.applicationInfo != null &&
                    !isSetupActivity(info) && !handlesThemeChanges(
                    info.activityInfo.applicationInfo.packageName, themeChangeInfos)) {
                String pkgToStop = info.activityInfo.applicationInfo.packageName;
                Log.d(TAG, "Force stopping " +  pkgToStop + " for theme change");
                try {
                    am.forceStopPackage(pkgToStop);
                } catch(Exception e) {
                    Log.e(TAG, "Unable to force stop package, did you forget platform signature?",
                            e);
                }
            }
        }
    }

    private boolean isSetupActivity(ResolveInfo info) {
        return GOOGLE_SETUPWIZARD_PACKAGE.equals(info.activityInfo.packageName) ||
                MANAGED_PROVISIONING_PACKAGE.equals(info.activityInfo.packageName) ||
                CM_SETUPWIZARD_PACKAGE.equals(info.activityInfo.packageName);
    }

    private boolean handlesThemeChanges(String pkgName, List<ResolveInfo> infos) {
        if (infos != null && infos.size() > 0) {
            for (ResolveInfo info : infos) {
                if (info.activityInfo.applicationInfo.packageName.equals(pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void postProgress() {
        int N = mClients.beginBroadcast();
        for(int i=0; i < N; i++) {
            IThemeChangeListener listener = mClients.getBroadcastItem(0);
            try {
                listener.onProgress(mProgress);
            } catch(RemoteException e) {
                Log.w(TAG, "Unable to post progress to client listener", e);
            }
        }
        mClients.finishBroadcast();
    }

    private void postFinish(boolean isSuccess, ThemeChangeRequest request, long updateTime) {
        synchronized(this) {
            mProgress = 0;
        }

        int N = mClients.beginBroadcast();
        for(int i=0; i < N; i++) {
            IThemeChangeListener listener = mClients.getBroadcastItem(0);
            try {
                listener.onFinish(isSuccess);
            } catch(RemoteException e) {
                Log.w(TAG, "Unable to post progress to client listener", e);
            }
        }
        mClients.finishBroadcast();

        // if successful, broadcast that the theme changed
        if (isSuccess) {
            broadcastThemeChange(request, updateTime);
        }
    }

    private void postFinishedProcessing(String pkgName) {
        int N = mProcessingListeners.beginBroadcast();
        for(int i=0; i < N; i++) {
            IThemeProcessingListener listener = mProcessingListeners.getBroadcastItem(0);
            try {
                listener.onFinishedProcessing(pkgName);
            } catch(RemoteException e) {
                Log.w(TAG, "Unable to post progress to listener", e);
            }
        }
        mProcessingListeners.finishBroadcast();
    }

    private void broadcastThemeChange(ThemeChangeRequest request, long updateTime) {
        Map<String, String> componentMap = request.getThemeComponentsMap();
        if (componentMap == null || componentMap.size() == 0) return;

        final Intent intent = new Intent(ThemeUtils.ACTION_THEME_CHANGED);
        ArrayList componentsArrayList = new ArrayList(componentMap.keySet());
        intent.putStringArrayListExtra(ThemeUtils.EXTRA_COMPONENTS, componentsArrayList);
        intent.putExtra(ThemeUtils.EXTRA_REQUEST_TYPE, request.getReqeustType().ordinal());
        intent.putExtra(ThemeUtils.EXTRA_UPDATE_TIME, updateTime);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void incrementProgress(int increment) {
        synchronized(this) {
            mProgress += increment;
            if (mProgress > 100) mProgress = 100;
        }
        postProgress();
    }

    private boolean applyBootAnimation(String themePath) {
        boolean success = false;
        try {
            ZipFile zip = new ZipFile(new File(themePath));
            ZipEntry ze = zip.getEntry(THEME_BOOTANIMATION_PATH);
            if (ze != null) {
                clearBootAnimation();
                BufferedInputStream is = new BufferedInputStream(zip.getInputStream(ze));
                final String bootAnimationPath = SYSTEM_THEME_PATH + File.separator
                        + "bootanimation.zip";
                ThemeUtils.copyAndScaleBootAnimation(mContext, is, bootAnimationPath);
                FileUtils.setPermissions(bootAnimationPath,
                        FileUtils.S_IRWXU|FileUtils.S_IRGRP|FileUtils.S_IROTH, -1, -1);
            }
            zip.close();
            success = true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to load boot animation for " + themePath, e);
        }

        return success;
    }

    private void clearBootAnimation() {
        File anim = new File(SYSTEM_THEME_PATH + File.separator + "bootanimation.zip");
        if (anim.exists())
            anim.delete();
    }

    private BroadcastReceiver mWallpaperChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mWallpaperChangedByUs) {
                // In case the mixnmatch table has a mods_launcher entry, we'll clear it
                ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
                builder.setWallpaper("");
                updateProvider(builder.build(), System.currentTimeMillis());
            } else {
                mWallpaperChangedByUs = false;
            }
        }
    };

    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userHandle = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
            if (userHandle >= 0 && userHandle != mCurrentUserId) {
                mCurrentUserId = userHandle;
                ThemeConfig config = ThemeConfig.getBootThemeForUser(mContext.getContentResolver(),
                        userHandle);
                if (DEBUG) {
                    Log.d(TAG,
                            "Changing theme for user " + userHandle + " to " + config.toString());
                }
                ThemeChangeRequest request = new ThemeChangeRequest.Builder(config).build();
                try {
                    ((IThemeService) mService).requestThemeChange(request, true);
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to change theme for user change", e);
                }
            }
        }
    };

    private void processInstalledThemes() {
        final String defaultTheme = getDefaultThemePackageName(mContext);
        Message msg;
        // Make sure the default theme is the first to get processed!
        if (!ThemeConfig.SYSTEM_DEFAULT.equals(defaultTheme)) {
            msg = mHandler.obtainMessage(
                    ResourceProcessingHandler.MESSAGE_QUEUE_THEME_FOR_PROCESSING,
                    0, 0, defaultTheme);
            mResourceProcessingHandler.sendMessage(msg);
        }
        // Iterate over all installed packages and queue up the ones that are themes or icon packs
        List<PackageInfo> packages = mPM.getInstalledPackages(0);
        for (PackageInfo info : packages) {
            if (!defaultTheme.equals(info.packageName) &&
                    (info.isThemeApk || info.isLegacyIconPackApk)) {
                msg = mHandler.obtainMessage(
                        ResourceProcessingHandler.MESSAGE_QUEUE_THEME_FOR_PROCESSING,
                        0, 0, info.packageName);
                mResourceProcessingHandler.sendMessage(msg);
            }
        }
    }

    private void sendThemeResourcesCachedBroadcast(String themePkgName, int resultCode) {
        final Intent intent = new Intent(Intent.ACTION_THEME_RESOURCES_CACHED);
        intent.putExtra(Intent.EXTRA_THEME_PACKAGE_NAME, themePkgName);
        intent.putExtra(Intent.EXTRA_THEME_RESULT, resultCode);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Posts a notification to let the user know the theme was not installed.
     * @param name
     */
    private void postFailedThemeInstallNotification(String name) {
        NotificationManager nm =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notice = new Notification.Builder(mContext)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentTitle(
                        mContext.getString(R.string.theme_install_error_title))
                .setContentText(String.format(
                        mContext.getString(R.string.theme_install_error_message), name))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setWhen(System.currentTimeMillis())
                .build();
        nm.notify(name.hashCode(), notice);
    }

    private String getThemeName(PackageInfo pi) {
        if (pi.themeInfo != null) {
            return pi.themeInfo.name;
        } else if (pi.isLegacyIconPackApk) {
            return pi.applicationInfo.name;
        }

        return null;
    }

    /**
     * Get the default theme package name
     * Historically this was done using {@link ThemeUtils#getDefaultThemePackageName(Context)} but
     * the setting that is queried in that method uses the AOSP settings provider but the setting
     * is now in CMSettings.  Since {@link ThemeUtils} is in the core framework we cannot access
     * CMSettings.
     * @param context
     * @return Default theme package name
     */
    private static String getDefaultThemePackageName(Context context) {
        final String defaultThemePkg = CMSettings.Secure.getString(context.getContentResolver(),
                CMSettings.Secure.DEFAULT_THEME_PACKAGE);
        if (!TextUtils.isEmpty(defaultThemePkg)) {
            PackageManager pm = context.getPackageManager();
            try {
                if (pm.getPackageInfo(defaultThemePkg, 0) != null) {
                    return defaultThemePkg;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // doesn't exist so system will be default
                Log.w(TAG, "Default theme " + defaultThemePkg + " not found", e);
            }
        }

        return SYSTEM_DEFAULT;
    }

    private void publishThemesTile() {
        // This action should be performed as system
        final int userId = UserHandle.myUserId();
        long token = Binder.clearCallingIdentity();
        try {
            final UserHandle user = new UserHandle(userId);
            final Context resourceContext = QSUtils.getQSTileContext(mContext, userId);

            CMStatusBarManager statusBarManager = CMStatusBarManager.getInstance(mContext);
            final PendingIntent chooserIntent = getThemeChooserPendingIntent();
            CustomTile tile = new CustomTile.Builder(resourceContext)
                    .setLabel(R.string.qs_themes_label)
                    .setContentDescription(R.string.qs_themes_content_description)
                    .setIcon(R.drawable.ic_qs_themes)
                    .setOnClickIntent(chooserIntent)
                    .setOnLongClickIntent(chooserIntent)
                    .shouldCollapsePanel(true)
                    .build();
            statusBarManager.publishTileAsUser(QSConstants.DYNAMIC_TILE_THEMES,
                    ThemeManagerService.class.hashCode(), tile, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private PendingIntent getThemeChooserPendingIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(CATEGORY_THEME_CHOOSER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(mContext, ThemeManagerService.class.hashCode(),
                intent, 0);
    }

    private final IBinder mService = new IThemeService.Stub() {
        @Override
        public void requestThemeChangeUpdates(IThemeChangeListener listener)
                throws RemoteException {
            enforcePermission();
            mClients.register(listener);
        }

        @Override
        public void removeUpdates(IThemeChangeListener listener) throws RemoteException {
            enforcePermission();
            mClients.unregister(listener);
        }

        @Override
        public void requestThemeChange(ThemeChangeRequest request, boolean removePerAppThemes)
                throws RemoteException {
            enforcePermission();
            Message msg;

            /**
             * Since the ThemeService handles compiling theme resource we need to make sure that any
             * of the components we are trying to apply are either already processed or put to the
             * front of the queue and handled before the theme change takes place.
             *
             * TODO: create a callback that can be sent to any ThemeChangeListeners to notify them
             * that the theme will be applied once the processing is done.
             */
            synchronized (mThemesToProcessQueue) {
                Map<String, String> componentMap = request.getThemeComponentsMap();
                for (Object key : componentMap.keySet()) {
                    if (ThemesColumns.MODIFIES_OVERLAYS.equals(key) ||
                            ThemesColumns.MODIFIES_NAVIGATION_BAR.equals(key) ||
                            ThemesColumns.MODIFIES_STATUS_BAR.equals(key) ||
                            ThemesColumns.MODIFIES_ICONS.equals(key)) {
                        String pkgName = componentMap.get(key);
                        if (mThemesToProcessQueue.indexOf(pkgName) > 0) {
                            mThemesToProcessQueue.remove(pkgName);
                            mThemesToProcessQueue.add(0, pkgName);
                            // We want to make sure these resources are taken care of first so
                            // send the dequeue message and place it in the front of the queue
                            msg = mResourceProcessingHandler.obtainMessage(
                                    ResourceProcessingHandler.MESSAGE_DEQUEUE_AND_PROCESS_THEME);
                            mResourceProcessingHandler.sendMessageAtFrontOfQueue(msg);
                        }
                    }
                }
            }
            msg = Message.obtain();
            msg.what = ThemeWorkerHandler.MESSAGE_CHANGE_THEME;
            msg.obj = request;
            msg.arg1 = removePerAppThemes ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        @Override
        public void applyDefaultTheme() {
            enforcePermission();
            Message msg = Message.obtain();
            msg.what = ThemeWorkerHandler.MESSAGE_APPLY_DEFAULT_THEME;
            mHandler.sendMessage(msg);
        }

        @Override
        public boolean isThemeApplying() throws RemoteException {
            enforcePermission();
            return mIsThemeApplying;
        }

        @Override
        public int getProgress() throws RemoteException {
            enforcePermission();
            synchronized(this) {
                return mProgress;
            }
        }

        @Override
        public boolean processThemeResources(String themePkgName) throws RemoteException {
            enforcePermission();
            try {
                mPM.getPackageInfo(themePkgName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Package doesn't exist so nothing to process
                return false;
            }
            // Obtain a message and send it to the handler to process this theme
            Message msg = mResourceProcessingHandler.obtainMessage(
                    ResourceProcessingHandler.MESSAGE_QUEUE_THEME_FOR_PROCESSING, 0, 0,
                    themePkgName);
            mResourceProcessingHandler.sendMessage(msg);
            return true;
        }

        @Override
        public boolean isThemeBeingProcessed(String themePkgName) throws RemoteException {
            enforcePermission();
            synchronized (mThemesToProcessQueue) {
                return mThemesToProcessQueue.contains(themePkgName);
            }
        }

        @Override
        public void registerThemeProcessingListener(IThemeProcessingListener listener)
                throws RemoteException {
            enforcePermission();
            mProcessingListeners.register(listener);
        }

        @Override
        public void unregisterThemeProcessingListener(IThemeProcessingListener listener)
                throws RemoteException {
            enforcePermission();
            mProcessingListeners.unregister(listener);
        }

        @Override
        public void rebuildResourceCache() throws RemoteException {
            enforcePermission();
            mHandler.sendEmptyMessage(ThemeWorkerHandler.MESSAGE_REBUILD_RESOURCE_CACHE);
        }

        @Override
        public long getLastThemeChangeTime() {
            return mLastThemeChangeTime;
        }

        @Override
        public int getLastThemeChangeRequestType() {
            return mLastThemeChangeRequestType;
        }

        private void enforcePermission() {
            mContext.enforceCallingOrSelfPermission(ACCESS_THEME_MANAGER, null);
        }
    };
}
