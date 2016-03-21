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

package cyanogenmod.weather;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import cyanogenmod.os.Build;

/**
 * This class holds the information of a single request submitted to the active weather
 * provider service
 */
public final class RequestInfo implements Parcelable {

    private Location mLocation;
    private String mCityName;
    private WeatherLocation mWeatherLocation;
    private int mRequestType;
    private IRequestInfoListener mListener;
    private int mKey;

    /**
     * A request to update the weather data using a geographical {@link android.location.Location}
     */
    public static final int TYPE_GEO_LOCATION_REQ = 1;
    /**
     * A request to update the weather data using a {@link WeatherLocation}
     */
    public static final int TYPE_WEATHER_LOCATION_REQ = 2;

    /**
     * A request to look up a city name
     */
    public static final int TYPE_LOOKUP_CITY_NAME_REQ = 3;

    private RequestInfo() {}

    /* package */ static class Builder {
        private Location mLocation;
        private String mCityName;
        private WeatherLocation mWeatherLocation;
        private int mRequestType;
        private IRequestInfoListener mListener;

        public Builder(IRequestInfoListener listener) {
            this.mListener = listener;
        }

        public Builder setCityName(String cityName) {
            this.mCityName = cityName;
            this.mRequestType = TYPE_LOOKUP_CITY_NAME_REQ;
            this.mLocation = null;
            this.mWeatherLocation = null;
            return this;
        }

        public Builder setLocation(Location location) {
            this.mLocation = location;
            this.mCityName = null;
            this.mWeatherLocation = null;
            mRequestType = TYPE_GEO_LOCATION_REQ;
            return this;
        }

        public Builder setWeatherLocation(WeatherLocation weatherLocation) {
            this.mWeatherLocation = weatherLocation;
            this.mLocation = null;
            this.mCityName = null;
            mRequestType = TYPE_WEATHER_LOCATION_REQ;
            return this;
        }

        public RequestInfo create() {
            RequestInfo info = new RequestInfo();
            info.mListener = this.mListener;
            info.mRequestType = this.mRequestType;
            info.mCityName = this.mCityName;
            info.mWeatherLocation = this.mWeatherLocation;
            info.mLocation = mLocation;
            info.mKey = info.hashCode();
            return info;
        }
    }

    private RequestInfo(Parcel parcel) {
        int parcelableVersion = parcel.readInt();
        int parcelableSize = parcel.readInt();
        int startPosition = parcel.dataPosition();
        if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
            mKey = parcel.readInt();
            mRequestType = parcel.readInt();
            switch (mRequestType) {
                case TYPE_GEO_LOCATION_REQ:
                    mLocation = Location.CREATOR.createFromParcel(parcel);
                    break;
                case TYPE_WEATHER_LOCATION_REQ:
                    mWeatherLocation = WeatherLocation.CREATOR.createFromParcel(parcel);
                    break;
                case TYPE_LOOKUP_CITY_NAME_REQ:
                    mCityName = parcel.readString();
                    break;
            }
            mListener = IRequestInfoListener.Stub.asInterface(parcel.readStrongBinder());
        }
        parcel.setDataPosition(startPosition + parcelableSize);
    }


    /**
     * @return The request type
     */
    public int getRequestType() {
        return mRequestType;
    }

    /**
     * @return the {@link android.location.Location} if this is a request by location, null
     * otherwise
     */
    public Location getLocation() {
        return mLocation;
    }

    /**
     * @return the {@link cyanogenmod.weather.WeatherLocation} if this is a request by weather
     * location, null otherwise
     */
    public WeatherLocation getWeatherLocation() {
        return mWeatherLocation;
    }

    /**
     * @hide
     */
    public IRequestInfoListener getRequestListener() {
        return mListener;
    }

    /**
     * @hide
     */
    public int getKey() {
        return mKey;
    }

    /**
     * @return the city name if this is a lookup request, null otherwise
     */
    public String getCityName() {
        return mCityName;
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
        dest.writeInt(mKey);
        dest.writeInt(mRequestType);
        switch (mRequestType) {
            case TYPE_GEO_LOCATION_REQ:
                mLocation.writeToParcel(dest, 0);
                break;
            case TYPE_WEATHER_LOCATION_REQ:
                mWeatherLocation.writeToParcel(dest, 0);
                break;
            case TYPE_LOOKUP_CITY_NAME_REQ:
                dest.writeString(mCityName);
                break;
        }
        dest.writeStrongBinder(mListener.asBinder());

        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ Request for ");
        switch (mRequestType) {
            case TYPE_GEO_LOCATION_REQ:
                builder.append("Location: ").append(mLocation);
                break;
            case TYPE_WEATHER_LOCATION_REQ:
                builder.append("WeatherLocation: ").append(mWeatherLocation);
                break;
            case TYPE_LOOKUP_CITY_NAME_REQ:
                builder.append("Lookup City: ").append(mCityName);
                break;
        }
        return builder.append(" }").toString();
    }
}
