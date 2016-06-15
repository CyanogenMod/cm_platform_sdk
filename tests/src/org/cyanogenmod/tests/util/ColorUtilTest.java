/**
 * Copyright (c) 2016, The CyanogenMod Project
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

package org.cyanogenmod.tests.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.test.AndroidTestCase;
import cyanogenmod.util.ColorUtils;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ColorUtilTest extends AndroidTestCase {
    private ColorUtils mColorUtils;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mColorUtils = new ColorUtils();
    }

    public void testDropAlpha() {
        // With pre-existing alpha
        int color = 0xBBCCDD;
        int alpha = 0xAA000000;
        int result = mColorUtils.dropAlpha(color | alpha);
        // Assert alpha was removed
        assertEquals(Color.alpha(result), 0);
        // Ensure color is preserved
        assertEquals(result & color, color);

        // Without pre-existing alpha
        color = Color.argb(0, 100, 200 ,300);
        result = mColorUtils.dropAlpha(color);
        // Assert alpha was removed
        assertEquals(Color.alpha(result), 0);
        // Ensure color is preserved
        assertEquals(result, color);
    }

    public void testGenerateAlertColorFromDrawable() {
        // Test null drawable
        int color = mColorUtils.generateAlertColorFromDrawable(null);
        assertEquals(color, Color.BLACK);

        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
        bitmapDrawable.setBounds(0, 0, 10, 10);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.RED);

        // Test fully red bitmap
        color = mColorUtils.generateAlertColorFromDrawable(bitmapDrawable);
        assertEquals(color, Color.RED);
        color = mColorUtils.generateAlertColorFromDrawable(
                getColorDrawableFromBitmapDrawable(bitmapDrawable));
        assertEquals(color, Color.RED);

        // Test blue/red bitmap with blue dominating
        Paint p = new Paint();
        p.setColor(Color.BLUE);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(0, 0, 8, 8, p);
        color = mColorUtils.generateAlertColorFromDrawable(bitmapDrawable);
        assertEquals(color, Color.BLUE);
        color = mColorUtils.generateAlertColorFromDrawable(
                getColorDrawableFromBitmapDrawable(bitmapDrawable));
        assertEquals(color, Color.BLUE);

        // Test large white + small blue scenario
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(0, 0, 2, 2, p);
        color = mColorUtils.generateAlertColorFromDrawable(bitmapDrawable);
        assertEquals(color, Color.BLUE);
        color = mColorUtils.generateAlertColorFromDrawable(
                getColorDrawableFromBitmapDrawable(bitmapDrawable));
        assertEquals(color, Color.BLUE);

        // Test large white + small black scenario
        canvas.drawColor(Color.WHITE);
        p.setColor(Color.BLACK);
        canvas.drawRect(0, 0, 2, 2, p);
        color = mColorUtils.generateAlertColorFromDrawable(bitmapDrawable);
        assertEquals(color, Color.WHITE);
        color = mColorUtils.generateAlertColorFromDrawable(
                getColorDrawableFromBitmapDrawable(bitmapDrawable));
        assertEquals(color, Color.WHITE);

        assertEquals(bitmap.isRecycled(), false);
        bitmap.recycle();
    }

    private ColorDrawable getColorDrawableFromBitmapDrawable(final BitmapDrawable bitmapDrawable) {
        ColorDrawable colorDrawable = Mockito.mock(ColorDrawable.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Canvas canvas = (Canvas) invocation.getArguments()[0];
                bitmapDrawable.draw(canvas);
                return null;
            }
        }).when(colorDrawable).draw(Mockito.any(Canvas.class));
        return colorDrawable;
    }
}
