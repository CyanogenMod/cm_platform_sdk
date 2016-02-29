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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;

/**
 * TODO: unhide once documented and finalized
 * @hide
 */
public class ExternalViewProperties {

    private final int[] mScreenCoords = new int[2];
    private final View mView;
    private final View mDecorView;
    private int mWidth, mHeight;
    private boolean mVisible;
    private Rect mHitRect = new Rect();

    ExternalViewProperties(View view, Context context) {
        mView = view;
        if (context  instanceof Activity) {
            mDecorView = ((Activity) context).getWindow().getDecorView();
        } else {
            mDecorView = null;
        }
    }

    public Rect getHitRect() {
        return mHitRect;
    }

    public int getX() {
        return mScreenCoords[0];
    }

    public int getY() {
        return mScreenCoords[1];
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public boolean hasChanged() {
        int previousWidth = mWidth;
        int previousHeight = mHeight;
        mWidth = mView.getWidth();
        mHeight = mView.getHeight();

        int previousX = mScreenCoords[0];
        int previousY = mScreenCoords[1];
        mView.getLocationOnScreen(mScreenCoords);
        int newX = mScreenCoords[0];
        int newY = mScreenCoords[1];

        mHitRect.setEmpty();
        if (mDecorView != null) {
            mDecorView.getHitRect(mHitRect);
        }
        boolean visible = mView.getLocalVisibleRect(mHitRect);
        mVisible = visible;

        // Check if anything actually changed
        return previousX != newX || previousY != newY
                || previousWidth != mWidth || previousHeight != mHeight
                || mVisible != visible;
    }
}
