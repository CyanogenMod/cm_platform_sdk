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
import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;

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
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(in);
        int parcelableVersion = parcelInfo.getParcelVersion();

        if (parcelableVersion >= Build.CM_VERSION_CODES.APRICOT) {
            mName = in.readString();
            mPackage = in.readString();
            mDownloadUri = in.readParcelable(Uri.class.getClassLoader());
            mThumbnailUri = in.readParcelable(Uri.class.getClassLoader());
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(out);

        out.writeString(mName);
        out.writeString(mPackage);
        out.writeParcelable(mDownloadUri, flags);
        out.writeParcelable(mThumbnailUri, flags);

        // Complete the parcel info for the concierge
        parcelInfo.complete();
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
