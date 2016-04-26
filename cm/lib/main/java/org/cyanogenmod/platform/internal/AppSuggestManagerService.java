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

package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.suggest.ApplicationSuggestion;
import cyanogenmod.app.suggest.IAppSuggestManager;

import java.util.ArrayList;
import java.util.List;

/** @hide */
public class AppSuggestManagerService extends CMSystemService {
    private static final String TAG = "AppSgstMgrService";
    public static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String NAME = "appsuggest";

    public static final String ACTION = "org.cyanogenmod.app.suggest";

    private AppSuggestProviderInterface mImpl;
    private Context mContext;

    private final IBinder mService = new IAppSuggestManager.Stub() {
        public boolean handles(Intent intent) {
            if (mImpl == null) return false;

            return mImpl.handles(intent);
        }

        public List<ApplicationSuggestion> getSuggestions(Intent intent) {
            if (mImpl == null) return new ArrayList<>(0);

            return mImpl.getSuggestions(intent);
        }
    };

    public AppSuggestManagerService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.APP_SUGGEST;
    }

    @Override
    public void onStart() {
        mImpl = AppSuggestProviderProxy.createAndBind(mContext, TAG, ACTION,
                R.bool.config_enableAppSuggestOverlay,
                R.string.config_appSuggestProviderPackageName,
                R.array.config_appSuggestProviderPackageNames);
        if (mImpl == null) {
            Slog.e(TAG, "no app suggest provider found");
        } else {
            Slog.i(TAG, "Bound to to suggest provider");
        }
        publishBinderService(CMContextConstants.CM_APP_SUGGEST_SERVICE, mService);
    }
}
