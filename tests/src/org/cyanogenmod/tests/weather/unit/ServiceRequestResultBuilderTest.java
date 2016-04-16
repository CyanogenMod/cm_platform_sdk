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
import cyanogenmod.weather.WeatherLocation;
import cyanogenmod.weatherservice.ServiceRequestResult;

import java.util.ArrayList;
import java.util.List;

public class ServiceRequestResultBuilderTest extends AndroidTestCase {

    @SmallTest
    public void testUnravelFromParcelWithWeatherInfo() {
        final String cityName = "Cancun";
        final double temperature = 70;
        final int temperatureUnit = WeatherContract.WeatherColumns.TempUnit.CELSIUS;

        WeatherInfo info = new WeatherInfo.Builder(cityName, temperature, temperatureUnit)
                .build();

        ServiceRequestResult result = new ServiceRequestResult.Builder(info).build();

        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        ServiceRequestResult resultFromParcel
                = ServiceRequestResult.CREATOR.createFromParcel(parcel);

        assertEquals(result, resultFromParcel);
        assertEquals(result.getWeatherInfo(), resultFromParcel.getWeatherInfo());
    }

    @SmallTest
    public void testsUnravelFromParcelWithWeatherLocationList() {

        List<WeatherLocation> weatherLocationList = new ArrayList<>();
        weatherLocationList.add(new WeatherLocation.Builder("Cancun").build());
        ServiceRequestResult result = new ServiceRequestResult.Builder(weatherLocationList).build();

        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        ServiceRequestResult resultFromParcel
                = ServiceRequestResult.CREATOR.createFromParcel(parcel);

        assertEquals(result, resultFromParcel);
        assertEquals(result.getLocationLookupList(), resultFromParcel.getLocationLookupList());
    }

    @SmallTest
    public void testNullWeatherInfo() {
        try {
            WeatherInfo info = null;
            ServiceRequestResult result = new ServiceRequestResult.Builder(info).build();
            throw new AssertionError("ServiceRequestResult object was built with null WeatherInfo");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }

    @SmallTest
    public void testNullWeatherLocationList() {
        try {
            List<WeatherLocation> list = null;
            ServiceRequestResult result = new ServiceRequestResult.Builder(list).build();
            throw new AssertionError("ServiceRequestResult object was built with null "
                    + "WeatherLocation list");
        } catch (IllegalArgumentException e) {
            /* EXPECTED */
        }
    }
}
