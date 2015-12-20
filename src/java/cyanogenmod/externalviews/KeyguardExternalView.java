/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package cyanogenmod.externalviews;

import android.content.Context;
import android.content.ComponentName;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.WindowManager;

/**
 * TODO: unhide once documented and finalized
 * @hide
 */
public final class KeyguardExternalView extends ExternalView {

    public static final String EXTRA_PERMISSION_LIST = "permissions_list";
    public static final String CATEGORY_KEYGUARD_GRANT_PERMISSION
            = "org.cyanogenmod.intent.category.KEYGUARD_GRANT_PERMISSION";

    private final Point mDisplaySize;

    public KeyguardExternalView(Context context, AttributeSet attrs) {
        this(context, attrs, null);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context,attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        this(context,attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attributeSet,
            ComponentName componentName) {
        super(context,attributeSet,componentName);
        mDisplaySize = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealSize(mDisplaySize);
    }

    @Override
    public boolean onPreDraw() {
        if (!mExternalViewProperties.hasChanged()) {
            return true;
        }
        // keyguard views always take up the full screen when visible
        final int x = mExternalViewProperties.getX();
        final int y = mExternalViewProperties.getY();
        final int width = mDisplaySize.x - x;
        final int height = mDisplaySize.y - y;
        final boolean visible = mExternalViewProperties.isVisible();
        final Rect clipRect = new Rect(x, y, width + x, height + y);
        performAction(new Runnable() {
            @Override
            public void run() {
                try {
                    mExternalViewProvider.alterWindow(x, y, width, height, visible,
                            clipRect);
                } catch (RemoteException e) {
                }
            }
        });
        return true;
    }
}
