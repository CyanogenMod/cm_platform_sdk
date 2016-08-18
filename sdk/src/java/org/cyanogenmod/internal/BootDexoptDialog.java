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

package org.cyanogenmod.internal;

import android.app.Dialog;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cyanogenmod.platform.internal.R;

/**
 * @hide
 */
public class BootDexoptDialog extends Dialog {
    private final boolean mHideAppDetails;

    private final ImageView mAppIcon;
    private final TextView mMessage;
    private final TextView mDetailMsg;
    private final ProgressBar mProgress;

    public static BootDexoptDialog create(Context context) {
        return create(context, WindowManager.LayoutParams.TYPE_BOOT_PROGRESS);
    }

    public static BootDexoptDialog create(Context context, int windowType) {
        final PackageManager pm = context.getPackageManager();
        final int brandLogo;
        final int theme;
        if (Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED , 0) == 1) {
            brandLogo = R.drawable.dexopt_brand_logo_alternative;
        } else {
            brandLogo = R.drawable.dexopt_brand_logo;
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            theme = com.android.internal.R.style.Theme_Micro_Dialog_Alert;
        } else if (pm.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            theme = com.android.internal.R.style.Theme_Leanback_Dialog_Alert;
        } else {
            theme = com.android.internal.R.style.Theme_Material_Light;
        }

        return new BootDexoptDialog(context, theme, windowType, brandLogo);
    }

    private BootDexoptDialog(Context context, int themeResId, int windowType,
            int brandLogoResId) {
        super(context, themeResId);
        mHideAppDetails = context.getResources().getBoolean(
                R.bool.config_bootDexoptHideAppDetails);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (windowType != 0) {
            getWindow().setType(windowType);
        }

        setContentView(R.layout.dexopt_dialog);

        final ImageView brandLogo = (ImageView) findViewById(R.id.dexopt_logo_view);
        brandLogo.setImageResource(brandLogoResId);

        mMessage = (TextView) findViewById(R.id.dexopt_message);
        mDetailMsg = (TextView) findViewById(R.id.dexopt_message_detail);
        mAppIcon = (ImageView) findViewById(R.id.dexopt_icon);
        mProgress = (ProgressBar) findViewById(R.id.dexopt_progress);

        getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final WindowManager.LayoutParams lp = getWindow().getAttributes();
        // turn off button lights when dexopting
        lp.buttonBrightness = 0;
        lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        getWindow().setAttributes(lp);
        setCancelable(false);
        show();

        // start the marquee
        mMessage.setSelected(true);
        mDetailMsg.setSelected(true);
    }

    public void setProgress(final int stage, final ApplicationInfo optimizedApp,
            final int currentAppPos, final int totalAppCount) {
        if (totalAppCount > 0) {
            mProgress.setMax(totalAppCount);
        }

        final Resources res = getContext().getResources();
        final PackageManager pm = getContext().getPackageManager();

        if (optimizedApp != null) {
            if (mHideAppDetails) {
                mMessage.setText(res.getString(R.string.android_preparing_apk_obscured));
                mAppIcon.setImageResource(R.drawable.ic_dexopt_obscured);
            } else {
                final CharSequence label = optimizedApp.loadLabel(pm);
                mMessage.setText(res.getString(R.string.android_preparing_apk, label));
                mAppIcon.setImageDrawable(optimizedApp.loadIcon(pm));
            }
            mDetailMsg.setText(res.getString(
                    R.string.android_upgrading_apk, currentAppPos, totalAppCount));
            mProgress.setProgress(currentAppPos);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mMessage.setText(res.getString(R.string.android_upgrading_complete));
            mAppIcon.setImageResource(R.drawable.ic_dexopt_starting);
            mProgress.setVisibility(View.INVISIBLE);

            if (stage == IActivityManager.BOOT_STAGE_STARTING_APPS) {
                mDetailMsg.setText(res.getString(R.string.android_upgrading_starting_apps));
            } else if (stage == IActivityManager.BOOT_STAGE_FSTRIM) {
                mDetailMsg.setText(res.getString(R.string.android_upgrading_fstrim));
            } else if (stage == IActivityManager.BOOT_STAGE_COMPLETE) {
                mDetailMsg.setText(res.getString(R.string.android_upgrading_complete_details));
            } else {
                mDetailMsg.setText(null);
            }
        }
    }

    // This dialog will consume all events coming in to
    // it, to avoid it trying to do things too early in boot.

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return true;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }
}
