/**
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

package org.cyanogenmod.tests.weather.unit;

import android.os.Parcel;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.WeatherInfo;

public class WeatherInfoBuilderTest extends AndroidTestCase {

    private static final String mCityName = "Cancun";
    private static final double mTemperature = 70;
    private static final int mTemperatureUnit = WeatherContract.WeatherColumns.TempUnit.CELSIUS;
    private static final double mHumidity = 45;
    private static final double mWindSpeed = 15;
    private static final double mWindDirection = 150;
    private static final int mWindSpeedUnit = WeatherContract.WeatherColumns.WindSpeedUnit.KPH;
    private static final long mTimestamp = System.currentTimeMillis();
    private static final double mTodaysHigh = 80;
    private static final double mTodaysLow = 65;
    private static final int mWeatherConditionCode
            = WeatherContract.WeatherColumns.WeatherCode.SUNNY;

    @SmallTest
    public void testUnravelFromParcelWithDefaultValues() {
        //City name, temp and unit are required
        WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature, mTemperatureUnit)
                .build();
        Parcel parcel = Parcel.obtain();
        info.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        WeatherInfo weatherInfoFromParcel = WeatherInfo.CREATOR.createFromParcel(parcel);
        assertNotNull(weatherInfoFromParcel);

        assertEquals(info, weatherInfoFromParcel);
        //City name
        assertEquals(info.getCity(), weatherInfoFromParcel.getCity());
        //Forecast list
        assertEquals(info.getForecasts(), weatherInfoFromParcel.getForecasts());
        //Humidity
        assertEquals(info.getHumidity(), weatherInfoFromParcel.getHumidity());
        //Temp
        assertEquals(info.getConditionCode(), weatherInfoFromParcel.getConditionCode());
        assertEquals(info.getTemperature(), weatherInfoFromParcel.getTemperature());
        assertEquals(info.getTemperatureUnit(), weatherInfoFromParcel.getTemperatureUnit());
        //Timestamp
        assertEquals(info.getTimestamp(), weatherInfoFromParcel.getTimestamp());
        //Today's low/high
        assertEquals(info.getTodaysHigh(), weatherInfoFromParcel.getTodaysHigh());
        assertEquals(info.getTodaysLow(), weatherInfoFromParcel.getTodaysLow());
        //Wind
        assertEquals(info.getWindDirection(), weatherInfoFromParcel.getWindDirection());
        assertEquals(info.getWindSpeed(), weatherInfoFromParcel.getWindDirection());
        assertEquals(info.getWindSpeedUnit(), weatherInfoFromParcel.getWindSpeedUnit());
        //Verify default values
        assertEquals(info.getTodaysHigh(), Double.NaN);
    }

    @SmallTest
    public void testWeatherInfoBuilder() {
        WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature, mTemperatureUnit)
                .setHumidity(mHumidity)
                .setWind(mWindSpeed, mWindDirection, mWindSpeedUnit)
                .setTimestamp(mTimestamp)
                .setTodaysHigh(mTodaysHigh)
                .setTodaysLow(mTodaysLow)
                .setWeatherCondition(mWeatherConditionCode).build();

        assertEquals(info.getCity(), mCityName);
        assertEquals(info.getTemperature(), mTemperature);
        assertEquals(info.getTemperatureUnit(), mTemperatureUnit);
        assertEquals(info.getHumidity(), mHumidity);
        assertEquals(info.getWindSpeed(), mWindSpeed);
        assertEquals(info.getWindDirection(), mWindDirection);
        assertEquals(info.getWindSpeedUnit(), mWindSpeedUnit);
        assertEquals(info.getTimestamp(), mTimestamp);
        assertEquals(info.getTodaysHigh(), mTodaysHigh);
        assertEquals(info.getTodaysLow(), mTodaysLow);
        assertEquals(info.getConditionCode(), mWeatherConditionCode);
    }

    @SmallTest
    public void testUnravelFromParcel() {
        WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature, mTemperatureUnit)
                .setHumidity(mHumidity)
                .setWind(mWindSpeed, mWindDirection, mWindSpeedUnit)
                .setTimestamp(mTimestamp)
                .setTodaysHigh(mTodaysHigh)
                .setTodaysLow(mTodaysLow)
                .setWeatherCondition(mWeatherConditionCode).build();

        Parcel parcel = Parcel.obtain();
        info.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        WeatherInfo infoFromParcel = WeatherInfo.CREATOR.createFromParcel(parcel);
        assertEquals(info.getCity(), infoFromParcel.getCity());
        assertEquals(info.getTemperature(), infoFromParcel.getTemperature());
        assertEquals(info.getTemperatureUnit(), infoFromParcel.getTemperatureUnit());
        assertEquals(info.getHumidity(), infoFromParcel.getHumidity());
        assertEquals(info.getWindSpeed(), infoFromParcel.getWindSpeed());
        assertEquals(info.getWindDirection(), infoFromParcel.getWindDirection());
        assertEquals(info.getWindSpeedUnit(), infoFromParcel.getWindSpeedUnit());
        assertEquals(info.getTimestamp(), infoFromParcel.getTimestamp());
        assertEquals(info.getTodaysHigh(), infoFromParcel.getTodaysHigh());
        assertEquals(info.getTodaysLow(), infoFromParcel.getTodaysLow());
        assertEquals(info.getConditionCode(), infoFromParcel.getConditionCode());
    }

    @SmallTest
    public void testNullCityName() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(null, mTemperature, mTemperatureUnit)
                    .build();
            throw new AssertionError("WeatherInfo object built with null city name!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidTemperature() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, Double.NaN, mTemperatureUnit)
                    .build();
            throw new AssertionError("WeatherInfo object built with invalid temperature value!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidTemperatureUnit() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,Integer.MIN_VALUE)
                    .build();
            throw new AssertionError("WeatherInfo object built with invalid temperature unit!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testNullForecastList() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setForecast(null).build();
            throw new AssertionError("WeatherInfo object built with null forecast list!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidWeatherConditionCode() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setWeatherCondition(Integer.MIN_VALUE).build();
            throw new AssertionError("WeatherInfo object built with invalid weather "
                    + "condition code!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidHumidity() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setHumidity(Double.NaN).build();
            throw new AssertionError("WeatherInfo object built with invalid humidity value!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidWindSpeed() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setWind(Double.NaN, mWindDirection, mWindSpeedUnit)
                    .build();
            throw new AssertionError("WeatherInfo object built with invalid wind speed!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidWindDirection() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setWind(mWindSpeed, Double.NaN, mWindSpeedUnit)
                    .build();
            throw new AssertionError("WeatherInfo object built with invalid wind direction!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidWindSpeedUnit(){
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setWind(mWindSpeed, mWindDirection, Integer.MIN_VALUE).build();
            throw new AssertionError("WeatherInfo object built with invalid wind speed unit!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidTodaysLow() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setTodaysLow(Double.NaN).build();
            throw new AssertionError("WeatherInfo object built with invalid low temp!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidTodaysHigh() {
        try {
            WeatherInfo info = new WeatherInfo.Builder(mCityName, mTemperature,mTemperatureUnit)
                    .setTodaysHigh(Double.NaN).build();
            throw new AssertionError("WeatherInfo object built with invalid high temp!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }
}
