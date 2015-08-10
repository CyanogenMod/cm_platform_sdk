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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.WindowManagerPolicy;
import android.view.WindowManagerPolicyControl;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.PolicyManager;
import cyanogenmod.app.Profile;
import cyanogenmod.os.Build;

/**
 * The {@link LockSettings} class allows for overriding and setting the
 * current Lock screen state/security level. Value should be one a constant from
 * of {@link Profile.LockMode}
 *
 * <p>Example for disabling lockscreen security:
 * <pre class="prettyprint">
 * LockSettings lock = new LockSettings(Profile.LockMode.INSECURE);
 * profile.setScreenLockMode(lock);
 * </pre>
 */
public final class LockSettings implements Parcelable {

    private static final String TAG = LockSettings.class.getSimpleName();

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
     * Construct a {@link LockSettings} with a default value of {@link Profile.LockMode.DEFAULT}.
     */
    public LockSettings() {
        this(Profile.LockMode.DEFAULT);
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
     * @return a constant from {@link Profile.LockMode}
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Set the value for the {@link LockSettings}
     *
     * see {@link Profile.LockMode}
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
    public void processOverride(Context context, IKeyguardService keyguard) {
        boolean enable;
        final DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager != null && devicePolicyManager.requireSecureKeyguard()) {
            enable = true;
        } else {
            switch (mValue) {
                default:
                case Profile.LockMode.DEFAULT:
                case Profile.LockMode.INSECURE:
                    enable = true;
                    break;
                case Profile.LockMode.DISABLE:
                    enable = false;
                    break;
            }
        }

        try {
            keyguard.setKeyguardEnabled(enable);
        } catch (RemoteException e) {
            Log.w(TAG, "unable to set keyguard enabled state to: " + enable, e);
        }
    }

    /** @hide */
    public void writeXmlString(StringBuilder builder, Context context) {
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
