 /**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.cmsettings.tests;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

import java.util.LinkedHashMap;
import java.util.Map;

 public class CMSettingsProviderTest extends AndroidTestCase {
     private static final String TAG = "CMSettingsProviderTest";

     private static final LinkedHashMap<String, String> mMap = new LinkedHashMap<String, String>();

     static {
         mMap.put("testKey1", "value1");
         mMap.put("testKey2", "value2");
         mMap.put("testKey3", "value3");
     }

     private static final String[] PROJECTIONS = new String[] { "name", "value" };

     private ContentResolver mContentResolver;

     @Override
     public void setUp() {
         mContentResolver = mContext.getContentResolver();
     }

     @MediumTest
     public void testBulkInsertSuccess() {
         Log.d(TAG, "Starting bulk insert test");

         ContentValues[] contentValues = new ContentValues[mMap.size()];
         int count = 0;
         for (Map.Entry<String, String> kVPair : mMap.entrySet()) {
             ContentValues contentValue = new ContentValues();
             contentValue.put(PROJECTIONS[0], kVPair.getKey());
             contentValue.put(PROJECTIONS[1], kVPair.getValue());
             contentValues[count++] = contentValue;
         }

         testBulkInsertForUri(CMSettings.System.CONTENT_URI, contentValues);
         testBulkInsertForUri(CMSettings.Secure.CONTENT_URI, contentValues);
         testBulkInsertForUri(CMSettings.Global.CONTENT_URI, contentValues);

         Log.d(TAG, "Finished bulk insert test");
     }

     private void testBulkInsertForUri(Uri uri, ContentValues[] contentValues) {
         int rowsInserted = mContentResolver.bulkInsert(uri, contentValues);
         assertEquals(mMap.size(), rowsInserted);

         Cursor queryCursor = mContentResolver.query(uri, PROJECTIONS, null, null, null);
         try {
             while (queryCursor.moveToNext()) {
                 assertEquals(PROJECTIONS.length, queryCursor.getColumnCount());

                 String actualKey = queryCursor.getString(0);
                 assertTrue(mMap.containsKey(actualKey));

                 assertEquals(mMap.get(actualKey), queryCursor.getString(1));
             }

             Log.d(TAG, "Test successful");
         }
         finally {
             queryCursor.close();
         }

         // TODO: Find a better way to cleanup database/use ProviderTestCase2 without process crash
         for (String key : mMap.keySet()) {
             mContentResolver.delete(uri, PROJECTIONS[0] + " = ?", new String[]{ key });
         }
     }

     @MediumTest
     public void testInsertUpdateDeleteSuccess() {
         Log.d(TAG, "Starting insert/update/delete test");

         testInsertUpdateDeleteForUri(CMSettings.System.CONTENT_URI);
         testInsertUpdateDeleteForUri(CMSettings.Secure.CONTENT_URI);
         testInsertUpdateDeleteForUri(CMSettings.Global.CONTENT_URI);

         Log.d(TAG, "Finished insert/update/delete test");
     }

     private void testInsertUpdateDeleteForUri(Uri uri) {
         String key1 = "testKey1";
         String value1 = "value1";
         String value2 = "value2";

         // test insert
         ContentValues contentValue = new ContentValues();
         contentValue.put(PROJECTIONS[0], key1);
         contentValue.put(PROJECTIONS[1], value1);

         mContentResolver.insert(uri, contentValue);

         // check insert
         Cursor queryCursor = mContentResolver.query(uri, PROJECTIONS, null, null, null);
         assertEquals(1, queryCursor.getCount());

         queryCursor.moveToNext();
         assertEquals(PROJECTIONS.length, queryCursor.getColumnCount());

         String actualKey = queryCursor.getString(0);
         assertEquals(key1, actualKey);
         assertEquals(value1, queryCursor.getString(1));

         // test update
         contentValue.clear();
         contentValue.put(PROJECTIONS[1], value2);

         int rowsAffected = mContentResolver.update(uri, contentValue, PROJECTIONS[0] + " = ?",
                 new String[]{key1});
         assertEquals(1, rowsAffected);

         // check update
         queryCursor = mContentResolver.query(uri, PROJECTIONS, null, null, null);
         assertEquals(1, queryCursor.getCount());

         queryCursor.moveToNext();
         assertEquals(PROJECTIONS.length, queryCursor.getColumnCount());

         actualKey = queryCursor.getString(0);
         assertEquals(key1, actualKey);
         assertEquals(value2, queryCursor.getString(1));

         // test delete
         rowsAffected = mContentResolver.delete(uri, PROJECTIONS[0] + " = ?", new String[]{key1});
         assertEquals(1, rowsAffected);

         // check delete
         queryCursor = mContentResolver.query(uri, PROJECTIONS, null, null, null);
         assertEquals(0, queryCursor.getCount());
     }
 }
