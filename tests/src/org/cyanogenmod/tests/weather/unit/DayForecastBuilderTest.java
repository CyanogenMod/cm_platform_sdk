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
import cyanogenmod.weather.WeatherInfo.DayForecast;

public class DayForecastBuilderTest extends AndroidTestCase {

    private static final int mConditionCode = WeatherContract.WeatherColumns.WeatherCode.HURRICANE;
    private static final double mLow = 35;
    private static final double mHigh = 58;

    @SmallTest
    public void testUnravelFromParcel() {
        DayForecast forecast = new DayForecast.Builder(mConditionCode).setHigh(mHigh).setLow(mLow)
                .build();

        Parcel parcel = Parcel.obtain();
        forecast.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        DayForecast forecastFromParcel = DayForecast.CREATOR.createFromParcel(parcel);

        assertEquals(forecast, forecastFromParcel);
        assertEquals(forecast.getConditionCode(), forecastFromParcel.getConditionCode());
        assertEquals(forecast.getHigh(), forecastFromParcel.getHigh());
        assertEquals(forecast.getLow(), forecastFromParcel.getLow());
    }

    @SmallTest
    public void testUnravelFromParcelWithDefaultValues() {
        //Condition code is required
        DayForecast forecast = new DayForecast.Builder(mConditionCode).build();

        Parcel parcel = Parcel.obtain();
        forecast.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        DayForecast forecastFromParcel = DayForecast.CREATOR.createFromParcel(parcel);

        assertEquals(forecast, forecastFromParcel);
        assertEquals(forecast.getConditionCode(), forecastFromParcel.getConditionCode());
        assertEquals(forecast.getHigh(), forecastFromParcel.getHigh());
        assertEquals(forecast.getLow(), forecastFromParcel.getLow());
    }

    @SmallTest
    public void testDayForecastBuilder() {
        DayForecast forecast = new DayForecast.Builder(mConditionCode).setHigh(mHigh).setLow(mLow)
                .build();

        assertEquals(forecast.getConditionCode(), mConditionCode);
        assertEquals(forecast.getHigh(), mHigh);
        assertEquals(forecast.getLow(), mLow);
    }

    @SmallTest
    public void testInvalidWeatherConditionCode() {
        try {
            DayForecast forecast = new DayForecast.Builder(Integer.MIN_VALUE).build();
            throw new AssertionError("DayForecast object was built with invalid condition code!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidHighTemperature() {
        try {
            DayForecast forecast = new DayForecast.Builder(mConditionCode).setHigh(Double.NaN)
                    .setLow(mLow).build();
            throw new AssertionError("DayForecast object was built with invalid high temperature!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testInvalidLowTemperature() {
        try {
            DayForecast forecast = new DayForecast.Builder(mConditionCode).setHigh(mHigh)
                    .setLow(Double.NaN).build();
            throw new AssertionError("DayForecast object was built with invalid low temperature!");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }
}
