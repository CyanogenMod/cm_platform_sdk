/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cyanogenmod.app.suggest;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import cyanogenmod.os.Build;

/**
 * @hide
 */
public class ApplicationSuggestion implements Parcelable {

    public static final Creator<ApplicationSuggestion> CREATOR =
            new Creator<ApplicationSuggestion>() {
                public ApplicationSuggestion createFromParcel(Parcel in) {
                    return new ApplicationSuggestion(in);
                }

                public ApplicationSuggestion[] newArray(int size) {
                    return new ApplicationSuggestion[size];
                }
            };

    private String mName;

    private String mPackage;

    private Uri mDownloadUri;

    private Uri mThumbnailUri;

    public ApplicationSuggestion(@NonNull String name, @NonNull String pkg,
            @NonNull Uri downloadUri, @NonNull Uri thumbnailUri) {
        mName = name;
        mPackage = pkg;
        mDownloadUri = downloadUri;
        mThumbnailUri = thumbnailUri;
    }

    private ApplicationSuggestion(Parcel in) {
        // Read parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        int parcelableVersion = in.readInt();
        int parcelableSize = in.readInt();
        int startPosition = in.dataPosition();

        if (parcelableVersion >= Build.CM_VERSION_CODES.APRICOT) {
            mName = in.readString();
            mPackage = in.readString();
            mDownloadUri = in.readParcelable(Uri.class.getClassLoader());
            mThumbnailUri = in.readParcelable(Uri.class.getClassLoader());
        }

        in.setDataPosition(startPosition + parcelableSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // Write parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        out.writeInt(Build.PARCELABLE_VERSION);

        // Inject a placeholder that will store the parcel size from this point on
        // (not including the size itself).
        int sizePosition = out.dataPosition();
        out.writeInt(0);
        int startPosition = out.dataPosition();

        out.writeString(mName);
        out.writeString(mPackage);
        out.writeParcelable(mDownloadUri, flags);
        out.writeParcelable(mThumbnailUri, flags);

        // Go back and write size
        int parcelableSize = out.dataPosition() - startPosition;
        out.setDataPosition(sizePosition);
        out.writeInt(parcelableSize);
        out.setDataPosition(startPosition + parcelableSize);
    }

    public String getName() {
        return mName;
    }

    public String getPackageName() {
        return mPackage;
    }

    public Uri getDownloadUri() {
        return mDownloadUri;
    }

    public Uri getThumbailUri() {
        return mThumbnailUri;
    }
}
