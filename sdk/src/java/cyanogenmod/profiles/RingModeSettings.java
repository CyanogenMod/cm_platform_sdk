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

package cyanogenmod.profiles;

import android.content.Context;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;

import cyanogenmod.os.Build;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * The {@link StreamSettings} class allows for creating various {@link android.media.AudioManager}
 * overrides on the device depending on their capabilities.
 *
 * <p>Example for setting the default ring mode to muted:
 * <pre class="prettyprint">
 * RingModeSettings ringSettings = new RingModeSettings(RING_MODE_MUTE, true));
 * profile.setRingMode(ringSettings);
 * </pre>
 */
public final class RingModeSettings implements Parcelable {
    public static final String RING_MODE_NORMAL = "normal";
    public static final String RING_MODE_VIBRATE = "vibrate";
    public static final String RING_MODE_MUTE = "mute";

    private String mValue;
    private boolean mOverride;
    private boolean mDirty;

    /** @hide */
    public static final Parcelable.Creator<RingModeSettings> CREATOR
            = new Parcelable.Creator<RingModeSettings>() {
        public RingModeSettings createFromParcel(Parcel in) {
            return new RingModeSettings(in);
        }

        @Override
        public RingModeSettings[] newArray(int size) {
            return new RingModeSettings[size];
        }
    };

    /**
     * Unwrap {@link RingModeSettings} from a parcel.
     * @param parcel
     */
    public RingModeSettings(Parcel parcel) {
        readFromParcel(parcel);
    }

    /**
     * Construct a {@link RingModeSettings} with a default state of #RING_MODE_NORMAL.
     */
    public RingModeSettings() {
        this(RING_MODE_NORMAL, false);
    }

    /**
     * Construct a {@link RingModeSettings} with a default value and whether or not it should
     * override user settings.
     * @param value ex: {@link #RING_MODE_VIBRATE}
     * @param override whether or not the setting should override user settings
     */
    public RingModeSettings(String value, boolean override) {
        mValue = value;
        mOverride = override;
        mDirty = false;
    }

    /**
     * Get the default value for the {@link RingModeSettings}
     * @return integer value corresponding with its type
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Set the default value for the {@link RingModeSettings}
     * @param value ex: {@link #RING_MODE_VIBRATE}
     */
    public void setValue(String value) {
        mValue = value;
        mDirty = true;
    }

    /**
     * Set whether or not the {@link RingModeSettings} should override default user values
     * @param override boolean override
     */
    public void setOverride(boolean override) {
        mOverride = override;
        mDirty = true;
    }

    /**
     * Check whether or not the {@link RingModeSettings} overrides user settings.
     * @return true if override
     */
    public boolean isOverride() {
        return mOverride;
    }

    /** @hide */
    public boolean isDirty() {
        return mDirty;
    }

    /** @hide */
    public void processOverride(Context context) {
        if (isOverride()) {
            int ringerMode = AudioManager.RINGER_MODE_NORMAL;
            if (mValue.equals(RING_MODE_MUTE)) {
                ringerMode = AudioManager.RINGER_MODE_SILENT;
            } else if (mValue.equals(RING_MODE_VIBRATE)) {
                ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            }
            AudioManager amgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            amgr.setRingerModeInternal(ringerMode);
        }
    }

    /** @hide */
    public static RingModeSettings fromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException, IOException {
        int event = xpp.next();
        RingModeSettings ringModeDescriptor = new RingModeSettings();
        while ((event != XmlPullParser.END_TAG && event != XmlPullParser.END_DOCUMENT) ||
                !xpp.getName().equals("ringModeDescriptor")) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("value")) {
                    ringModeDescriptor.mValue = xpp.nextText();
                } else if (name.equals("override")) {
                    ringModeDescriptor.mOverride = Boolean.parseBoolean(xpp.nextText());
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while parsing ring mode settings");
            }
            event = xpp.next();
        }
        return ringModeDescriptor;
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append("<ringModeDescriptor>\n<value>");
        builder.append(mValue);
        builder.append("</value>\n<override>");
        builder.append(mOverride);
        builder.append("</override>\n</ringModeDescriptor>\n");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        dest.writeInt(Build.PARCELABLE_VERSION);

        // Inject a placeholder that will store the parcel size from this point on
        // (not including the size itself).
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();

        // === BOYSENBERRY ===
        dest.writeInt(mOverride ? 1 : 0);
        dest.writeString(mValue);
        dest.writeInt(mDirty ? 1 : 0);

        // Go back and write size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    /** @hide */
    public void readFromParcel(Parcel in) {
        // Read parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        int parcelableVersion = in.readInt();
        int parcelableSize = in.readInt();
        int startPosition = in.dataPosition();

        // Pattern here is that all new members should be added to the end of
        // the writeToParcel method. Then we step through each version, until the latest
        // API release to help unravel this parcel
        if (parcelableVersion >= Build.CM_VERSION_CODES.BOYSENBERRY) {
            mOverride = in.readInt() != 0;
            mValue = in.readString();
            mDirty = in.readInt() != 0;
        }

        in.setDataPosition(startPosition + parcelableSize);
    }
}
