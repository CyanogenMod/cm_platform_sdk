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

package cyanogenmod.app;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

import android.text.TextUtils;

import cyanogenmod.os.Build;
import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;

/**
 * Data structure defining a Live lock screen.
 */
public class LiveLockScreenInfo implements Parcelable {

    /**
     * Default Live lock screen {@link #priority}.
     */
    public static final int PRIORITY_DEFAULT = 0;

    /**
     * Lower {@link #priority}, for items that are less important.
     */
    public static final int PRIORITY_LOW = -1;

    /**
     * Lowest {@link #priority}.
     */
    public static final int PRIORITY_MIN = -2;

    /**
     * Higher {@link #priority}, for items that are more important
     */
    public static final int PRIORITY_HIGH = 1;

    /**
     * Highest {@link #priority}.
     */
    public static final int PRIORITY_MAX = 2;

    /**
     * The component, which implements
     * {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}, to display for this
     * live lock screen.
     */
    public ComponentName component;

    /**
     * Relative priority for this Live lock screen.
     */
    public int priority;

    /**
     * Constructs a LiveLockScreenInfo object with the given values.
     * You might want to consider using {@link Builder} instead.
     */
    public LiveLockScreenInfo(@NonNull ComponentName component, int priority) {
        this.component = component;
        this.priority = priority;
    }

    /**
     * Constructs a LiveLockScreenInfo object with default values.
     * You might want to consider using {@link Builder} instead.
     */
    public LiveLockScreenInfo()
    {
        this.component = null;
        this.priority = PRIORITY_DEFAULT;
    }

    private LiveLockScreenInfo(Parcel source) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(source);
        int parcelableVersion = parcelInfo.getParcelVersion();

        this.priority = source.readInt();
        String component = source.readString();
        this.component = !TextUtils.isEmpty(component)
                ? ComponentName.unflattenFromString(component)
                : null;

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(dest);

        dest.writeInt(priority);
        dest.writeString(component != null ? component.flattenToString() : "");

        // Complete the parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public String toString() {
        return "LiveLockScreenInfo: priority=" + priority +
                ", component=" + component;
    }

    @Override
    public LiveLockScreenInfo clone() {
        LiveLockScreenInfo that = new LiveLockScreenInfo();
        cloneInto(that);
        return that;
    }

    /**
     * Copy all (or if heavy is false, all except Bitmaps and RemoteViews) members
     * of this into that.
     * @hide
     */
    public void cloneInto(LiveLockScreenInfo that) {
        that.component = this.component.clone();
        that.priority = this.priority;
    }

    public static final Parcelable.Creator<LiveLockScreenInfo> CREATOR =
            new Parcelable.Creator<LiveLockScreenInfo>() {
        @Override
        public LiveLockScreenInfo createFromParcel(Parcel source) {
            return new LiveLockScreenInfo(source);
        }

        @Override
        public LiveLockScreenInfo[] newArray(int size) {
            return new LiveLockScreenInfo[0];
        }
    };

    /**
     * Builder class for {@link LiveLockScreenInfo} objects.  Provides a convenient way to set
     * various fields of a {@link LiveLockScreenInfo}.
     */
    public static class Builder {
        private int mPriority;
        private ComponentName mComponent;

        public Builder setPriority(int priority) {
            if (priority < PRIORITY_MIN || priority > PRIORITY_MAX) {
                throw new IllegalArgumentException("Invalid priorty given (" + priority + "): " +
                        PRIORITY_MIN + " <= priority <= " + PRIORITY_MIN);
            }
            mPriority = priority;
            return this;
        }

        public Builder setComponent(@NonNull ComponentName component) {
            if (component == null) {
                throw new IllegalArgumentException(
                        "Cannot call setComponent with a null component");
            }
            mComponent = component;
            return this;
        }

        public LiveLockScreenInfo build() {
            return new LiveLockScreenInfo(mComponent, mPriority);
        }
    }
}
