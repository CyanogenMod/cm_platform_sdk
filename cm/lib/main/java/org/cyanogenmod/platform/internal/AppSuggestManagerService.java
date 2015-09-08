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

public class AppSuggestManagerService extends SystemService {
    private static final String TAG = "AppSgstMgrService";
    public static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String NAME = "appsuggest";

    public static final String ACTION = "org.cyanogenmod.app.suggest";

    private AppSuggestProviderInterface mImpl;

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
