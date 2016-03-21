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
import cyanogenmod.os.Build;

/**
 * A class representing a geographical location that a weather service provider can use to
 * get weather data from. Each service provider will potentially populate objects of this class
 * with different content, so make sure you don't preserve the values when a service provider
 * is changed
 */
public final class WeatherLocation implements Parcelable{
    private String mId;
    private String mCity;
    private String mPostal;
    private String mCountryId;
    private String mCountry;

    public WeatherLocation(String id, String city, String postal, String countryId, String country) {
        mId = id;
        mCity = city;
        mPostal = postal;
        mCountryId = countryId;
        mCountry = country;
    }

    public String getCityId() {
        return mId;
    }

    public String getCity() {
        return mCity;
    }

    public String getPostalCode() {
        return mPostal;
    }

    public String getCountryId() {
        return mCountryId;
    }

    public String getCountry() {
        return mCountry;
    }

    private WeatherLocation(Parcel in) {
        int parcelableVersion = in.readInt();
        int parcelableSize = in.readInt();
        int startPosition = in.dataPosition();
        if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
            mId = in.readString();
            mCity = in.readString();
            mPostal = in.readString();
            mCountryId = in.readString();
            mCountry = in.readString();
        }
        in.setDataPosition(startPosition + parcelableSize);
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
        dest.writeInt(Build.PARCELABLE_VERSION);

        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();

        // ==== ELDERBERRY =====
        dest.writeString(mId);
        dest.writeString(mCity);
        dest.writeString(mPostal);
        dest.writeString(mCountryId);
        dest.writeString(mCountry);

        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{ City ID: ").append(mId)
                .append(" City: ").append(mCity)
                .append(" Postal Code: ").append(mPostal)
                .append(" Country Id: ").append(mCountryId)
                .append(" Country: ").append(mCountry).append("}")
                .toString();
    }
}
