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

package cyanogenmod.providers;

import android.net.Uri;

/**
 * The contract between the weather provider and applications.
 */
public class WeatherContract {

    /**
     * The authority of the weather content provider
     */
    public static final String AUTHORITY = "com.cyanogenmod.weather";

    /**
     * A content:// style uri to the authority for the weather provider
     */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static class WeatherColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "weather");

        public static final Uri CURRENT_AND_FORECAST_WEATHER_URI
                = Uri.withAppendedPath(CONTENT_URI, "current_and_forecast");
        public static final Uri CURRENT_WEATHER_URI
                = Uri.withAppendedPath(CONTENT_URI, "current");
        public static final Uri FORECAST_WEATHER_URI
                = Uri.withAppendedPath(CONTENT_URI, "forecast");

        /**
         * A unique ID for the city. NOTE: this value fully depends on the implementation of the
         * weather provider service and can potentially change when you switch providers.
         * <P>Type: TEXT</P>
         */
        public static final String CURRENT_CITY_ID = "city_id";

        /**
         * The city name
         * <P>Type: TEXT</P>
         */
        public static final String CURRENT_CITY = "city";

        /**
         * A code identifying the current weather condition. NOTE: this value is implementation
         * dependent and can potentially change when you switch service providers
         * <P>Type: INTEGER</P>
         */
        public static final String CURRENT_CONDITION_CODE = "condition_code";

        /**
         * The current weather condition
         * <P>Type: TEXT</P>
         */
        public static final String CURRENT_CONDITION = "condition";

        /**
         * The current weather temperature
         * <P>Type: FLOAT</P>
         */
        public static final String CURRENT_TEMPERATURE = "temperature";

        /**
         * The unit in which current temperature is reported
         * <P>Type: TEXT</P>
         */
        public static final String CURRENT_TEMPERATURE_UNIT = "temperature_unit";

        /**
         * The current weather humidity
         * <P>Type: FLOAT</P>
         */
        public static final String CURRENT_HUMIDITY = "humidity";

        /**
         * The current wind direction (in degrees)
         * <P>Type: FLOAT</P>
         */
        public static final String CURRENT_WIND_DIRECTION = "wind_direction";

        /**
         * The current wind speed
         * <P>Type: FLOAT</P>
         */
        public static final String CURRENT_WIND_SPEED = "wind_speed";

        /**
         * The unit in which the wind speed is reported
         * <P>Type: TEXT</P>
         */
        public static final String CURRENT_WIND_SPEED_UNIT = "wind_speed_unit";

        /**
         * The timestamp when this weather was reported
         * <P>Type: LONG</P>
         */
        public static final String CURRENT_TIMESTAMP = "timestamp";

        /**
         * The forecasted low temperature
         * <P>Type: FLOAT</P>
         */
        public static final String FORECAST_LOW = "forecast_low";

        /**
         * The forecasted high temperature
         * <P>Type: FLOAT</P>
         */
        public static final String FORECAST_HIGH = "forecast_high";

        /**
         * The forecasted weather condition
         * <P>Type: TEXT</P>
         */
        public static final String FORECAST_CONDITION = "forecast_condition";

        /**
         * The code identifying the forecasted weather condition.
         * @see #CURRENT_CONDITION_CODE
         */
        public static final String FORECAST_CONDITION_CODE = "forecast_condition_code";
    }
}