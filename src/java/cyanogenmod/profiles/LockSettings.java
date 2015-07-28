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
import android.os.Parcel;
import android.os.Parcelable;
import android.view.WindowManagerPolicy;
import com.android.internal.policy.PolicyManager;
import cyanogenmod.os.Build;

/**
 * The {@link LockSettings} class allows for overriding and setting the
 * current Lock screen state/security level. Value should be one a constant from
 * of {@link cyanogenmod.app.Profile.LockMode}
 */
public final class LockSettings implements Parcelable {

    private int mValue;
    private boolean mDirty;

    /** @hide */
    public static final Creator<LockSettings> CREATOR
            = new Creator<LockSettings>() {
        public LockSettings createFromParcel(Parcel in) {
            return new LockSettings(in);
        }

        @Override
        public LockSettings[] newArray(int size) {
            return new LockSettings[size];
        }
    };

    /**
     * Unwrap {@link LockSettings} from a parcel.
     * @param parcel
     */
    public LockSettings(Parcel parcel) {
        readFromParcel(parcel);
    }

    /**
     * Construct a {@link LockSettings} with a default value of 0.
     */
    public LockSettings() {
        this(cyanogenmod.app.Profile.LockMode.DEFAULT);
    }

    /**
     * Construct a {@link LockSettings} with a default value.
     */
    public LockSettings(int value) {
        mValue = value;
        mDirty = false;
    }

    /**
     * Get the value for the {@link LockSettings}
     * @return a constant from {@link cyanogenmod.app.Profile.LockMode}
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Set the value for the {@link LockSettings}
     *
     * see {@link cyanogenmod.app.Profile.LockMode}
     */
    public void setValue(int value) {
        mValue = value;
        mDirty = true;
    }

    /** @hide */
    public boolean isDirty() {
        return mDirty;
    }

    /** @hide */
    public void processOverride(Context context) {
        final WindowManagerPolicy policy = PolicyManager.makeNewWindowManager();
        switch (mValue) {
            case cyanogenmod.app.Profile.LockMode.DEFAULT:
            case cyanogenmod.app.Profile.LockMode.INSECURE:
                policy.enableKeyguard(true);
                break;
            case cyanogenmod.app.Profile.LockMode.DISABLE:
                policy.enableKeyguard(false);
                break;
        }
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append(mValue);
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
            mValue = in.readInt();
            mDirty = in.readInt() != 0;
        }

        in.setDataPosition(startPosition + parcelableSize);
    }
}
