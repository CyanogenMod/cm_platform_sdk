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

package cyanogenmod.weather.util;


/**
 * Helper class to perform operations and formatting of weather data
 */
public class WeatherUtils {

    public static float celsiusToFahrenheit(float celsius) {
        return ((celsius * (9f/5f)) + 32f);
    }

    public static float fahrenheitToCelsius(float fahrenheit) {
        return  ((fahrenheit - 32f) * (5f/9f));
    }
}
