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

import java.util.Collections;
import java.util.Comparator;

/**
 * Helper class for colorspace conversions, and color-related
 * algorithms which may be generally useful.
 */
public class ColorUtils {

    private static int[] SOLID_COLORS = new int[] {
        Color.RED, 0xFFFFA500, Color.YELLOW, Color.GREEN, Color.CYAN,
        Color.BLUE, Color.MAGENTA, Color.WHITE, Color.BLACK
    };

    /**
     * Drop the alpha component from an RGBA packed int and return
     * a non sign-extended RGB int.
     *
     * @param rgba
     * @return rgb
     */
    public static int dropAlpha(int rgba) {
        return rgba & 0x00FFFFFF;
    }

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
     * Calculate the colour difference value between two colours in lab space.
     * This code is from OpenIMAJ under BSD License
     *
     * @param L1 first colour's L component
     * @param a1 first colour's a component
     * @param b1 first colour's b component
     * @param L2 second colour's L component
     * @param a2 second colour's a component
     * @param b2 second colour's b component
     * @return the CIE 2000 colour difference
     */
    public static double calculateDeltaE(double L1, double a1, double b1,
            double L2, double a2, double b2) {
        double Lmean = (L1 + L2) / 2.0;
        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double Cmean = (C1 + C2) / 2.0;

        double G = (1 - Math.sqrt(Math.pow(Cmean, 7) / (Math.pow(Cmean, 7) + Math.pow(25, 7)))) / 2;
        double a1prime = a1 * (1 + G);
        double a2prime = a2 * (1 + G);

        double C1prime = Math.sqrt(a1prime * a1prime + b1 * b1);
        double C2prime = Math.sqrt(a2prime * a2prime + b2 * b2);
        double Cmeanprime = (C1prime + C2prime) / 2;

        double h1prime = Math.atan2(b1, a1prime)
                + 2 * Math.PI * (Math.atan2(b1, a1prime) < 0 ? 1 : 0);
        double h2prime = Math.atan2(b2, a2prime)
                + 2 * Math.PI * (Math.atan2(b2, a2prime) < 0 ? 1 : 0);
        double Hmeanprime = ((Math.abs(h1prime - h2prime) > Math.PI)
                ? (h1prime + h2prime + 2 * Math.PI) / 2 : (h1prime + h2prime) / 2);

        double T = 1.0 - 0.17 * Math.cos(Hmeanprime - Math.PI / 6.0)
                + 0.24 * Math.cos(2 * Hmeanprime) + 0.32 * Math.cos(3 * Hmeanprime + Math.PI / 30)
                - 0.2 * Math.cos(4 * Hmeanprime - 21 * Math.PI / 60);

        double deltahprime = ((Math.abs(h1prime - h2prime) <= Math.PI) ? h2prime - h1prime
                : (h2prime <= h1prime) ? h2prime - h1prime + 2 * Math.PI
                        : h2prime - h1prime - 2 * Math.PI);

        double deltaLprime = L2 - L1;
        double deltaCprime = C2prime - C1prime;
        double deltaHprime = 2.0 * Math.sqrt(C1prime * C2prime) * Math.sin(deltahprime / 2.0);
        double SL = 1.0 + ((0.015 * (Lmean - 50) * (Lmean - 50))
                / (Math.sqrt(20 + (Lmean - 50) * (Lmean - 50))));
        double SC = 1.0 + 0.045 * Cmeanprime;
        double SH = 1.0 + 0.015 * Cmeanprime * T;

        double deltaTheta = (30 * Math.PI / 180)
                * Math.exp(-((180 / Math.PI * Hmeanprime - 275) / 25)
                        * ((180 / Math.PI * Hmeanprime - 275) / 25));
        double RC = (2
                * Math.sqrt(Math.pow(Cmeanprime, 7) / (Math.pow(Cmeanprime, 7) + Math.pow(25, 7))));
        double RT = (-RC * Math.sin(2 * deltaTheta));

        double KL = 1;
        double KC = 1;
        double KH = 1;

        double deltaE = Math.sqrt(
                ((deltaLprime / (KL * SL)) * (deltaLprime / (KL * SL))) +
                        ((deltaCprime / (KC * SC)) * (deltaCprime / (KC * SC))) +
                        ((deltaHprime / (KH * SH)) * (deltaHprime / (KH * SH))) +
                        (RT * (deltaCprime / (KC * SC)) * (deltaHprime / (KH * SH))));

        return deltaE;
    }

    /**
     * Finds the "perceptually nearest" color from a list of colors to
     * the given RGB value. This is done by converting to
     * L*a*b colorspace and using the CIE2000 deltaE algorithm.
     *
     * @param rgb The original color to start with
     * @param colors An array of colors to test
     * @return RGB packed int of nearest color in the list
     */
    public static int findPerceptuallyNearestColor(int rgb, int[] colors) {
        int nearestColor = 0;
        double closest = Double.MAX_VALUE;

        float[] original = convertRGBtoLAB(rgb);

        for (int i = 0; i < colors.length; i++) {
            float[] cl = convertRGBtoLAB(colors[i]);
            double deltaE = calculateDeltaE(original[0], original[1], original[2],
                                            cl[0], cl[1], cl[2]);
            if (deltaE < closest) {
                nearestColor = colors[i];
                closest = deltaE;
            }
        }
        return nearestColor;
    }

    /**
     * Convenience method to find the nearest "solid" color (having RGB components
     * of either 0 or 255) to the given color. This is useful for cases such as
     * LED notification lights which may not be able to display the full range
     * of colors due to hardware limitations.
     *
     * @param rgb
     * @return the perceptually nearest color in RGB
     */
    public static int findPerceptuallyNearestSolidColor(int rgb) {
        return findPerceptuallyNearestColor(rgb, SOLID_COLORS);
    }

    /**
     * Given a Palette, pick out the dominant swatch based on population
     *
     * @param palette
     * @return the dominant Swatch
     */
    public static Palette.Swatch getDominantSwatch(Palette palette) {
        // find most-represented swatch based on population
        return Collections.max(palette.getSwatches(), new Comparator<Palette.Swatch>() {
            @Override
            public int compare(Palette.Swatch sw1, Palette.Swatch sw2) {
                return Integer.compare(sw1.getPopulation(), sw2.getPopulation());
            }
        });
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
        int alertColor = Color.BLACK;
        Bitmap bitmap = null;

        if (drawable == null) {
            return alertColor;
        }

        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                                         drawable.getIntrinsicHeight(),
                                         Bitmap.Config.ARGB_8888);
        }

        if (bitmap != null) {
            Palette p = Palette.from(bitmap).generate();

            // First try the dominant color
            int iconColor = getDominantSwatch(p).getRgb();
            alertColor = findPerceptuallyNearestSolidColor(iconColor);

            // Try the most saturated color if we got white or black (boring)
            if (alertColor == Color.BLACK || alertColor == Color.WHITE) {
                iconColor = p.getVibrantColor(Color.WHITE);
                alertColor = findPerceptuallyNearestSolidColor(iconColor);
            }

            if (!(drawable instanceof BitmapDrawable)) {
                bitmap.recycle();
            }
        }

        return alertColor;
    }
}
