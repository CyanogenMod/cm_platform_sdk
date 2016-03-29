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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import cyanogenmod.os.Build;
import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;
import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.ServiceRequestResult;

import java.util.ArrayList;

/**
 * This class represents the weather information that a
 * {@link cyanogenmod.weatherservice.WeatherProviderService} will use to update the weather content
 * provider. A weather provider service will be called by the system to process an update
 * request at any time. If the service successfully processes the request, then the weather provider
 * service is responsible of calling
 * {@link ServiceRequest#complete(ServiceRequestResult)} to notify the
 * system that the request was completed and that the weather content provider should be updated
 * with the supplied weather information.
 */
public final class WeatherInfo implements Parcelable {

    private String mCityId;
    private String mCity;
    private int mConditionCode;
    private float mTemperature;
    private int mTempUnit;
    private float mHumidity;
    private float mWindSpeed;
    private float mWindDirection;
    private int mWindSpeedUnit;
    private long mTimestamp;
    private ArrayList<DayForecast> mForecastList;
    int mKey;

    private WeatherInfo() {}

    public static class Builder {
        private String mCityId;
        private String mCity;
        private int mConditionCode;
        private float mTemperature;
        private int mTempUnit;
        private float mHumidity;
        private float mWindSpeed;
        private float mWindDirection;
        private int mWindSpeedUnit;
        private long mTimestamp;
        private ArrayList<DayForecast> mForecastList;

        public Builder(long timestamp) {
            mTimestamp = timestamp;
        }

        public Builder setCity(String cityId, @NonNull String cityName) {
            if (cityName == null || cityId == null) {
                throw new IllegalArgumentException("City name and id can't be null");
            }
            mCityId = cityId;
            mCity = cityName;
            return this;
        }

        public Builder setTemperature(float temperature, int tempUnit) {
            if (!isValidTempUnit(tempUnit)) {
                throw new IllegalArgumentException("Invalid temperature unit");
            }

            if (Float.isNaN(temperature)) {
                throw new IllegalArgumentException("Invalid temperature value");
            }

            mTemperature = temperature;
            mTempUnit = tempUnit;
            return this;
        }

        public Builder setHumidity(float humidity) {
            if (Float.isNaN(humidity)) {
                throw new IllegalArgumentException("Invalid humidity value");
            }

            mHumidity = humidity;
            return this;
        }

        public Builder setWind(float windSpeed, float windDirection, int windSpeedUnit) {
            if (Float.isNaN(windSpeed)) {
                throw new IllegalArgumentException("Invalid wind speed value");
            }
            if (Float.isNaN(windDirection)) {
                throw new IllegalArgumentException("Invalid wind direction value");
            }
            if (!isValidWindSpeedUnit(windSpeedUnit)) {
                throw new IllegalArgumentException("Invalid speed unit");
            }
            mWindSpeed = windSpeed;
            mWindSpeedUnit = windSpeedUnit;
            mWindDirection = windDirection;
            return this;
        }

        public Builder setWeatherCondition(int conditionCode) {
            if (!isValidWeatherCode(conditionCode)) {
                throw new IllegalArgumentException("Invalid weather condition code");
            }
            mConditionCode = conditionCode;
            return this;
        }

        public Builder setForecast(@NonNull ArrayList<DayForecast> forecasts) {
            if (forecasts == null) {
                throw new IllegalArgumentException("Forecast list can't be null");
            }
            mForecastList = forecasts;
            return this;
        }

        public WeatherInfo build() {
            WeatherInfo info = new WeatherInfo();
            info.mCityId = this.mCityId;
            info.mCity = this.mCity;
            info.mConditionCode = this.mConditionCode;
            info.mTemperature = this.mTemperature;
            info.mTempUnit = this.mTempUnit;
            info.mHumidity = this.mHumidity;
            info.mWindSpeed = this.mWindSpeed;
            info.mWindDirection = this.mWindDirection;
            info.mWindSpeedUnit = this.mWindSpeedUnit;
            info.mTimestamp = this.mTimestamp;
            info.mForecastList = this.mForecastList;
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

        private boolean isValidWindSpeedUnit(int unit) {
            switch (unit) {
                case WeatherContract.WeatherColumns.WindSpeedUnit.KPH:
                case WeatherContract.WeatherColumns.WindSpeedUnit.MPH:
                    return true;
                default:
                    return false;
            }
        }
    }


    private static boolean isValidWeatherCode(int code) {
        if (code < WeatherContract.WeatherColumns.WeatherCode.WEATHER_CODE_MIN
                || code > WeatherContract.WeatherColumns.WeatherCode.WEATHER_CODE_MAX) {
            if (code != WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return city id
     */
    public String getCityId() {
        return mCityId;
    }

    /**
     * @return city name
     */
    public String getCity() {
        return mCity;
    }

    /**
     * @return An implementation specific weather condition code
     */
    public int getConditionCode() {
        return mConditionCode;
    }

    /**
     * @return humidity
     */
    public float getHumidity() {
        return mHumidity;
    }

    /**
     * @return time stamp when the request was processed
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * @return wind direction (degrees)
     */
    public float getWindDirection() {
        return mWindDirection;
    }

    /**
     * @return wind speed
     */
    public float getWindSpeed() {
        return mWindSpeed;
    }

    /**
     * @return wind speed unit
     */
    public int getWindSpeedUnit() {
        return mWindSpeedUnit;
    }

    /**
     * @return current temperature
     */
    public float getTemperature() {
        return mTemperature;
    }

    /**
     * @return temperature unit
     */
    public int getTemperatureUnit() {
        return mTempUnit;
    }

    /**
     * @return List of {@link cyanogenmod.weather.WeatherInfo.DayForecast}
     */
    public ArrayList<DayForecast> getForecasts() {
        return mForecastList;
    }

    private WeatherInfo(Parcel parcel) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
        int parcelableVersion = parcelInfo.getParcelVersion();

        if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
            mKey = parcel.readInt();
            mCityId = parcel.readString();
            mCity = parcel.readString();
            mConditionCode = parcel.readInt();
            mTemperature = parcel.readFloat();
            mTempUnit = parcel.readInt();
            mHumidity = parcel.readFloat();
            mWindSpeed = parcel.readFloat();
            mWindDirection = parcel.readFloat();
            mWindSpeedUnit = parcel.readInt();
            mTimestamp = parcel.readLong();
            int forecastListSize = parcel.readInt();
            mForecastList = new ArrayList<>();
            while (forecastListSize > 0) {
                mForecastList.add(DayForecast.CREATOR.createFromParcel(parcel));
                forecastListSize--;
            }
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

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
        dest.writeInt(mConditionCode);
        dest.writeFloat(mTemperature);
        dest.writeInt(mTempUnit);
        dest.writeFloat(mHumidity);
        dest.writeFloat(mWindSpeed);
        dest.writeFloat(mWindDirection);
        dest.writeInt(mWindSpeedUnit);
        dest.writeLong(mTimestamp);
        dest.writeInt(mForecastList.size());
        for (DayForecast dayForecast : mForecastList) {
            dayForecast.writeToParcel(dest, 0);
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
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

    /**
     * This class represents the weather forecast for a given day
     */
    public static class DayForecast implements Parcelable{
        float mLow;
        float mHigh;
        int mConditionCode;
        int mKey;

        private DayForecast() {}

        public static class Builder {
            float mLow;
            float mHigh;
            int mConditionCode;

            public Builder() {}
            public Builder setHigh(float high) {
                if (Float.isNaN(high)) {
                    throw new IllegalArgumentException("Invalid high forecast temperature");
                }
                mHigh = high;
                return this;
            }
            public Builder setLow(float low) {
                if (Float.isNaN(low)) {
                    throw new IllegalArgumentException("Invalid low forecast temperature");
                }
                mLow = low;
                return this;
            }

            public Builder setWeatherCondition(int code) {
                if (!isValidWeatherCode(code)) {
                    throw new IllegalArgumentException("Invalid weather condition code");
                }
                mConditionCode = code;
                return this;
            }

            public DayForecast build() {
                DayForecast forecast = new DayForecast();
                forecast.mLow = this.mLow;
                forecast.mHigh = this.mHigh;
                forecast.mConditionCode = this.mConditionCode;
                forecast.mKey = this.hashCode();
                return forecast;
            }
        }

        /**
         * @return forecasted low temperature
         */
        public float getLow() {
            return mLow;
        }

        /**
         * @return not what you think. Returns the forecasted high temperature
         */
        public float getHigh() {
            return mHigh;
        }

        /**
         * @return forecasted weather condition code. Implementation specific
         */
        public int getConditionCode() {
            return mConditionCode;
        }

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
            dest.writeFloat(mLow);
            dest.writeFloat(mHigh);
            dest.writeInt(mConditionCode);

            // Complete parcel info for the concierge
            parcelInfo.complete();
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
            // Read parcelable version via the Concierge
            ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
            int parcelableVersion = parcelInfo.getParcelVersion();

            if (parcelableVersion >= Build.CM_VERSION_CODES.ELDERBERRY) {
                mKey = parcel.readInt();
                mLow = parcel.readFloat();
                mHigh = parcel.readFloat();
                mConditionCode = parcel.readInt();
            }

            // Complete parcel info for the concierge
            parcelInfo.complete();
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("{Low temp: ").append(mLow)
                    .append(" High temp: ").append(mHigh)
                    .append(" Condition code: ").append(mConditionCode)
                    .append("}").toString();
        }

        @Override
        public int hashCode() {
            //The hashcode of this object was stored when it was built. This is an
            //immutable object but we need to preserve the hashcode since this object is parcelable
            //and it's reconstructed over IPC, and clients of this object might want to store it in
            //a collection that relies on this code to identify the object
            return mKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DayForecast) {
                DayForecast forecast = (DayForecast) obj;
                return (forecast.hashCode() == this.mKey);
            }
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
            .append("{CityId: ").append(mCityId)
            .append(" City Name: ").append(mCity)
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
        if (obj instanceof WeatherInfo) {
            WeatherInfo info = (WeatherInfo) obj;
            return (info.hashCode() == this.mKey);
        }
        return false;
    }
}