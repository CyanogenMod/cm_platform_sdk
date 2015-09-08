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
 * @hide
 */
public class AppSuggestManager {
    private static final String TAG = AppSuggestManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static IAppSuggestManager sImpl;

    private static AppSuggestManager sInstance;

    private Context mContext;

    public static synchronized AppSuggestManager getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        context = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        sInstance = new AppSuggestManager(context);

        return sInstance;
    }

    public AppSuggestManager(Context context) {
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

    public boolean handles(Intent intent) {
        IAppSuggestManager mgr = getService();
        if (mgr == null) return false;
        try {
            return mgr.handles(intent);
        } catch (RemoteException e) {
            return false;
        }
    }

    public List<ApplicationSuggestion> getSuggestions(Intent intent) {
        IAppSuggestManager mgr = getService();
        if (mgr == null) return new ArrayList<>(0);
        try {
            return mgr.getSuggestions(intent);
        } catch (RemoteException e) {
            return new ArrayList<>(0);
        }
    }

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
