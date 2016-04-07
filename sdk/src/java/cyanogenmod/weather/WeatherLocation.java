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

import android.os.Parcel;
import android.os.Parcelable;

import android.text.TextUtils;
import cyanogenmod.os.Build;
import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;

/**
 * A class representing a geographical location that a weather service provider can use to
 * get weather data from. Each service provider will potentially populate objects of this class
 * with different content, so make sure you don't preserve the values when a service provider
 * is changed
 */
public final class WeatherLocation implements Parcelable{
    private String mCityId;
    private String mCity;
    private String mState;
    private String mPostal;
    private String mCountryId;
    private String mCountry;
    private int mKey;

    private WeatherLocation() {}

    /**
     * Builder class for {@link WeatherLocation}
     */
    public static class Builder {
        String mCityId = "";
        String mCity = "";
        String mState = "";
        String mPostal = "";
        String mCountryId = "";
        String mCountry = "";

        /**
         * @param cityId An identifier for the city (for example WOEID - Where On Earth IDentifier)
         * @param cityName The name of the city
         */
        public Builder(String cityId, String cityName) {
            if (cityId == null && cityName == null) {
                throw new IllegalArgumentException("Illegal to set city id AND city to null");
            }
            this.mCityId = cityId;
            this.mCity = cityName;
        }

        /**
         * @param countryId An identifier for the country (for example ISO alpha-2, ISO alpha-3,
         *                 ISO 3166-1 numeric-3, etc)
         * @param country The country name
         * @return The {@link Builder} instance
         */
        public Builder setCountry(String countryId, String country) {
            if (countryId == null && country == null) {
                throw new IllegalArgumentException("Illegal to set country id AND country to null");
            }
            this.mCountryId = countryId;
            this.mCountry = country;
            return this;
        }

        /**
         * @param postalCode The postal/ZIP code
         * @return The {@link Builder} instance
         */
        public Builder setPostalCode(String postalCode) {
            if (postalCode == null) {
                throw new IllegalArgumentException("Postal code/ZIP can't be null");
            }
            this.mPostal = postalCode;
            return this;
        }

        /**
         * @param state The state or territory where the city is located
         * @return The {@link Builder} instance
         */
        public Builder setState(String state) {
            if (state == null) {
                throw new IllegalArgumentException("State can't be null");
            }
            this.mState = state;
            return this;
        }

        /**
         * Combine all of the options that have been set and return a new {@link WeatherLocation}
         * object
         * @return {@link WeatherLocation}
         */
        public WeatherLocation build() {
            WeatherLocation weatherLocation = new WeatherLocation();
            weatherLocation.mCityId = this.mCityId;
            weatherLocation.mCity = this.mCity;
            weatherLocation.mState = this.mState;
            weatherLocation.mPostal = this.mPostal;
            weatherLocation.mCountryId = this.mCountryId;
            weatherLocation.mCountry = this.mCountry;
            weatherLocation.mKey = this.hashCode();
            return weatherLocation;
        }
    }

    /**
     * @return The city ID
     */
    public String getCityId() {
        return mCityId;
    }

    /**
     * @return The city name
     */
    public String getCity() {
        return mCity;
    }

    /**
     * @return The state name
     */
    public String getState() {
        return mState;
    }

    /**
     * @return The postal/ZIP code
     */
    public String getPostalCode() {
        return mPostal;
    }

    /**
     * @return The country ID
     */
    public String getCountryId() {
        return mCountryId;
    }

    /**
     * @return The country name
     */
    public String getCountry() {
        return mCountry;
    }

    private WeatherLocation(Parcel in) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(in);
        int parcelableVersion = parcelInfo.getParcelVersion();

        if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
            mKey = in.readInt();
            mCityId = in.readString();
            mCity = in.readString();
            mState = in.readString();
            mPostal = in.readString();
            mCountryId = in.readString();
            mCountry = in.readString();
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    public static final Creator<WeatherLocation> CREATOR = new Creator<WeatherLocation>() {
        @Override
        public WeatherLocation createFromParcel(Parcel in) {
            return new WeatherLocation(in);
        }

        @Override
        public WeatherLocation[] newArray(int size) {
            return new WeatherLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(dest);

        // ==== ELDERBERRY =====
        dest.writeInt(mKey);
        dest.writeString(mCityId);
        dest.writeString(mCity);
        dest.writeString(mState);
        dest.writeString(mPostal);
        dest.writeString(mCountryId);
        dest.writeString(mCountry);

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{ City ID: ").append(mCityId)
                .append(" City: ").append(mCity)
                .append(" State: ").append(mState)
                .append(" Postal/ZIP Code: ").append(mPostal)
                .append(" Country Id: ").append(mCountryId)
                .append(" Country: ").append(mCountry).append("}")
                .toString();
    }

    @Override
    public int hashCode() {
        return mKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WeatherLocation) {
            WeatherLocation info = (WeatherLocation) obj;
            return (info.hashCode() == this.mKey);
        }
        return false;
    }
}
