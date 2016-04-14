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

import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_MANAGED_OUTDOOR_MODE;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_DAY;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_FIRST;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_LAST;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_OUTDOOR;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;

import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.pm.UserContentObserver;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;

import org.cyanogenmod.internal.util.QSConstants;
import org.cyanogenmod.internal.util.QSUtils;
import org.cyanogenmod.platform.internal.R;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;
import cyanogenmod.hardware.ILiveDisplayService;
import cyanogenmod.hardware.LiveDisplayConfig;
import cyanogenmod.providers.CMSettings;

/**
 * LiveDisplay is an advanced set of features for improving
 * display quality under various ambient conditions.
 *
 * The service is constructed with a set of LiveDisplayFeatures
 * which provide capabilities such as outdoor mode, night mode,
 * and calibration. It interacts with CMHardwareService to relay
 * changes down to the lower layers.
 */
public class LiveDisplayService extends SystemService {

    private static final String TAG = "LiveDisplay";

    private static final int MSG_MODE_CHANGED = 1;
    private static final int MSG_DISPLAY_CHANGED = 2;
    private static final int MSG_LOW_POWER_MODE_CHANGED = 3;
    private static final int MSG_TWILIGHT_UPDATE = 4;

    private final Context mContext;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;

    private DisplayManager mDisplayManager;
    private ModeObserver mModeObserver;
    private TwilightManager mTwilightManager;

    private boolean mInitialized = false;
    private boolean mAwaitingNudge = true;
    private boolean mSunset = false;

    private boolean mLowPowerMode;
    private int mDisplayState = -1;

    private final List<LiveDisplayFeature> mFeatures = new ArrayList<LiveDisplayFeature>();

    private ColorTemperatureController mCTC;
    private DisplayHardwareController mDHC;
    private OutdoorModeController mOMC;

    private LiveDisplayConfig mConfig;

    // QS tile
    private String[] mTileEntries;
    private String[] mTileDescriptionEntries;
    private String[] mTileAnnouncementEntries;
    private String[] mTileValues;
    private int[] mTileEntryIconRes;

    private static String ACTION_NEXT_MODE = "cyanogenmod.hardware.NEXT_LIVEDISPLAY_MODE";
    private static String EXTRA_NEXT_MODE = "next_mode";

    public LiveDisplayService(Context context) {
        super(context);

        mContext = context;

        // We want a slightly higher priority thread to handle these requests
        mHandlerThread = new ServiceThread(TAG,
                Process.THREAD_PRIORITY_DISPLAY + 1, false /*allowIo*/);
        mHandlerThread.start();
        mHandler = new LiveDisplayHandler(mHandlerThread.getLooper());

        updateCustomTileEntries();
    }

    @Override
    public void onStart() {
        if (mContext.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.LIVEDISPLAY)) {
            publishBinderService(CMContextConstants.CM_LIVEDISPLAY_SERVICE, mBinder);
        } else {
            Log.wtf(TAG, "CM LiveDisplay service started by system server but feature xml not" +
                    " declared. Not publishing binder service!");
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {

            mAwaitingNudge = getSunsetCounter() < 1;

            mDHC = new DisplayHardwareController(mContext, mHandler);
            mFeatures.add(mDHC);

            mCTC = new ColorTemperatureController(mContext, mHandler, mDHC);
            mFeatures.add(mCTC);

            mOMC = new OutdoorModeController(mContext, mHandler);
            mFeatures.add(mOMC);

            // Call onStart of each feature and get it's capabilities
            final BitSet capabilities = new BitSet();
            for (Iterator<LiveDisplayFeature> it = mFeatures.iterator(); it.hasNext();) {
                final LiveDisplayFeature feature = it.next();
                if (feature.onStart()) {
                    feature.getCapabilities(capabilities);
                } else {
                    it.remove();
                }
            }

            int defaultMode = mContext.getResources().getInteger(
                    org.cyanogenmod.platform.internal.R.integer.config_defaultLiveDisplayMode);

            mConfig = new LiveDisplayConfig(capabilities, defaultMode,
                    mCTC.getDefaultDayTemperature(), mCTC.getDefaultNightTemperature(),
                    mOMC.getDefaultAutoOutdoorMode(), mDHC.getDefaultAutoContrast(),
                    mDHC.getDefaultCABC(), mDHC.getDefaultColorEnhancement());

            mDisplayManager = (DisplayManager) getContext().getSystemService(
                    Context.DISPLAY_SERVICE);
            mDisplayManager.registerDisplayListener(mDisplayListener, null);

            PowerManagerInternal pmi = LocalServices.getService(PowerManagerInternal.class);
            pmi.registerLowPowerModeObserver(mLowPowerModeListener);

            mTwilightManager = LocalServices.getService(TwilightManager.class);
            mTwilightManager.registerListener(mTwilightListener, mHandler);
            updateTwilight();

            updateDisplayState(mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY).getState());

            mModeObserver = new ModeObserver(mHandler);
            mModeObserver.update();

            mContext.registerReceiver(mNextModeReceiver,
                    new IntentFilter(ACTION_NEXT_MODE));
            publishCustomTile();

            mInitialized = true;
        }
    }

    private void updateCustomTileEntries() {
        Resources res = mContext.getResources();
        mTileEntries = res.getStringArray(R.array.live_display_entries);
        mTileDescriptionEntries = res.getStringArray(R.array.live_display_description);
        mTileAnnouncementEntries = res.getStringArray(R.array.live_display_announcement);
        mTileValues = res.getStringArray(R.array.live_display_values);
        TypedArray typedArray = res.obtainTypedArray(R.array.live_display_drawables);
        mTileEntryIconRes = new int[typedArray.length()];
        for (int i = 0; i < mTileEntryIconRes.length; i++) {
            mTileEntryIconRes[i] = typedArray.getResourceId(i, 0);
        }
        typedArray.recycle();
    }

    private int getCurrentModeIndex() {
        return ArrayUtils.indexOf(mTileValues, String.valueOf(mModeObserver.getMode()));
    }

    private int getNextModeIndex() {
        int next = getCurrentModeIndex() + 1;

        if (next >= mTileValues.length) {
            next = 0;
        }

        int nextMode = 0;

        while (true) {
            nextMode = Integer.valueOf(mTileValues[next]);
            // Skip outdoor mode if it's unsupported, and skip the day setting
            // if it's the same as the off setting
            if  (((!mConfig.hasFeature(MODE_OUTDOOR) ||
                    mConfig.hasFeature(FEATURE_MANAGED_OUTDOOR_MODE)
                            && nextMode == MODE_OUTDOOR)) ||
                    (mCTC.getDayColorTemperature() == mConfig.getDefaultDayTemperature()
                            && nextMode == MODE_DAY)) {
                next++;
                if (next >= mTileValues.length) {
                    next = 0;
                }
            } else {
                break;
            }
        }

        return nextMode;
    }

    private void publishCustomTile() {
        // This action should be performed as system
        final int userId = UserHandle.myUserId();
        long token = Binder.clearCallingIdentity();
        try {
            int idx = getCurrentModeIndex();
            final UserHandle user = new UserHandle(userId);
            final Context resourceContext = QSUtils.getQSTileContext(mContext, userId);

            CMStatusBarManager statusBarManager = CMStatusBarManager.getInstance(mContext);
            CustomTile tile = new CustomTile.Builder(resourceContext)
                    .setLabel(mTileEntries[idx])
                    .setContentDescription(mTileDescriptionEntries[idx])
                    .setIcon(mTileEntryIconRes[idx])
                    .setOnLongClickIntent(getCustomTileLongClickPendingIntent())
                    .setOnClickIntent(getCustomTileNextModePendingIntent())
                    .shouldCollapsePanel(false)
                    .build();
            statusBarManager.publishTileAsUser(QSConstants.TILE_LIVE_DISPLAY,
                    LiveDisplayService.class.hashCode(), tile, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void unpublishCustomTile() {
        // This action should be performed as system
        final int userId = UserHandle.myUserId();
        long token = Binder.clearCallingIdentity();
        try {
            CMStatusBarManager statusBarManager = CMStatusBarManager.getInstance(mContext);
            statusBarManager.removeTileAsUser(QSConstants.TILE_LIVE_DISPLAY,
                    LiveDisplayService.class.hashCode(), new UserHandle(userId));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private PendingIntent getCustomTileNextModePendingIntent() {
        Intent i = new Intent(ACTION_NEXT_MODE);
        i.putExtra(EXTRA_NEXT_MODE, getNextModeIndex());
        return PendingIntent.getBroadcastAsUser(mContext, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT, UserHandle.CURRENT);
    }

    private PendingIntent getCustomTileLongClickPendingIntent() {
        Intent i = new Intent(CMSettings.ACTION_LIVEDISPLAY_SETTINGS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivityAsUser(mContext, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT, null, UserHandle.CURRENT);
    }

    private final BroadcastReceiver mNextModeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra(EXTRA_NEXT_MODE, mConfig.getDefaultMode());
            if (mConfig.hasFeature(mode) && mode >= MODE_FIRST && mode <= MODE_LAST) {
                putInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE, mode);
            }
        }
    };

    private final IBinder mBinder = new ILiveDisplayService.Stub() {

        @Override
        public LiveDisplayConfig getConfig() {
            return mConfig;
        }

        @Override
        public int getMode() {
            return mModeObserver.getMode();
        }

        @Override
        public boolean setMode(int mode) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            if (mConfig.hasFeature(mode) && mode >= MODE_FIRST && mode <= MODE_LAST) {
                putInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE, mode);
                return true;
            }
            return false;
        }

        @Override
        public float[] getColorAdjustment() {
            return mDHC.getColorAdjustment();
        }

        @Override
        public boolean setColorAdjustment(float[] adj) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            return mDHC.setColorAdjustment(adj);
        }

        @Override
        public boolean isAutoContrastEnabled() {
            return mDHC.isAutoContrastEnabled();
        }

        @Override
        public  boolean setAutoContrastEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            return mDHC.setAutoContrastEnabled(enabled);
        }

        @Override
        public boolean isCABCEnabled() {
            return mDHC.isCABCEnabled();
        }

        @Override
        public boolean setCABCEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            return mDHC.setCABCEnabled(enabled);
        }

        @Override
        public boolean isColorEnhancementEnabled() {
            return mDHC.isColorEnhancementEnabled();
        }

        @Override
        public boolean setColorEnhancementEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            return mDHC.setColorEnhancementEnabled(enabled);
        }

        @Override
        public boolean isAutomaticOutdoorModeEnabled() {
            return mOMC.isAutomaticOutdoorModeEnabled();
        }

        @Override
        public boolean setAutomaticOutdoorModeEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            return mOMC.setAutomaticOutdoorModeEnabled(enabled);
        }

        @Override
        public int getDayColorTemperature() {
            return mCTC.getDayColorTemperature();
        }

        @Override
        public boolean setDayColorTemperature(int temperature) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            mCTC.setDayColorTemperature(temperature);
            return true;
        }

        @Override
        public int getNightColorTemperature() {
            return mCTC.getNightColorTemperature();
        }

        @Override
        public boolean setNightColorTemperature(int temperature) {
            mContext.enforceCallingOrSelfPermission(
                    cyanogenmod.platform.Manifest.permission.MANAGE_LIVEDISPLAY, null);
            mCTC.setNightColorTemperature(temperature);
            return true;
        }

        @Override
        public int getColorTemperature() {
            return mCTC.getColorTemperature();
        }

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println();
            pw.println("LiveDisplay Service State:");
            pw.println("  mMode=" + mModeObserver.getMode());
            pw.println("  mDisplayState=" + mDisplayState);
            pw.println("  mAwaitingNudge=" + mAwaitingNudge);
            pw.println("  mConfig=" + mConfig.toString());

            for (int i = 0; i < mFeatures.size(); i++) {
                mFeatures.get(i).dump(pw);
            }
        }
    };

    // Listener for screen on/off events
    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                mHandler.obtainMessage(MSG_DISPLAY_CHANGED,
                        mDisplayManager.getDisplay(displayId).getState(), 0).sendToTarget();
            }
        }
    };


    // Display postprocessing can have power impact.
    private PowerManagerInternal.LowPowerModeListener mLowPowerModeListener =
            new PowerManagerInternal.LowPowerModeListener() {
        @Override
        public void onLowPowerModeChanged(boolean lowPowerMode) {
            if (lowPowerMode != mLowPowerMode) {
                mLowPowerMode = lowPowerMode;
                mHandler.obtainMessage(MSG_LOW_POWER_MODE_CHANGED,
                        (lowPowerMode ? 1 : 0), 0).sendToTarget();
            }
         }
    };

    // Watch for mode changes
    private final class ModeObserver extends UserContentObserver {

        private final Uri MODE_SETTING =
                CMSettings.System.getUriFor(CMSettings.System.DISPLAY_TEMPERATURE_MODE);

        ModeObserver(Handler handler) {
            super(handler);

            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(MODE_SETTING, false, this, UserHandle.USER_ALL);

            observe();
        }

        @Override
        protected void update() {
            mHandler.obtainMessage(MSG_MODE_CHANGED, getMode(), 0).sendToTarget();
            publishCustomTile();
        }

        int getMode() {
            return getInt(CMSettings.System.DISPLAY_TEMPERATURE_MODE,
                    mConfig.getDefaultMode());
        }
    }

    // Night watchman
    private final TwilightListener mTwilightListener = new TwilightListener() {
        @Override
        public void onTwilightStateChanged() {
            mHandler.obtainMessage(MSG_TWILIGHT_UPDATE,
                    mTwilightManager.getCurrentState()).sendToTarget();
        }
    };

    private int getSunsetCounter() {
        // Counter used to determine when we should tell the user about this feature.
        // If it's not used after 3 sunsets, we'll show the hint once.
        return CMSettings.System.getIntForUser(mContext.getContentResolver(),
                CMSettings.System.LIVE_DISPLAY_HINTED,
                -3,
                UserHandle.USER_CURRENT);
    }


    private void updateSunsetCounter(int count) {
        CMSettings.System.putIntForUser(mContext.getContentResolver(),
                CMSettings.System.LIVE_DISPLAY_HINTED,
                count,
                UserHandle.USER_CURRENT);
        mAwaitingNudge = count > 0;
    }

    private void stopNudgingMe() {
        if (mAwaitingNudge) {
            updateSunsetCounter(1);
        }
    }

    /**
     * Show a friendly notification to the user about the potential benefits of decreasing
     * blue light at night. Do this only once if the feature has not been used after
     * three sunsets. It would be great to enable this by default, but we don't want
     * the change of screen color to be considered a "bug" by a user who doesn't
     * understand what's happening.
     *
     * @param state
     */
    private void nudge() {
        final TwilightState twilight = mTwilightManager.getCurrentState();
        if (!mAwaitingNudge || twilight == null) {
            return;
        }

        int counter = getSunsetCounter();

        // check if we should send the hint only once after sunset
        boolean transition = twilight.isNight() && !mSunset;
        mSunset = twilight.isNight();
        if (!transition) {
            return;
        }

        if (counter <= 0) {
            counter++;
            updateSunsetCounter(counter);
        }
        if (counter == 0) {
            //show the notification and don't come back here
            final Intent intent = new Intent(CMSettings.ACTION_LIVEDISPLAY_SETTINGS);
            PendingIntent result = PendingIntent.getActivity(
                    mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(mContext)
                    .setContentTitle(mContext.getResources().getString(
                            org.cyanogenmod.platform.internal.R.string.live_display_title))
                    .setContentText(mContext.getResources().getString(
                            org.cyanogenmod.platform.internal.R.string.live_display_hint))
                    .setSmallIcon(org.cyanogenmod.platform.internal.R.drawable.ic_livedisplay_notif)
                    .setStyle(new Notification.BigTextStyle().bigText(mContext.getResources()
                             .getString(
                                     org.cyanogenmod.platform.internal.R.string.live_display_hint)))
                    .setContentIntent(result)
                    .setAutoCancel(true);

            NotificationManager nm =
                    (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notifyAsUser(null, 1, builder.build(), UserHandle.CURRENT);

            updateSunsetCounter(1);
        }
    }

    private int getInt(String setting, int defValue) {
        return CMSettings.System.getIntForUser(mContext.getContentResolver(),
                setting, defValue, UserHandle.USER_CURRENT);
    }

    private void putInt(String setting, int value) {
        CMSettings.System.putIntForUser(mContext.getContentResolver(),
                setting, value, UserHandle.USER_CURRENT);
    }

    private synchronized void updateTwilight() {
        final TwilightState twilight = mTwilightManager.getCurrentState();
        for (int i = 0; i < mFeatures.size(); i++) {
            mFeatures.get(i).onTwilightUpdated(twilight);
        }
    }

    private synchronized void updateDisplayState(int displayState) {
        if (mDisplayState != displayState) {
            mDisplayState = displayState;

            for (int i = 0; i < mFeatures.size(); i++) {
                mFeatures.get(i).onDisplayStateChanged(displayState == Display.STATE_ON);
            }
        }
    }

    private synchronized void updateMode(int mode) {
        for (int i = 0; i < mFeatures.size(); i++) {
            mFeatures.get(i).onModeChanged(mode);
        }
    }

    private synchronized void updateLowPowerMode(boolean lowPowerMode) {
        if (mLowPowerMode != lowPowerMode) {
            mLowPowerMode = lowPowerMode;

            for (int i = 0; i < mFeatures.size(); i++) {
                mFeatures.get(i).onLowPowerModeChanged(mLowPowerMode);
            }
        }
    }

    private final class LiveDisplayHandler extends Handler {
        public LiveDisplayHandler(Looper looper) {
            super(looper, null, true /*async*/);
        }

        @Override
        public void handleMessage(Message msg) {
            if (!mInitialized) {
                return;
            }

            switch (msg.what) {
                case MSG_DISPLAY_CHANGED:
                    updateDisplayState(msg.arg1);
                    break;
                case MSG_LOW_POWER_MODE_CHANGED:
                    updateLowPowerMode(msg.arg1 == 1);
                    break;
                case MSG_TWILIGHT_UPDATE:
                    updateTwilight();
                    nudge();
                    break;
                case MSG_MODE_CHANGED:
                    stopNudgingMe();
                    updateMode(msg.arg1);
                    break;
            }
        }
    }
}
