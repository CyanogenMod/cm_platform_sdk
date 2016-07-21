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
package org.cyanogenmod.internal.hardware;

import cyanogenmod.hardware.DisplayMode;

public interface CMHardwareInterface {
    public int getSupportedFeatures();

    public boolean get(int feature);

    public boolean set(int feature, boolean enable);

    public int[] getDisplayColorCalibration();

    public boolean setDisplayColorCalibration(int[] rgb);

    public int getNumGammaControls();

    public int[] getDisplayGammaCalibration(int idx);

    public boolean setDisplayGammaCalibration(int idx, int[] rgb);

    public int[] getVibratorIntensity();

    public boolean setVibratorIntensity(int intensity);

    public String getLtoSource();

    public String getLtoDestination();

    public long getLtoDownloadInterval();

    public String getSerialNumber();

    public String getUniqueDeviceId();

    public boolean requireAdaptiveBacklightForSunlightEnhancement();

    public boolean isSunlightEnhancementSelfManaged();

    public DisplayMode[] getDisplayModes();

    public DisplayMode getCurrentDisplayMode();

    public DisplayMode getDefaultDisplayMode();

    public boolean setDisplayMode(DisplayMode mode, boolean makeDefault);

    public boolean writePersistentBytes(String key, byte[] value);

    public byte[] readPersistentBytes(String key);

    public int getColorBalanceMin();

    public int getColorBalanceMax();

    public int getColorBalance();

    public boolean setColorBalance(int value);
}
