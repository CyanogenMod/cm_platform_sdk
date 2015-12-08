/*
 * Copyright (c) 2011-2015 CyanogenMod Project
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
package cyanogenmod.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.android.internal.util.cm.palette.Palette;

/**
 * Helper class for colorspace conversions, and color-related
 * algorithms which may be generally useful.
 */
public class ColorUtils {

    private static int[] SOLID_COLORS = new int[] {
        Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
        Color.BLUE, Color.MAGENTA, Color.WHITE, Color.GRAY
    };

    /**
     * Converts an RGB packed int into L*a*b space, which is well-suited for finding
     * perceptual differences in color
     *
     * @param rgb A 32-bit value of packed RGB ints
     * @return array of Lab values of size 3
     */
    public static float[] convertRGBtoLAB(int rgb) {
        float[] lab = new float[3];
        float fx, fy, fz;
        float eps = 216.f / 24389.f;
        float k = 24389.f / 27.f;

        float Xr = 0.964221f;  // reference white D50
        float Yr = 1.0f;
        float Zr = 0.825211f;

        // RGB to XYZ
        float r = Color.red(rgb) / 255.f; //R 0..1
        float g = Color.green(rgb) / 255.f; //G 0..1
        float b = Color.blue(rgb) / 255.f; //B 0..1

        // assuming sRGB (D65)
        if (r <= 0.04045)
            r = r / 12;
        else
            r = (float) Math.pow((r + 0.055) / 1.055, 2.4);

        if (g <= 0.04045)
            g = g / 12;
        else
            g = (float) Math.pow((g + 0.055) / 1.055, 2.4);

        if (b <= 0.04045)
            b = b / 12;
        else
            b = (float) Math.pow((b + 0.055) / 1.055, 2.4);

        float X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        float Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        float Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        float xr = X / Xr;
        float yr = Y / Yr;
        float zr = Z / Zr;

        if (xr > eps)
            fx = (float) Math.pow(xr, 1 / 3.);
        else
            fx = (float) ((k * xr + 16.) / 116.);

        if (yr > eps)
            fy = (float) Math.pow(yr, 1 / 3.);
        else
            fy = (float) ((k * yr + 16.) / 116.);

        if (zr > eps)
            fz = (float) Math.pow(zr, 1 / 3.);
        else
            fz = (float) ((k * zr + 16.) / 116);

        float Ls = (116 * fy) - 16;
        float as = 500 * (fx - fy);
        float bs = 200 * (fy - fz);

        lab[0] = (2.55f * Ls + .5f);
        lab[1] = (as + .5f);
        lab[2] = (bs + .5f);

        return lab;
    }

    /**
     * Finds the "perceptually nearest" color from a list of colors to
     * the given RGB value. This is done by converting to
     * L*a*b colorspace and using a simple distance calculation.
     *
     * @param rgb The original color to start with
     * @param colors An array of colors to test
     * @return RGB packed int of nearest color in the list
     */
    public static int findPerceptuallyNearestColor(int rgb, int[] colors) {
        int nearest = 0;
        double distance = 3 * 255;

        if (rgb <= 0) {
            return 0;
        }

        float[] original = convertRGBtoLAB(rgb);

        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            float[] target = convertRGBtoLAB(color);

            double total = Math.sqrt(Math.pow(original[0] - target[0], 2) +
                                     Math.pow(original[1] - target[1], 2) +
                                     Math.pow(original[2] - target[2], 2));
            if (total < distance) {
                nearest = color;
                distance = total;
            }
        }
        return nearest;
    }

    /**
     * Convenience method to find the nearest "solid" color (having RGB components
     * of either 0 or 255) to the given color. This is useful for cases such as
     * LED notification lights which may not be able to display the full range
     * of colors due to hardware limitations.
     *
     * @param rgb
     * @return
     */
    public static int findPerceptuallyNearestSolidColor(int rgb) {
        return findPerceptuallyNearestColor(rgb, SOLID_COLORS);
    }

    /**
     * Takes a drawable and uses Palette to generate a suitable "alert"
     * color which can be used for an external notification mechanism
     * such as an RGB LED. This will always pick a solid color having
     * RGB components of 255 or 0.
     *
     * @param drawable The drawable to generate a color for
     * @return a suitable solid color which corresponds to the image
     */
    public static int generateAlertColorFromDrawable(Drawable drawable) {
        int color = 0;
        Bitmap bitmap = null;

        if (drawable == null) {
            return 0;
        }

        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                                         drawable.getIntrinsicHeight(),
                                         Bitmap.Config.ARGB_8888);
        }

        if (bitmap != null) {
            Palette p = Palette.generate(bitmap);
            color = findPerceptuallyNearestSolidColor(p.getVibrantColor(0)) & 0xFFFFFF;
            if (!(drawable instanceof BitmapDrawable)) {
                bitmap.recycle();
            }
        }

        return color;
    }
}
