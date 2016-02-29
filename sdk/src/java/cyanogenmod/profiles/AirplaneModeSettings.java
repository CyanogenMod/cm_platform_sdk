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
import android.content.Intent;
import android.provider.Settings;
import android.os.Parcel;
import android.os.Parcelable;

import cyanogenmod.os.Build;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


import java.io.IOException;

/**
 * The {@link AirplaneModeSettings} class allows for overriding and setting the airplane mode.
 *
 * <p>Example for setting the airplane mode to enabled:
 * <pre class="prettyprint">
 * AirplaneModeSettings airplaneMode = new AirplaneModeSettings(BooleanState.STATE_ENABLED, true)
 * profile.setAirplaneMode(airplaneMode);
 * </pre>
 */
public final class AirplaneModeSettings implements Parcelable {

    private int mValue;
    private boolean mOverride;
    private boolean mDirty;

    /** @hide */
    public static final Parcelable.Creator<AirplaneModeSettings> CREATOR =
            new Parcelable.Creator<AirplaneModeSettings>() {
        public AirplaneModeSettings createFromParcel(Parcel in) {
            return new AirplaneModeSettings(in);
        }

        @Override
        public AirplaneModeSettings[] newArray(int size) {
            return new AirplaneModeSettings[size];
        }
    };

    /**
     * BooleanStates for specific {@link AirplaneModeSettings}
     */
    public static class BooleanState {
        /** Disabled state */
        public static final int STATE_DISALED = 0;
        /** Enabled state */
        public static final int STATE_ENABLED = 1;
    }

    /**
     * Unwrap {@link AirplaneModeSettings} from a parcel.
     * @param parcel
     */
    public AirplaneModeSettings(Parcel parcel) {
        readFromParcel(parcel);
    }

    /**
     * Construct a {@link AirplaneModeSettings} with a default value of
     * {@link BooleanState#STATE_DISALED}.
     */
    public AirplaneModeSettings() {
        this(BooleanState.STATE_DISALED, false);
    }

    /**
     * Construct a {@link AirplaneModeSettings} with a default value and whether or not it should
     * override user settings.
     * @param value ex: {@link BooleanState#STATE_DISALED}
     * @param override whether or not the setting should override user settings
     */
    public AirplaneModeSettings(int value, boolean override) {
        mValue = value;
        mOverride = override;
        mDirty = false;
    }

    /**
     * Get the default value for the {@link AirplaneModeSettings}
     * @return integer value corresponding with its brightness value
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Set the default value for the {@link AirplaneModeSettings}
     * @param value {@link BooleanState#STATE_DISALED}
     */
    public void setValue(int value) {
        mValue = value;
        mDirty = true;
    }

    /**
     * Set whether or not the {@link AirplaneModeSettings} should override default user values
     * @param override boolean override
     */
    public void setOverride(boolean override) {
        mOverride = override;
        mDirty = true;
    }

    /**
     * Check whether or not the {@link AirplaneModeSettings} overrides user settings.
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
            int current = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0);
            if (current != mValue) {
                Settings.Global.putInt(context.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, mValue);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", mValue == 1);
                context.sendBroadcast(intent);
            }
        }
    }

    /** @hide */
    public static AirplaneModeSettings fromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException, IOException {
        int event = xpp.next();
        AirplaneModeSettings airplaneModeDescriptor = new AirplaneModeSettings();
        while ((event != XmlPullParser.END_TAG && event != XmlPullParser.END_DOCUMENT) ||
                !xpp.getName().equals("airplaneModeDescriptor")) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("value")) {
                    airplaneModeDescriptor.mValue = Integer.parseInt(xpp.nextText());
                } else if (name.equals("override")) {
                    airplaneModeDescriptor.mOverride = Boolean.parseBoolean(xpp.nextText());
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while parsing airplane mode settings");
            }
            event = xpp.next();
        }
        return airplaneModeDescriptor;
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append("<airplaneModeDescriptor>\n<value>");
        builder.append(mValue);
        builder.append("</value>\n<override>");
        builder.append(mOverride);
        builder.append("</override>\n</airplaneModeDescriptor>\n");
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
        dest.writeInt(mValue);
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
            mValue = in.readInt();
            mDirty = in.readInt() != 0;
        }

        in.setDataPosition(startPosition + parcelableSize);
    }
}
