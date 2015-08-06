package cyanogenmod.externalviews;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;

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
