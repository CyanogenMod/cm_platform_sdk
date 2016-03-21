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
import cyanogenmod.providers.WeatherContract;

/**
 * This class holds the information of a request submitted to the active weather provider service
 */
public final class RequestInfo implements Parcelable {

    private Location mLocation;
    private String mCityName;
    private WeatherLocation mWeatherLocation;
    private int mRequestType;
    private IRequestInfoListener mListener;
    private int mTempUnit;
    private int mKey;
    private boolean mIsQueryOnly;

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
        private int mTempUnit = WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT;
        private boolean mIsQueryOnly = false;

        public Builder(IRequestInfoListener listener) {
            this.mListener = listener;
        }

        /**
         * Sets the city name and identifies this request as a {@link #TYPE_LOOKUP_CITY_NAME_REQ}
         * request. If set, will null out the location and weather location.
         */
        public Builder setCityName(String cityName) {
            this.mCityName = cityName;
            this.mRequestType = TYPE_LOOKUP_CITY_NAME_REQ;
            this.mLocation = null;
            this.mWeatherLocation = null;
            return this;
        }

        /**
         * Sets the Location and identifies this request as a {@link #TYPE_GEO_LOCATION_REQ}. If
         * set, will null out the city name and weather location.
         */
        public Builder setLocation(Location location) {
            this.mLocation = location;
            this.mCityName = null;
            this.mWeatherLocation = null;
            this.mRequestType = TYPE_GEO_LOCATION_REQ;
            return this;
        }

        /**
         * Sets the weather location and identifies this request as a
         * {@link #TYPE_WEATHER_LOCATION_REQ}. If set, will null out the location and city name
         */
        public Builder setWeatherLocation(WeatherLocation weatherLocation) {
            this.mWeatherLocation = weatherLocation;
            this.mLocation = null;
            this.mCityName = null;
            this.mRequestType = TYPE_WEATHER_LOCATION_REQ;
            return this;
        }

        /**
         * Sets the unit in which the temperature will be reported if the request is honored.
         * Valid values are:
         * <ul>
         * {@link cyanogenmod.providers.WeatherContract.WeatherColumns.TempUnit#CELSIUS}
         * {@link cyanogenmod.providers.WeatherContract.WeatherColumns.TempUnit#FAHRENHEIT}
         * </ul>
         * Any other value will generate an IllegalArgumentException. If the temperature unit is not
         * set, the default will be degrees Fahrenheit
         * @param unit A valid temperature unit
         */
        public Builder setTemperatureUnit(int unit) {
            if (!isValidTempUnit(unit)) {
                throw new IllegalArgumentException("Invalid temperature unit");
            }
            this.mTempUnit = unit;
            return this;
        }

        /**
         * If this is a weather request, marks the request as a query only, meaning that the
         * content provider won't be updated after the active weather service has finished
         * processing the request.
         */
        public Builder queryOnly() {
            switch (mRequestType) {
                case TYPE_GEO_LOCATION_REQ:
                case TYPE_WEATHER_LOCATION_REQ:
                    this.mIsQueryOnly = true;
                    break;
                default:
                    this.mIsQueryOnly = false;
                    break;
            }
            return this;
        }

        public RequestInfo build() {
            RequestInfo info = new RequestInfo();
            info.mListener = this.mListener;
            info.mRequestType = this.mRequestType;
            info.mCityName = this.mCityName;
            info.mWeatherLocation = this.mWeatherLocation;
            info.mLocation = this.mLocation;
            info.mTempUnit = this.mTempUnit;
            info.mIsQueryOnly = this.mIsQueryOnly;
            info.mKey = this.hashCode();
            return info;
        }

        private boolean isValidTempUnit(int unit) {
            switch (unit) {
                case WeatherContract.WeatherColumns.TempUnit.CELSIUS:
                case WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT:
                    return true;
                default:
                    return false;
            }
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
                    mTempUnit = parcel.readInt();
                    break;
                case TYPE_WEATHER_LOCATION_REQ:
                    mWeatherLocation = WeatherLocation.CREATOR.createFromParcel(parcel);
                    mTempUnit = parcel.readInt();
                    break;
                case TYPE_LOOKUP_CITY_NAME_REQ:
                    mCityName = parcel.readString();
                    break;
            }
            mIsQueryOnly = (parcel.readInt() == 1);
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
     * @return the city name if this is a lookup request, null otherwise
     */
    public String getCityName() {
        return mCityName;
    }

    /**
     * @return the temperature unit if this is a weather request, -1 otherwise
     */
    public int getTemperatureUnit() {
        switch (mRequestType) {
            case TYPE_GEO_LOCATION_REQ:
            case TYPE_WEATHER_LOCATION_REQ:
                return mTempUnit;
            default:
                return -1;
        }
    }

    /**
     * @return if this is a weather request, whether the request will update the content provider.
     * False for other kind of requests
     * @hide
     */
    public boolean isQueryOnlyWeatherRequest() {
        switch (mRequestType) {
            case TYPE_GEO_LOCATION_REQ:
            case TYPE_WEATHER_LOCATION_REQ:
                return mIsQueryOnly;
            default:
                return false;
        }
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
                dest.writeInt(mTempUnit);
                break;
            case TYPE_WEATHER_LOCATION_REQ:
                mWeatherLocation.writeToParcel(dest, 0);
                dest.writeInt(mTempUnit);
                break;
            case TYPE_LOOKUP_CITY_NAME_REQ:
                dest.writeString(mCityName);
                break;
        }
        dest.writeInt(mIsQueryOnly == true ? 1 : 0);
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
                builder.append(" Temp Unit: ");
                if (mTempUnit == WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT) {
                    builder.append("Fahrenheit");
                } else {
                    builder.append(" Celsius");
                }
                break;
            case TYPE_WEATHER_LOCATION_REQ:
                builder.append("WeatherLocation: ").append(mWeatherLocation);
                builder.append(" Temp Unit: ");
                if (mTempUnit == WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT) {
                    builder.append("Fahrenheit");
                } else {
                    builder.append(" Celsius");
                }
                break;
            case TYPE_LOOKUP_CITY_NAME_REQ:
                builder.append("Lookup City: ").append(mCityName);
                break;
        }
        return builder.append(" }").toString();
    }

    @Override
    public int hashCode() {
        //The hashcode of this object was stored when it was built. This is an
        //immutable object but we need to preserve the hashcode since this object is parcelable and
        //it's reconstructed over IPC, and clients of this object might want to store it in a
        //collection that relies on this code to identify the object
        return mKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RequestInfo) {
            RequestInfo info = (RequestInfo) obj;
            return (info.hashCode() == this.mKey);
        }
        return false;
    }
}
