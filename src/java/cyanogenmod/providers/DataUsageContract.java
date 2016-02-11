/**
 * Copyright (c) 2016, The CyanogenMod Project
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cyanogenmod.providers;

import android.content.ContentResolver;
import android.net.Uri;


public final class DataUsageContract {

    /**
     * The authority for the DataUsage provider.
     */
    public static final String DATAUSAGE_AUTHORITY = "org.cyanogenmod.providers.datausage";
    public static final String DATAUSAGE_TABLE = "datausage";

    /**
     * The content URI for the top-level datausage authority
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + DATAUSAGE_AUTHORITY);

    /**
     * The content URI for this table
     */
    public static final Uri CONTENT_URI =
            Uri.withAppendedPath(BASE_CONTENT_URI, DATAUSAGE_TABLE);

    /**
     * Define database columns
     */
    public static final String _ID             = "_id";
    public static final String UID             = "uid";
    public static final String ENB             = "enb";     // warning gen enabled
    public static final String ACTIVE          = "active";  // warning active
    public static final String LABEL           = "label";   // app label for debug
    public static final String BYTES           = "bytes";   // prev sample bytes
    // consumed bw avg over samples - slow moving
    public static final String SLOW_AVG        = "slow_avg";
    // accumulated samples - slow moving average
    public static final String SLOW_SAMPLES    = "slow_samples";
    // consumed bw avg over samples - fast moving
    public static final String FAST_AVG        = "fast_avg";
    // accumulated samples - fast moving average
    public static final String FAST_SAMPLES    = "fast_samples";
    public static final String EXTRA           = "extra";   // extra samples for debug

    /**
     * The mime type of a directory of items
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "datausage_item";

    /**
     * The mime type of a single item
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "datausage_item";

    /**
     * A projection of all columns in the datausage table.
     */
    public static final String [] PROJECTION_ALL = {
            _ID,
            UID,
            ENB,
            ACTIVE,
            LABEL,
            BYTES,
            SLOW_AVG,
            SLOW_SAMPLES,
            FAST_AVG,
            FAST_SAMPLES,
            EXTRA
    };

    public static final int COLUMN_OF_ID           = 0;
    public static final int COLUMN_OF_UID          = 1;
    public static final int COLUMN_OF_ENB          = 2;
    public static final int COLUMN_OF_ACTIVE       = 3;
    public static final int COLUMN_OF_LABEL        = 4;
    public static final int COLUMN_OF_BYTES        = 5;
    public static final int COLUMN_OF_SLOW_AVG     = 6;
    public static final int COLUMN_OF_SLOW_SAMPLES = 7;
    public static final int COLUMN_OF_FAST_AVG     = 8;
    public static final int COLUMN_OF_FAST_SAMPLES = 9;
    public static final int COLUMN_OF_EXTRA        = 10;


}
