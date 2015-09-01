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

package org.cyanogenmod.tests.hardware;

import android.os.Bundle;

import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.DisplayMode;

import org.cyanogenmod.tests.TestActivity;

/**
 * Created by adnan on 8/31/15.
 */
public class CMHardwareTest extends TestActivity {
    private CMHardwareManager mHardwareManager;

    private static final List<Integer> FEATURES = Arrays.asList(
            CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT,
            CMHardwareManager.FEATURE_COLOR_ENHANCEMENT,
            CMHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION,
            CMHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION,
            CMHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY,
            CMHardwareManager.FEATURE_KEY_DISABLE,
            CMHardwareManager.FEATURE_LONG_TERM_ORBITS,
            CMHardwareManager.FEATURE_SERIAL_NUMBER,
            CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT,
            CMHardwareManager.FEATURE_TAP_TO_WAKE,
            CMHardwareManager.FEATURE_TOUCH_HOVERING,
            CMHardwareManager.FEATURE_AUTO_CONTRAST,
            CMHardwareManager.FEATURE_DISPLAY_MODES,
            CMHardwareManager.FEATURE_PERSISTENT_STORAGE
    );

    private static final List<Integer> BOOLEAN_FEATURES = Arrays.asList(
            CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT,
            CMHardwareManager.FEATURE_COLOR_ENHANCEMENT,
            CMHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY,
            CMHardwareManager.FEATURE_KEY_DISABLE,
            CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT,
            CMHardwareManager.FEATURE_TAP_TO_WAKE,
            CMHardwareManager.FEATURE_TOUCH_HOVERING,
            CMHardwareManager.FEATURE_AUTO_CONTRAST
    );

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mHardwareManager = CMHardwareManager.getInstance(this);
    }

    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        return mTests;
    }

    private boolean vibratorSupported() {
        if (mHardwareManager.isSupported(CMHardwareManager.FEATURE_VIBRATOR)) {
            return true;
        } else {
            Toast.makeText(CMHardwareTest.this, "Vibrator not supported",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean displayColorCalibrationSupported() {
        if (mHardwareManager.isSupported(CMHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION)) {
            return true;
        } else {
            Toast.makeText(CMHardwareTest.this, "Display Color Calibration not supported",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean ltoSupported() {
        if (mHardwareManager.isSupported(CMHardwareManager.FEATURE_LONG_TERM_ORBITS)) {
            return true;
        } else {
            Toast.makeText(CMHardwareTest.this, "Long Term Orbits not supported",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean serialSupported() {
        if (mHardwareManager.isSupported(CMHardwareManager.FEATURE_SERIAL_NUMBER)) {
            return true;
        } else {
            Toast.makeText(CMHardwareTest.this, "Serial number not supported",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean displayModesSupported() {
        if (mHardwareManager.isSupported(CMHardwareManager.FEATURE_DISPLAY_MODES)) {
            return true;
        } else {
            Toast.makeText(CMHardwareTest.this, "Display modes not supported",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean persistentStorageSupported() {
        if (mHardwareManager.isSupported(CMHardwareManager.FEATURE_PERSISTENT_STORAGE)) {
            return true;
        } else {
            Toast.makeText(CMHardwareTest.this, "Persistent storage not supported",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private Test[] mTests = new Test[] {
            new Test("Test get supported features") {
                public void run() {
                    Toast.makeText(CMHardwareTest.this, "Supported features " +
                                    mHardwareManager.getSupportedFeatures(),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("Test features supported") {
                @Override
                protected void run() {
                    StringBuilder builder = new StringBuilder();
                    for (int feature : FEATURES) {
                        builder.append("Feature " + feature + "\n")
                                .append("is supported " + mHardwareManager.isSupported(feature)
                                        + "\n");
                    }
                    Toast.makeText(CMHardwareTest.this, "Supported features " +
                                    builder.toString(),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("Test boolean features enabled") {
                @Override
                protected void run() {
                    StringBuilder builder = new StringBuilder();
                    for (int feature : BOOLEAN_FEATURES) {
                        builder.append("Feature " + feature + "\n")
                                .append("is enabled " + mHardwareManager.isSupported(feature)
                                        + "\n");
                    }
                    Toast.makeText(CMHardwareTest.this, "Features " +
                                    builder.toString(),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("Test get vibrator intensity") {
                @Override
                protected void run() {
                    if (vibratorSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Vibrator intensity " +
                                        mHardwareManager.getVibratorIntensity(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test get vibrator default intensity") {
                @Override
                protected void run() {
                    if (vibratorSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Vibrator default intensity " +
                                        mHardwareManager.getVibratorDefaultIntensity(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test get vibrator max intensity") {
                @Override
                protected void run() {
                    if (vibratorSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Vibrator max intensity " +
                                        mHardwareManager.getVibratorMaxIntensity(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test get vibrator min intensity") {
                @Override
                protected void run() {
                    if (vibratorSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Vibrator min intensity " +
                                        mHardwareManager.getVibratorMinIntensity(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test get vibrator min intensity") {
                @Override
                protected void run() {
                    if (vibratorSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Vibrator min intensity " +
                                        mHardwareManager.getVibratorWarningIntensity(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Display Color Calibration") {
                @Override
                protected void run() {
                    if (displayColorCalibrationSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Display Color Calibration " +
                                        mHardwareManager.getDisplayColorCalibration(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Default Display Color Calibration") {
                @Override
                protected void run() {
                    if (displayColorCalibrationSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Default Display Color Calibration " +
                                        mHardwareManager.getDisplayColorCalibrationDefault(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Display Color Calibration Max") {
                @Override
                protected void run() {
                    if (displayColorCalibrationSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Display Color Calibration Max " +
                                        mHardwareManager.getDisplayColorCalibrationMax(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Display Color Calibration Min") {
                @Override
                protected void run() {
                    if (displayColorCalibrationSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Display Color Calibration Min " +
                                        mHardwareManager.getDisplayColorCalibrationMin(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Set Display Color Calibration") {
                @Override
                protected void run() {
                    if (displayColorCalibrationSupported()) {
                        mHardwareManager.setDisplayColorCalibration(new int[] {0,0,0});
                    }
                }
            },
            new Test("Test Get Long Term Orbits Source") {
                @Override
                protected void run() {
                    if (ltoSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Long Term Orbit Source " +
                                        mHardwareManager.getLtoSource(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Get Long Term Orbits Destination") {
                @Override
                protected void run() {
                    if (ltoSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Long Term Orbit Destination " +
                                        mHardwareManager.getLtoDestination(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Get Long Term Orbits Interval") {
                @Override
                protected void run() {
                    if (ltoSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Long Term Orbit Download Interval " +
                                        mHardwareManager.getLtoDownloadInterval(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Get Serial Number") {
                @Override
                protected void run() {
                    if (serialSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Serial number " +
                                        mHardwareManager.getSerialNumber(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Get Display Modes") {
                @Override
                protected void run() {
                    if (displayModesSupported()) {
                        StringBuilder builder = new StringBuilder();
                        for (DisplayMode displayMode : mHardwareManager.getDisplayModes()) {
                            builder.append("Display mode " + displayMode.name + "\n");
                        }
                        Toast.makeText(CMHardwareTest.this, "Display modes: \n"
                                        + builder.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Get Current Display Mode") {
                @Override
                protected void run() {
                    if (displayModesSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Default Display Mode " +
                                        mHardwareManager.getCurrentDisplayMode(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Test("Test Get Default Display Mode") {
                @Override
                protected void run() {
                    if (displayModesSupported()) {
                        Toast.makeText(CMHardwareTest.this, "Default Display Mode " +
                                        mHardwareManager.getCurrentDisplayMode(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            },
    };
}
