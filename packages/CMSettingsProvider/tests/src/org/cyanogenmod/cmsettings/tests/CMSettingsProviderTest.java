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
import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.TextUtils;
import cyanogenmod.providers.CMSettings;

import java.util.LinkedHashMap;
import java.util.Map;

 public class CMSettingsProviderTest extends AndroidTestCase {
     private static final String TAG = "CMSettingsProviderTest";

     private static final LinkedHashMap<String, String> sMap = new LinkedHashMap<String, String>();

     static {
         sMap.put("testKey1", "value1");
         sMap.put("testKey2", "value2");
         sMap.put("testKey3", "value3");
     }

     private static final String[] PROJECTIONS = new String[] { Settings.NameValueTable.NAME,
             Settings.NameValueTable.VALUE };

     private ContentResolver mContentResolver;
     private UserManager mUserManager;
     private UserInfo mGuest;

     @Override
     public void setUp() {
         mContentResolver = mContext.getContentResolver();
         mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
     }

     @Override
     public void tearDown() {
         if (mGuest != null) {
             mUserManager.removeUser(mGuest.id);
         }
     }

     @MediumTest
     public void testMigrateCMSettingsForOtherUser() {
         // Make sure there's an owner
         assertTrue(findUser(mUserManager, UserHandle.USER_OWNER));

         mGuest = mUserManager.createGuest(mContext, "GuestUser1");
         assertNotNull(mGuest);

         testMigrateSettingsForUser(mGuest.id);
     }

     private void testMigrateSettingsForUser(int userId) {
         // Setup values in Settings
         final String expectedPullDownValue = "testQuickPullDownValue";
         Settings.System.putStringForUser(mContentResolver, Settings.System.QS_QUICK_PULLDOWN,
                 expectedPullDownValue, userId);

         final int expectedKeyboardBrightness = 4;
         Settings.Secure.putIntForUser(mContentResolver, Settings.Secure.KEYBOARD_BRIGHTNESS,
                 expectedKeyboardBrightness, userId);

         Bundle arg = new Bundle();
         arg.putInt(CMSettings.CALL_METHOD_USER_KEY, userId);
         IContentProvider contentProvider = mContentResolver.acquireProvider(
                 CMSettings.AUTHORITY);

         try{
             // Trigger migrate settings for guest
             contentProvider.call(mContentResolver.getPackageName(),
                     CMSettings.CALL_METHOD_MIGRATE_SETTINGS_FOR_USER, null, arg);
         } catch (RemoteException ex) {
             fail("Failed to trigger settings migration due to RemoteException");
         }

         // Check values
         final String actualPullDownValue = CMSettings.System.getStringForUser(mContentResolver,
                 CMSettings.System.QS_QUICK_PULLDOWN, userId);
         assertEquals(expectedPullDownValue, actualPullDownValue);

         final int actualKeyboardBrightness = CMSettings.Secure.getIntForUser(mContentResolver,
                 CMSettings.Secure.KEYBOARD_BRIGHTNESS, -1, userId);
         assertEquals(expectedKeyboardBrightness, actualKeyboardBrightness);
     }

     private boolean findUser(UserManager userManager, int userHandle) {
         for (UserInfo user : userManager.getUsers()) {
             if (user.id == userHandle) {
                 return true;
             }
         }
         return false;
     }

     @MediumTest
     public void testBulkInsertSuccess() {
         ContentValues[] contentValues = new ContentValues[sMap.size()];
         String[] keyValues = new String[sMap.size()];
         int count = 0;
         for (Map.Entry<String, String> kVPair : sMap.entrySet()) {
             ContentValues contentValue = new ContentValues();

             final String key = kVPair.getKey();
             contentValue.put(Settings.NameValueTable.NAME, key);
             keyValues[count] = key;

             contentValue.put(Settings.NameValueTable.VALUE, kVPair.getValue());
             contentValues[count++] = contentValue;
         }

         testBulkInsertForUri(CMSettings.System.CONTENT_URI, contentValues, keyValues);
         testBulkInsertForUri(CMSettings.Secure.CONTENT_URI, contentValues, keyValues);
         testBulkInsertForUri(CMSettings.Global.CONTENT_URI, contentValues, keyValues);
     }

     private void testBulkInsertForUri(Uri uri, ContentValues[] contentValues, String[] keyValues) {
         int rowsInserted = mContentResolver.bulkInsert(uri, contentValues);
         assertEquals(sMap.size(), rowsInserted);

         final String placeholderSymbol = "?";
         String[] placeholders = new String[contentValues.length];
         for (int i = 0; i < placeholders.length; i++) {
             placeholders[i] = placeholderSymbol;
         }

         final String placeholdersString = TextUtils.join(",", placeholders);

         Cursor queryCursor = mContentResolver.query(uri, PROJECTIONS,
                 Settings.NameValueTable.NAME + " IN (" + placeholdersString + ")", keyValues,
                        null);
         assertEquals(contentValues.length, queryCursor.getCount());
         try {
             while (queryCursor.moveToNext()) {
                 assertEquals(PROJECTIONS.length, queryCursor.getColumnCount());

                 String actualKey = queryCursor.getString(0);
                 assertTrue(sMap.containsKey(actualKey));

                 assertEquals(sMap.get(actualKey), queryCursor.getString(1));
             }
         }
         finally {
             queryCursor.close();
         }

         // TODO: Find a better way to cleanup database/use ProviderTestCase2 without process crash
         for (String key : sMap.keySet()) {
             mContentResolver.delete(uri, Settings.NameValueTable.NAME + " = ?",
                     new String[]{ key });
         }
     }

     @MediumTest
     public void testInsertUpdateDeleteSuccess() {
         testInsertUpdateDeleteForUri(CMSettings.System.CONTENT_URI);
         testInsertUpdateDeleteForUri(CMSettings.Secure.CONTENT_URI);
         testInsertUpdateDeleteForUri(CMSettings.Global.CONTENT_URI);
     }

     private void testInsertUpdateDeleteForUri(Uri uri) {
         String key = "key";
         String value1 = "value1";
         String value2 = "value2";

         // test insert
         ContentValues contentValue = new ContentValues();
         contentValue.put(Settings.NameValueTable.NAME, key);
         contentValue.put(Settings.NameValueTable.VALUE, value1);

         Uri expectedUri = uri.withAppendedPath(uri, key);
         Uri returnUri = mContentResolver.insert(uri, contentValue);
         assertEquals(expectedUri, returnUri);
         
         Cursor queryCursor = null;
         try {
             // check insert
             queryCursor = mContentResolver.query(uri, PROJECTIONS, Settings.NameValueTable.NAME +
                     " = ?", new String[]{ key }, null);
             assertEquals(1, queryCursor.getCount());

             assertExpectedKeyValuePair(queryCursor, key, value1);

             // check insert with returned uri
             queryCursor = mContentResolver.query(returnUri, PROJECTIONS, null, null, null);
             assertEquals(1, queryCursor.getCount());

             assertExpectedKeyValuePair(queryCursor, key, value1);

             // test update
             contentValue.clear();
             contentValue.put(Settings.NameValueTable.VALUE, value2);

             int rowsAffected = mContentResolver.update(uri, contentValue,
                     Settings.NameValueTable.NAME + " = ?", new String[]{ key });
             assertEquals(1, rowsAffected);

             // check update
             queryCursor = mContentResolver.query(uri, PROJECTIONS, Settings.NameValueTable.NAME +
                     " = ?", new String[]{ key }, null);
             assertEquals(1, queryCursor.getCount());

             assertExpectedKeyValuePair(queryCursor, key, value2);

             // test delete
             rowsAffected = mContentResolver.delete(uri, Settings.NameValueTable.NAME + " = ?",
                     new String[]{ key });
             assertEquals(1, rowsAffected);

             // check delete
             queryCursor = mContentResolver.query(uri, PROJECTIONS, Settings.NameValueTable.NAME +
                     " = ?", new String[]{ key }, null);
             assertEquals(0, queryCursor.getCount());
         } finally {
             if (queryCursor != null) {
                 queryCursor.close();
             }
         }
     }

     private void assertExpectedKeyValuePair(Cursor cursor, String expectedKey,
            String expectedValue) {
         cursor.moveToNext();
         assertEquals(PROJECTIONS.length, cursor.getColumnCount());

         String actualKey = cursor.getString(0);
         assertEquals(expectedKey, actualKey);
         assertEquals(expectedValue, cursor.getString(1));
     }
 }
