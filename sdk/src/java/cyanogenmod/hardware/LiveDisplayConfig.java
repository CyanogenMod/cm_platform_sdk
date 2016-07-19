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
package cyanogenmod.hardware;

import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_FIRST;
import static cyanogenmod.hardware.LiveDisplayManager.FEATURE_LAST;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_FIRST;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_LAST;
import static cyanogenmod.hardware.LiveDisplayManager.MODE_OFF;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Range;

import java.util.BitSet;

import cyanogenmod.os.Build;
import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;

/**
 * Holder class for LiveDisplay static configuration.
 *
 * This class holds various defaults and hardware capabilities
 * which are involved with LiveDisplay.
 */
public class LiveDisplayConfig implements Parcelable {

    private final BitSet mCapabilities;
    private final BitSet mAllModes = new BitSet();

    private final int mDefaultDayTemperature;
    private final int mDefaultNightTemperature;
    private final int mDefaultMode;

    private final boolean mDefaultAutoContrast;
    private final boolean mDefaultAutoOutdoorMode;
    private final boolean mDefaultCABC;
    private final boolean mDefaultColorEnhancement;

    private final Range<Integer> mColorTemperatureRange;
    private final Range<Integer> mColorBalanceRange;

    public LiveDisplayConfig(BitSet capabilities, int defaultMode,
            int defaultDayTemperature, int defaultNightTemperature,
            boolean defaultAutoOutdoorMode, boolean defaultAutoContrast,
            boolean defaultCABC, boolean defaultColorEnhancement,
            Range<Integer> colorTemperatureRange,
            Range<Integer> colorBalanceRange) {
        super();
        mCapabilities = (BitSet) capabilities.clone();
        mAllModes.set(MODE_FIRST, MODE_LAST);
        mDefaultMode = defaultMode;
        mDefaultDayTemperature = defaultDayTemperature;
        mDefaultNightTemperature = defaultNightTemperature;
        mDefaultAutoContrast = defaultAutoContrast;
        mDefaultAutoOutdoorMode = defaultAutoOutdoorMode;
        mDefaultCABC = defaultCABC;
        mDefaultColorEnhancement = defaultColorEnhancement;
        mColorTemperatureRange = colorTemperatureRange;
        mColorBalanceRange = colorBalanceRange;
    }

    private LiveDisplayConfig(Parcel parcel) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
        int parcelableVersion = parcelInfo.getParcelVersion();

        // temp vars
        long capabilities = 0;
        int defaultMode = 0;
        int defaultDayTemperature = -1;
        int defaultNightTemperature = -1;
        boolean defaultAutoContrast = false;
        boolean defaultAutoOutdoorMode = false;
        boolean defaultCABC = false;
        boolean defaultColorEnhancement = false;
        int minColorTemperature = 0;
        int maxColorTemperature = 0;
        int minColorBalance = 0;
        int maxColorBalance = 0;

        if (parcelableVersion >= Build.CM_VERSION_CODES.FIG) {
            capabilities = parcel.readLong();
            defaultMode = parcel.readInt();
            defaultDayTemperature = parcel.readInt();
            defaultNightTemperature = parcel.readInt();
            defaultAutoContrast = parcel.readInt() == 1;
            defaultAutoOutdoorMode = parcel.readInt() == 1;
            defaultCABC = parcel.readInt() == 1;
            defaultColorEnhancement = parcel.readInt() == 1;
            minColorTemperature = parcel.readInt();
            maxColorTemperature = parcel.readInt();
            minColorBalance = parcel.readInt();
            maxColorBalance = parcel.readInt();
        }

        // set temps
        mCapabilities = BitSet.valueOf(new long[] { capabilities });
        mAllModes.set(MODE_FIRST, MODE_LAST);
        mDefaultMode = defaultMode;
        mDefaultDayTemperature = defaultDayTemperature;
        mDefaultNightTemperature = defaultNightTemperature;
        mDefaultAutoContrast = defaultAutoContrast;
        mDefaultAutoOutdoorMode = defaultAutoOutdoorMode;
        mDefaultCABC = defaultCABC;
        mDefaultColorEnhancement = defaultColorEnhancement;
        mColorTemperatureRange = Range.create(minColorTemperature, maxColorTemperature);
        mColorBalanceRange = Range.create(minColorBalance, maxColorBalance);

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("capabilities=").append(mCapabilities.toString());
        sb.append(" defaultMode=").append(mDefaultMode);
        sb.append(" defaultDayTemperature=").append(mDefaultDayTemperature);
        sb.append(" defaultNightTemperature=").append(mDefaultNightTemperature);
        sb.append(" defaultAutoOutdoorMode=").append(mDefaultAutoOutdoorMode);
        sb.append(" defaultAutoContrast=").append(mDefaultAutoContrast);
        sb.append(" defaultCABC=").append(mDefaultCABC);
        sb.append(" defaultColorEnhancement=").append(mDefaultColorEnhancement);
        sb.append(" colorTemperatureRange=").append(mColorTemperatureRange);
        sb.append(" colorBalanceRange=").append(mColorBalanceRange);
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(out);

        // ==== FIG =====
        long[] caps = mCapabilities.toLongArray();
        out.writeLong(caps != null && caps.length > 0 ? caps[0] : 0L);
        out.writeInt(mDefaultMode);
        out.writeInt(mDefaultDayTemperature);
        out.writeInt(mDefaultNightTemperature);
        out.writeInt(mDefaultAutoContrast ? 1 : 0);
        out.writeInt(mDefaultAutoOutdoorMode ? 1 : 0);
        out.writeInt(mDefaultCABC ? 1 : 0);
        out.writeInt(mDefaultColorEnhancement ? 1 : 0);
        out.writeInt(mColorTemperatureRange.getLower());
        out.writeInt(mColorTemperatureRange.getUpper());
        out.writeInt(mColorBalanceRange.getLower());
        out.writeInt(mColorBalanceRange.getUpper());

        // Complete the parcel info for the concierge
        parcelInfo.complete();
    }

    /**
     * Checks if a particular feature or mode is supported by the system.
     *
     * @param feature
     * @return true if capable
     */
    public boolean hasFeature(int feature) {
        return ((feature >= MODE_FIRST && feature <= MODE_LAST) ||
                (feature >= FEATURE_FIRST && feature <= FEATURE_LAST)) &&
                (feature == MODE_OFF || mCapabilities.get(feature));
    }

    /**
     * Checks if LiveDisplay is available for use on this device.
     *
     * @return true if any feature is enabled
     */
    public boolean isAvailable() {
        return !mCapabilities.isEmpty();
    }

    /**
     * Checks if LiveDisplay has support for adaptive modes.
     *
     * @return true if adaptive modes are available
     */
    public boolean hasModeSupport() {
        return isAvailable() && mCapabilities.intersects(mAllModes);
    }

    /**
     * Gets the default color temperature to use in the daytime. This is typically
     * set to 6500K, however this may not be entirely accurate. Use this value for
     * resetting controls to the default.
     *
     * @return the default day temperature in K
     */
    public int getDefaultDayTemperature() {
        return mDefaultDayTemperature;
    }

    /**
     * Gets the default color temperature to use at night. This is typically set
     * to 4500K, but this may not be entirely accurate. Use this value for resetting
     * controls to defaults.
     *
     * @return the default night color temperature
     */
    public int getDefaultNightTemperature() {
        return mDefaultNightTemperature;
    }

    /**
     * Get the default adaptive mode.
     *
     * @return the default mode
     */
    public int getDefaultMode() {
        return mDefaultMode;
    }

    /**
     * Get the default value for auto contrast
     *
     * @return true if enabled
     */
    public boolean getDefaultAutoContrast() {
        return mDefaultAutoContrast;
    }

    /**
     * Get the default value for automatic outdoor mode
     *
     * @return true if enabled
     */
    public boolean getDefaultAutoOutdoorMode() {
        return mDefaultAutoOutdoorMode;
    }

    /**
     * Get the default value for CABC
     *
     * @return true if enabled
     */
    public boolean getDefaultCABC() {
        return mDefaultCABC;
    }

    /**
     * Get the default value for color enhancement
     *
     * @return true if enabled
     */
    public boolean getDefaultColorEnhancement() {
        return mDefaultColorEnhancement;
    }

    /**
     * Get the range of supported color temperatures
     *
     * @return range in Kelvin
     */
    public Range<Integer> getColorTemperatureRange() {
        return mColorTemperatureRange;
    }

    /**
     * Get the range of supported color balance
     *
     * @return linear range which maps into the temperature range curve
     */
    public Range<Integer> getColorBalanceRange() {
        return mColorBalanceRange;
    }

    /** @hide */
    public static final Parcelable.Creator<LiveDisplayConfig> CREATOR =
            new Parcelable.Creator<LiveDisplayConfig>() {
        public LiveDisplayConfig createFromParcel(Parcel in) {
            return new LiveDisplayConfig(in);
        }

        @Override
        public LiveDisplayConfig[] newArray(int size) {
            return new LiveDisplayConfig[size];
        }
    };
}
