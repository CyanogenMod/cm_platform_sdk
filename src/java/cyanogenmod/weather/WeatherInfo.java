/*
 * Copyright (C) 2016 The CyanongenMod Project
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

import java.util.ArrayList;

public class WeatherInfo implements Parcelable {

    private String mCityId;
    private String mCity;
    private String mCondition;
    private int mConditionCode;
    private float mTemperature;
    private String mTempUnit;
    private float mHumidity;
    private float mWindSpeed;
    private int mWindDirection;
    private String mWindSpeedUnit;
    private long mTimestamp;
    private ArrayList<DayForecast> mForecastList;

    public WeatherInfo(String cityId, String city, String condition, int conditionCode, float temp,
                       String tempUnit, float humidity, float wind, int windDir,
                       String speedUnit, ArrayList<DayForecast> forecasts, long timestamp) {
        this.mCityId = cityId;
        this.mCity = city;
        this.mCondition = condition;
        this.mConditionCode = conditionCode;
        this.mHumidity = humidity;
        this.mWindSpeed = wind;
        this.mWindDirection = windDir;
        this.mWindSpeedUnit = speedUnit;
        this.mTimestamp = timestamp;
        this.mTemperature = temp;
        this.mTempUnit = tempUnit;
        this.mForecastList = forecasts;
    }

    public String getCityId() {
        return mCityId;
    }

    public String getCity() {
        return mCity;
    }

    public int getConditionCode() {
        return mConditionCode;
    }

    public String getCondition() {
        //TODO: How to deal with locales??
        return mCondition;
    }

    public float getHumidity() {
        return mHumidity;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public int getWindDirection() {
        return mWindDirection;
    }

    public float getWindSpeed() {
        return mWindSpeed;
    }

    public String getWindSpeedUnit() {
        return mWindSpeedUnit;
    }

    public float getTemperature() {
        return mTemperature;
    }

    public String getTemperatureUnit() {
        return mTempUnit;
    }

    public ArrayList<DayForecast> getForecasts() {
        return mForecastList;
    }

    private WeatherInfo(Parcel parcel) {
        int parcelableVersion = parcel.readInt();
        int parcelableSize = parcel.readInt();
        int startPosition = parcel.dataPosition();
        if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
            mCityId = parcel.readString();
            mCity = parcel.readString();
            mCondition = parcel.readString();
            mConditionCode = parcel.readInt();
            mTemperature = parcel.readFloat();
            mTempUnit = parcel.readString();
            mHumidity = parcel.readFloat();
            mWindSpeed = parcel.readFloat();
            mWindDirection = parcel.readInt();
            mWindSpeedUnit = parcel.readString();
            mTimestamp = parcel.readLong();
            int forecastListSize = parcel.readInt();
            mForecastList = new ArrayList<>();
            while (forecastListSize > 0) {
                mForecastList.add(DayForecast.CREATOR.createFromParcel(parcel));
                forecastListSize--;
            }
        }
        parcel.setDataPosition(startPosition + parcelableSize);
    }

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
        dest.writeString(mCityId);
        dest.writeString(mCity);
        dest.writeString(mCondition);
        dest.writeInt(mConditionCode);
        dest.writeFloat(mTemperature);
        dest.writeString(mTempUnit);
        dest.writeFloat(mHumidity);
        dest.writeFloat(mWindSpeed);
        dest.writeInt(mWindDirection);
        dest.writeString(mWindSpeedUnit);
        dest.writeLong(mTimestamp);
        dest.writeInt(mForecastList.size());
        for (DayForecast dayForecast : mForecastList) {
            dayForecast.writeToParcel(dest, 0);
        }

        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Parcelable.Creator<WeatherInfo> CREATOR =
            new Parcelable.Creator<WeatherInfo>() {

                @Override
                public WeatherInfo createFromParcel(Parcel source) {
                    return new WeatherInfo(source);
                }

                @Override
                public WeatherInfo[] newArray(int size) {
                    return new WeatherInfo[size];
                }
            };

    public static class DayForecast implements Parcelable{
        public float mLow, mHigh;
        public int mConditionCode;
        public String mCondition;

        public DayForecast(float low, float high, String condition, int conditionCode) {
            this.mLow = low;
            this.mHigh = high;
            this.mCondition = condition;
            this.mConditionCode = conditionCode;
        }

        public float getLow() {
            return mLow;
        }

        public float getHigh() {
            return mHigh;
        }

        public String getCondition() {
            return mCondition;
        }

        public int getConditionCode() {
            return mConditionCode;
        }

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
            dest.writeFloat(mLow);
            dest.writeFloat(mHigh);
            dest.writeInt(mConditionCode);
            dest.writeString(mCondition);

            int parcelableSize = dest.dataPosition() - startPosition;
            dest.setDataPosition(sizePosition);
            dest.writeInt(parcelableSize);
            dest.setDataPosition(startPosition + parcelableSize);
        }

        public static final Parcelable.Creator<DayForecast> CREATOR =
                new Parcelable.Creator<DayForecast>() {

                    @Override
                    public DayForecast createFromParcel(Parcel source) {
                        return new DayForecast(source);
                    }

                    @Override
                    public DayForecast[] newArray(int size) {
                        return new DayForecast[size];
                    }
                };

        private DayForecast(Parcel parcel) {
            int parcelableVersion = parcel.readInt();
            int parcelableSize = parcel.readInt();
            int startPosition = parcel.dataPosition();
            if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
                mLow = parcel.readFloat();
                mHigh = parcel.readFloat();
                mConditionCode = parcel.readInt();
                mCondition = parcel.readString();
            }
            parcel.setDataPosition(startPosition + parcelableSize);
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("{Low temp: ").append(mLow)
                    .append(" High temp: ").append(mHigh)
                    .append(" Condition code: ").append(mConditionCode)
                    .append(" Condition: ").append(mCondition)
                    .append("}").toString();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
            .append("{CityId: ").append(mCityId)
            .append(" City Name: ").append(mCity)
            .append(" Condition: ").append(mCondition)
            .append(" Condition Code: ").append(mConditionCode)
            .append(" Temperature: ").append(mTemperature)
            .append(" Temperature Unit: ").append(mTempUnit)
            .append(" Humidity: ").append(mHumidity)
            .append(" Wind speed: ").append(mWindSpeed)
            .append(" Wind direction: ").append(mWindDirection)
            .append(" Wind Speed Unit: ").append(mWindSpeedUnit)
            .append(" Timestamp: ").append(mTimestamp).append(" Forecasts: [");
        for (DayForecast dayForecast : mForecastList) {
            builder.append(dayForecast.toString());
        }
        return builder.append("]}").toString();
    }
}