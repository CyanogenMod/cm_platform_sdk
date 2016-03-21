package cyanogenmod.weather;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import cyanogenmod.os.Build;

/**
 * This class represents a weather update request
 */
public final class RequestInfo implements Parcelable {

    /**
     * Weather update request state: Successfully completed
     */
    public static final int WEATHER_REQUEST_COMPLETED = 1;

    /**
     * Weather update request state: The weather does not change very often. You need to wait
     * a bit longer before requesting an update again
     */
    public static final int WEATHER_REQUEST_SUBMITTED_TOO_SOON = -1;

    /**
     * Weather update request state: An error occurred while trying to update the weather. You
     * should wait before trying again, or your request will be rejected with
     * {@link #WEATHER_REQUEST_SUBMITTED_TOO_SOON}
     */
    public static final int WEATHER_REQUEST_FAILED = -2;

    /**
     * Weather update request state: Only one update request can be processed at a given time.
     */
    public static final int WEATHER_REQUEST_ALREADY_IN_PROGRESS = -3;

    private Location mLocation;
    private String mCityName;
    private String mPackageName;
    private UserHandle mUserHandle;

    private static final int REQUEST_BY_LOCATION = 1;
    private static final int REQUEST_BY_CITY = 2;

    /* package */ RequestInfo(Location location, String packageName) {
        mLocation = location;
        mPackageName = packageName;
        mUserHandle = Process.myUserHandle();
    }

    /* package */ RequestInfo(String cityName, String packageName) {
        mCityName = cityName;
        mPackageName = packageName;
        mUserHandle = Process.myUserHandle();
    }

    private RequestInfo(Parcel parcel) {
        int parcelableVersion = parcel.readInt();
        int parcelableSize = parcel.readInt();
        int startPosition = parcel.dataPosition();
        if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
            mPackageName = parcel.readString();
            int requestType = parcel.readInt();
            if (requestType == REQUEST_BY_CITY) {
                mCityName = parcel.readString();
            } else if (requestType == REQUEST_BY_LOCATION) {
                mLocation = Location.CREATOR.createFromParcel(parcel);
            }
            mUserHandle = UserHandle.CREATOR.createFromParcel(parcel);
        }
        parcel.setDataPosition(startPosition + parcelableSize);
    }

    /**
     * Checks whether this is a request by location
     * @return true if this is a request by location
     */
    public boolean isRequestByLocation() {
        return mLocation != null;
    }

    /**
     * Checks whether this is a request by city
     * @return true if this is a request by city name
     */
    public boolean isRequestByCity() {
        return mCityName != null;
    }

    /**
     * If this is a request by location, returns the location provided when the request was
     * submitted
     *
     * @return the {@link android.location.Location} if this is a request by location, null
     * otherwise
     */
    public Location getLocation() {
        return mLocation;
    }

    /**
     * If this is a request by city, returns the city name provided when the request was submitted
     *
     * @return the city name if this is a request by city, null otherwise
     */
    public String getCityName() {
        return mCityName;
    }

    /**
     * @hide
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * @hide
     */
    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    public static final Creator<RequestInfo> CREATOR = new Creator<RequestInfo>() {
        @Override
        public RequestInfo createFromParcel(Parcel in) {
            return new RequestInfo(in);
        }

        @Override
        public RequestInfo[] newArray(int size) {
            return new RequestInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(Build.PARCELABLE_VERSION);

        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();

        // ==== ELDERBERRY =====
        dest.writeString(mPackageName);
        if (isRequestByLocation()) {
            dest.writeInt(REQUEST_BY_LOCATION);
            mLocation.writeToParcel(dest, 0);
        } else if (isRequestByCity()) {
            dest.writeInt(REQUEST_BY_CITY);
            dest.writeString(mCityName);
        }
        mUserHandle.writeToParcel(dest, 0);

        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Weather Request for ");
        if (isRequestByCity()) {
            builder.append("City [name: ");
            builder.append(mCityName);
            builder.append("]");
        } else if (isRequestByLocation()) {
            builder.append("Location [");
            builder.append(mLocation.toString());
            builder.append("] ");
        }
        return builder.toString();
    }
}
