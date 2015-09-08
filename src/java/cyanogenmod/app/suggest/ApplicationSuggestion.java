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

    public ApplicationSuggestion(@NonNull String name,
                                 @NonNull String pkg,
                                 @NonNull Uri downloadUri,
                                 @NonNull Uri thumbnailUri) {
        mName = name;
        mPackage = pkg;
        mDownloadUri = downloadUri;
        mThumbnailUri = thumbnailUri;
    }

    public ApplicationSuggestion(Parcel in) {
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
        out.writeInt(Build.PARCELABLE_VERSION);

        int sizePosition = out.dataPosition();
        out.writeInt(0);
        int startPosition = out.dataPosition();

        out.writeString(mName);
        out.writeString(mPackage);
        out.writeParcelable(mDownloadUri, flags);
        out.writeParcelable(mThumbnailUri, flags);

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
