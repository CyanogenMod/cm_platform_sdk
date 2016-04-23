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

package cyanogenmod.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import cyanogenmod.os.Build;
import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;

public final class AudioSessionInfo implements Parcelable {

    /**
     * Unique session id
     */
    public final int mSessionId;
    /**
     * Stream type - see audio_stream_type_t
     */
    public final int mStream;
    /**
     * Output flags - see audio_output_flags_t
     */
    public final int mFlags;
    /**
     * Channel mask - see audio_channel_mask_t
     */
    public final int mChannelMask;
    /**
     * UID of the source application
     */
    public final int mUid;

    public AudioSessionInfo(int sessionId, int stream, int flags, int channelMask, int uid) {
        mSessionId = sessionId;
        mStream = stream;
        mFlags = flags;
        mChannelMask = channelMask;
        mUid = uid;
    }

    private AudioSessionInfo(Parcel in) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(in);
        int parcelableVersion = parcelInfo.getParcelVersion();

        mSessionId = in.readInt();
        mStream = in.readInt();
        mFlags = in.readInt();
        mChannelMask = in.readInt();
        mUid = in.readInt();

        if (parcelableVersion > Build.CM_VERSION_CODES.FIG) {
            // next-gen mind-altering shit goes here
        }
    }

    @Override
    public String toString() {
        return String.format(
                "audioSessionInfo[sessionId=%d, stream=%d, flags=%d, channelMask=%d, uid=%d",
                mSessionId, mStream, mFlags, mChannelMask, mUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSessionId, mStream, mFlags, mChannelMask, mUid);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AudioSessionInfo)) {
            return false;
        }

        // sessions are unique per id
        if (mSessionId == ((AudioSessionInfo)obj).mSessionId) {
            return true;
        }

        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(dest);

        dest.writeInt(mSessionId);
        dest.writeInt(mStream);
        dest.writeInt(mFlags);
        dest.writeInt(mChannelMask);
        dest.writeInt(mUid);

        parcelInfo.complete();
    }

    public static final Creator<AudioSessionInfo> CREATOR = new Creator<AudioSessionInfo>() {

        @Override
        public AudioSessionInfo createFromParcel(Parcel source) {
            return new AudioSessionInfo(source);
        }

        @Override
        public AudioSessionInfo[] newArray(int size) {
            return new AudioSessionInfo[size];
        }
    };
}


