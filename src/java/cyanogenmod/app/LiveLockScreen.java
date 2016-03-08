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

import cyanogenmod.os.Build;

/** @hide */
public class LiveLockScreen implements Parcelable {

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
     * The component, which implements {@link cyanogenmod.externalviews.KeyguardExternalViewProviderService}, to display
     * for this live lock screen.
     */
    public ComponentName component;

    /**
     * Relative priority for this Live lock screen.
     */
    public int priority;

    public LiveLockScreen(@NonNull ComponentName component, int priority) {
        this.component = component;
        this.priority = priority;
    }

    private LiveLockScreen(Parcel source) {
        // Read parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        int version = source.readInt();
        int size = source.readInt();
        int start = source.dataPosition();

        this.priority = source.readInt();
        this.component = ComponentName.unflattenFromString(source.readString());

        source.setDataPosition(start + size);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        dest.writeInt(Build.PARCELABLE_VERSION);
        int sizePos = dest.dataPosition();
        // Inject a placeholder that will store the parcel size from this point on
        // (not including the size itself).
        dest.writeInt(0);
        int dataStartPos = dest.dataPosition();

        dest.writeInt(priority);
        dest.writeString(component.flattenToString());

        // Go back and write size
        int size = dest.dataPosition() - dataStartPos;
        dest.setDataPosition(sizePos);
        dest.writeInt(size);
        dest.setDataPosition(dataStartPos + size);
    }

    public static final Parcelable.Creator<LiveLockScreen> CREATOR = new Parcelable.Creator<LiveLockScreen>() {

        @Override
        public LiveLockScreen createFromParcel(Parcel source) {
            return null;
        }

        @Override
        public LiveLockScreen[] newArray(int size) {
            return new LiveLockScreen[0];
        }
    };

    public static class Builder {
        private int mPriority;
        private ComponentName mComponent;

        public Builder setPriority(int priority) {
            mPriority = priority;
            return this;
        }

        public Builder setComponent(@NonNull ComponentName component) {
            mComponent = component;
            return this;
        }

        public LiveLockScreen build() {
            return new LiveLockScreen(mComponent, mPriority);
        }
    }
}
