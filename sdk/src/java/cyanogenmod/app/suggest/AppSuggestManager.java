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

package cyanogenmod.app.suggest;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.suggest.ApplicationSuggestion;

/**
 * Provides an interface to get information about suggested apps for an intent which may include
 * applications not installed on the device. This is used by the CMResolver in order to provide
 * suggestions when an intent is fired but no application exists for the given intent.
 *
 * @hide
 */
public class AppSuggestManager {
    private static final String TAG = AppSuggestManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static IAppSuggestManager sImpl;

    private static AppSuggestManager sInstance;

    private Context mContext;

    /**
     * Gets an instance of the AppSuggestManager.
     *
     * @param context
     *
     * @return An instance of the AppSuggestManager
     */
    public static synchronized AppSuggestManager getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        context = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        sInstance = new AppSuggestManager(context);

        return sInstance;
    }

    private AppSuggestManager(Context context) {
        mContext = context.getApplicationContext();
    }

    private static synchronized IAppSuggestManager getService() {
        if (sImpl == null) {
            IBinder b = ServiceManager.getService(CMContextConstants.CM_APP_SUGGEST_SERVICE);
            if (b != null) {
                sImpl = IAppSuggestManager.Stub.asInterface(b);
            } else {
                Log.e(TAG, "Unable to find implementation for app suggest service");
            }
        }

        return sImpl;
    }

    /**
     * Checks to see if an intent is handled by the App Suggestions Service. This should be
     * implemented in such a way that it is safe to call inline on the UI Thread.
     *
     * @param intent The intent
     * @return true if the App Suggestions Service has suggestions for this intent, false otherwise
     */
    public boolean handles(Intent intent) {
        IAppSuggestManager mgr = getService();
        if (mgr == null) return false;
        try {
            return mgr.handles(intent);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     *
     * Gets a list of the suggestions for the given intent.
     *
     * @param intent The intent
     * @return A list of application suggestions or an empty list if none.
     */
    public List<ApplicationSuggestion> getSuggestions(Intent intent) {
        IAppSuggestManager mgr = getService();
        if (mgr == null) return new ArrayList<>(0);
        try {
            return mgr.getSuggestions(intent);
        } catch (RemoteException e) {
            return new ArrayList<>(0);
        }
    }

    /**
     * Loads the icon for the given suggestion.
     *
     * @param suggestion The suggestion to load the icon for
     *
     * @return A {@link Drawable} or null if one cannot be found
     */
    public Drawable loadIcon(ApplicationSuggestion suggestion) {
        try {
            InputStream is = mContext.getContentResolver()
                    .openInputStream(suggestion.getThumbailUri());
            return Drawable.createFromStream(is, null);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
