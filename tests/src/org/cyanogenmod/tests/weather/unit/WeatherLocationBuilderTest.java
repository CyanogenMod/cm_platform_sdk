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
import cyanogenmod.weather.WeatherLocation;

public class WeatherLocationBuilderTest extends AndroidTestCase {

    private static final String mCityId = "can1";
    private static final String mCityName = "Cancun";
    private static final String mState = "Quintana Roo";
    private static final String mCountryId = "MX";
    private static final String mCountry = "Mexico";
    private static final String mZipCode = "77510";

    @SmallTest
    public void testUnravelFromParcelTwoArgsConstructor() {
        WeatherLocation location = new WeatherLocation.Builder(mCityId, mCityName)
                .setState(mState).setPostalCode(mZipCode).setCountryId(mCountryId)
                .setCountry(mCountry).build();

        Parcel parcel = Parcel.obtain();
        location.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        WeatherLocation locationFromParcel = WeatherLocation.CREATOR.createFromParcel(parcel);

        assertEquals(location, locationFromParcel);
        assertEquals(location.getCityId(), locationFromParcel.getCityId());
        assertEquals(location.getCity(), locationFromParcel.getCity());
        assertEquals(location.getState(), locationFromParcel.getState());
        assertEquals(location.getPostalCode(), locationFromParcel.getPostalCode());
        assertEquals(location.getCountry(), locationFromParcel.getCountry());
        assertEquals(location.getCountryId(), locationFromParcel.getCountryId());
    }

    @SmallTest
    public void testUnravelFromParcelOneArgConstructor() {
        WeatherLocation location = new WeatherLocation.Builder(mCityName)
                .setState(mState).setPostalCode(mZipCode).setCountryId(mCountryId)
                .setCountry(mCountry).build();

        Parcel parcel = Parcel.obtain();
        location.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        WeatherLocation locationFromParcel = WeatherLocation.CREATOR.createFromParcel(parcel);

        assertEquals(location, locationFromParcel);
        assertEquals(location.getCityId(), locationFromParcel.getCityId());
        assertEquals(location.getCity(), locationFromParcel.getCity());
        assertEquals(location.getState(), locationFromParcel.getState());
        assertEquals(location.getPostalCode(), locationFromParcel.getPostalCode());
        assertEquals(location.getCountry(), locationFromParcel.getCountry());
        assertEquals(location.getCountryId(), locationFromParcel.getCountryId());
    }

    @SmallTest
    public void testUnravelFromParcelWithDefaultsOneArgConstructor() {
        WeatherLocation location = new WeatherLocation.Builder(mCityName).build();
        Parcel parcel = Parcel.obtain();
        location.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        WeatherLocation locationFromParcel = WeatherLocation.CREATOR.createFromParcel(parcel);

        assertEquals(location, locationFromParcel);
        assertEquals(location.getCityId(), locationFromParcel.getCityId());
        assertEquals(location.getCity(), locationFromParcel.getCity());
        assertEquals(location.getState(), locationFromParcel.getState());
        assertEquals(location.getPostalCode(), locationFromParcel.getPostalCode());
        assertEquals(location.getCountry(), locationFromParcel.getCountry());
        assertEquals(location.getCountryId(), locationFromParcel.getCountryId());
    }

    @SmallTest
    public void testUnravelFromParcelWithDefaultsTwoArgsConstructor() {
        WeatherLocation location = new WeatherLocation.Builder(mCityId, mCityName).build();
        Parcel parcel = Parcel.obtain();
        location.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        WeatherLocation locationFromParcel = WeatherLocation.CREATOR.createFromParcel(parcel);

        assertEquals(location, locationFromParcel);
        assertEquals(location.getCityId(), locationFromParcel.getCityId());
        assertEquals(location.getCity(), locationFromParcel.getCity());
        assertEquals(location.getState(), locationFromParcel.getState());
        assertEquals(location.getPostalCode(), locationFromParcel.getPostalCode());
        assertEquals(location.getCountry(), locationFromParcel.getCountry());
        assertEquals(location.getCountryId(), locationFromParcel.getCountryId());
    }


    @SmallTest
    public void testWeatherLocationBuilder() {
        WeatherLocation location = new WeatherLocation.Builder(mCityId, mCityName)
                .setState(mState).setPostalCode(mZipCode).setCountryId(mCountryId)
                .setCountry(mCountry).build();

        assertEquals(location.getCityId(), mCityId);
        assertEquals(location.getCity(), mCityName);
        assertEquals(location.getState(), mState);
        assertEquals(location.getPostalCode(), mZipCode);
        assertEquals(location.getCountryId(), mCountryId);
        assertEquals(location.getCountry(), mCountry);
    }
}
