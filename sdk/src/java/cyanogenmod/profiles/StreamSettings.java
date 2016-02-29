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

import cyanogenmod.os.Build;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

/**
 * The {@link StreamSettings} class allows for creating various {@link android.media.AudioManager}
 * overrides on the device depending on their capabilities.
 *
 * <p>Example for setting the alarm stream defaults and override:
 * <pre class="prettyprint">
 * StreamSettings alarmStreamSettings = new StreamSettings(AudioManager.STREAM_ALARM,
 *         am.getStreamVolume(AudioManager.STREAM_ALARM), true));
 * profile.setStreamSettings(alarmStreamSettings);
 * </pre>
 */
public final class StreamSettings implements Parcelable{

    private int mStreamId;
    private int mValue;
    private boolean mOverride;
    private boolean mDirty;

    /** @hide */
    public static final Parcelable.Creator<StreamSettings> CREATOR =
            new Parcelable.Creator<StreamSettings>() {
        public StreamSettings createFromParcel(Parcel in) {
            return new StreamSettings(in);
        }

        @Override
        public StreamSettings[] newArray(int size) {
            return new StreamSettings[size];
        }
    };

    /**
     * Unwrap {@link StreamSettings} from a parcel.
     * @param parcel
     */
    public StreamSettings(Parcel parcel) {
        readFromParcel(parcel);
    }

    /**
     * Construct a {@link StreamSettings} with a stream id and default states.
     * @param streamId ex: {@link android.media.AudioManager#STREAM_ALARM}
     */
    public StreamSettings(int streamId) {
        this(streamId, 0, false);
    }

    /**
     * Construct a {@link StreamSettings} with a stream id, default value,
     * and if the setting should override the user defaults.
     * @param streamId ex: {@link android.media.AudioManager#STREAM_ALARM}
     * @param value default value for the {@link StreamSettings}
     * @param override whether or not the {@link StreamSettings} should override user defaults
     */
    public StreamSettings(int streamId, int value, boolean override) {
        mStreamId = streamId;
        mValue = value;
        mOverride = override;
        mDirty = false;
    }

    /**
     * Retrieve the stream id id associated with the {@link StreamSettings}
     * @return an integer identifier
     */
    public int getStreamId() {
        return mStreamId;
    }

    /**
     * Get the default value for the {@link StreamSettings}
     * @return integer value corresponding with its state
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Set the default value for the {@link StreamSettings}
     * @param value see {@link android.media.AudioManager} for viable values
     */
    public void setValue(int value) {
        mValue = value;
        mDirty = true;
    }

    /**
     * Set whether or not the {@link StreamSettings} should override default user values
     * @param override boolean override
     */
    public void setOverride(boolean override) {
        mOverride = override;
        mDirty = true;
    }

    /**
     * Check whether or not the {@link StreamSettings} overrides user settings.
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
    public static StreamSettings fromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException, IOException {
        int event = xpp.next();
        StreamSettings streamDescriptor = new StreamSettings(0);
        while (event != XmlPullParser.END_TAG || !xpp.getName().equals("streamDescriptor")) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("streamId")) {
                    streamDescriptor.mStreamId = Integer.parseInt(xpp.nextText());
                } else if (name.equals("value")) {
                    streamDescriptor.mValue = Integer.parseInt(xpp.nextText());
                } else if (name.equals("override")) {
                    streamDescriptor.mOverride = Boolean.parseBoolean(xpp.nextText());
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while parsing stream settings");
            }
            event = xpp.next();
        }
        return streamDescriptor;
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append("<streamDescriptor>\n<streamId>");
        builder.append(mStreamId);
        builder.append("</streamId>\n<value>");
        builder.append(mValue);
        builder.append("</value>\n<override>");
        builder.append(mOverride);
        builder.append("</override>\n</streamDescriptor>\n");
        mDirty = false;
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
        dest.writeInt(mStreamId);
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
            mStreamId = in.readInt();
            mOverride = in.readInt() != 0;
            mValue = in.readInt();
            mDirty = in.readInt() != 0;
        }

        in.setDataPosition(startPosition + parcelableSize);
    }
}
