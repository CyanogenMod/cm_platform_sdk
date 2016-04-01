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

/**
 * <p>
 * The DataUsageProvdier contract containing definitions for the supported URIs and columns
 * </p>
 */

public final class DataUsageContract {

    /** The authority for the DataUsage provider */
    public static final String DATAUSAGE_AUTHORITY = "org.cyanogenmod.providers.datausage";
    public static final String DATAUSAGE_TABLE = "datausage";

    /** The content URI for the top-level datausage authority */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + DATAUSAGE_AUTHORITY);

    /** The content URI for this table */
    public static final Uri CONTENT_URI =
            Uri.withAppendedPath(BASE_CONTENT_URI, DATAUSAGE_TABLE);

    /** Define database columns */
    /**
     * The unique ID for a row
     * <P>Type: INTEGER </P>
     */
    public static final String _ID             = "_id";

    /**
     * The UID of the application whose bandwidth is stored in this row
     * <P>Type: INTEGER </P>
     */
    public static final String UID             = "uid";

    /**
     * DataUsage Enable Configuration - statically set by the Settings App
     * 0 - no data usage warning are generated
     * 1 - data usage algorithm is evaluated and warnings are generated
     * <P>Type: INTEGER</P>
     */
    public static final String ENABLE          = "enable";

    /**
     * DataUsage Active State - dynamically computed by the DataUsage algorithm to
     * determine warning type to display to the user
     * 0 - first warning type is generated, once a warning generation is triggered
     * 1 - Nth warning type is generated, once a warning generation is triggered
     * <P>Type: INTEGER</P>
     */
    public static final String ACTIVE          = "active";

    /**
     * The Name of the Application that corresponds to the uid
     * <P>Type: TEXT</P>
     */
    public static final String LABEL           = "label";

    /**
     * Number of bytes consumed by the App so far. It is used to determine the number
     * of bytes consumed between samples
     * <P>Type: INTEGER (long) </P>
     */
    public static final String BYTES           = "bytes";

    /**
     * The slow bandwidth consumption average accumulated over 'SLOW' number of samples
     * <P>Type: INTEGER (long)</P>
     */
    public static final String SLOW_AVG        = "slow_avg";

    /**
     * Number of slow samples accumulated so far, once the number of samples reaches a
     * MAX number of samples, the 'slow_samples' pegs at MAX and new samples
     * are factored into 'slow_avg' by "taking out" one sample.
     * slow_samples < MAX: slow_avg = (slow_avg * slow_samples + new_sample)/(slow_samples+1)
     * slow_samples == MAX: slow_avg = (slow_avg * (MAX-1) + new_sample)/MAX
     * <P>Type: Integer (long></P>
     */
    public static final String SLOW_SAMPLES    = "slow_samples";

    /**
     * The fast bandwidth consumption average accumulated over 'fast' number of samples
     * <P>Type: INTEGER (long)</P>
     */
    public static final String FAST_AVG        = "fast_avg";

    /**
     * Number of fast samples accumulated so far, analogous algorithm to 'slow_samples'
     * <P>Type: INTEGER (long)</P>
     */
    public static final String FAST_SAMPLES    = "fast_samples";

    /**
     * Extra information used debugging purposes - collects up to 1000 samples so that
     * algorithm can be manually verified
     * <P>Type: TEXT</P>
     */
    public static final String EXTRA           = "extra";

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
            ENABLE,
            ACTIVE,
            LABEL,
            BYTES,
            SLOW_AVG,
            SLOW_SAMPLES,
            FAST_AVG,
            FAST_SAMPLES,
            EXTRA
    };

    /**
     * Column index for each field in the database row
     */
    public static final int COLUMN_OF_ID           = 0;
    public static final int COLUMN_OF_UID          = 1;
    public static final int COLUMN_OF_ENABLE       = 2;
    public static final int COLUMN_OF_ACTIVE       = 3;
    public static final int COLUMN_OF_LABEL        = 4;
    public static final int COLUMN_OF_BYTES        = 5;
    public static final int COLUMN_OF_SLOW_AVG     = 6;
    public static final int COLUMN_OF_SLOW_SAMPLES = 7;
    public static final int COLUMN_OF_FAST_AVG     = 8;
    public static final int COLUMN_OF_FAST_SAMPLES = 9;
    public static final int COLUMN_OF_EXTRA        = 10;


}
