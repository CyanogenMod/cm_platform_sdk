package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.ServiceWatcher;

import cyanogenmod.app.suggest.ApplicationSuggestion;
import cyanogenmod.app.suggest.IAppSuggestProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
public class AppSuggestProviderProxy implements AppSuggestProviderInterface {
    private static final String TAG = AppSuggestProviderProxy.class.getSimpleName();
    private static final boolean DEBUG = AppSuggestManagerService.DEBUG;

    public static AppSuggestProviderProxy createAndBind(
            Context context, String name, String action,
            int overlaySwitchResId, int defaultServicePackageNameResId,
            int initialPackageNamesResId) {
        AppSuggestProviderProxy proxy = new AppSuggestProviderProxy(context, name, action,
                overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId);
        if (proxy.bind()) {
            return proxy;
        } else {
            return null;
        }
    }

    private Context mContext;
    private ServiceWatcher mServiceWatcher;

    public AppSuggestProviderProxy(Context context, String name, String action,
                                   int overlaySwitchResId, int defaultServicePackageNameResId,
                                   int initialPackageNamesResId) {
        mContext = context;
        mServiceWatcher = new ServiceWatcher(mContext, TAG + "-" + name, action, overlaySwitchResId,
                defaultServicePackageNameResId, initialPackageNamesResId, null, null);
    }

    public boolean bind() {
        return mServiceWatcher.start();
    }

    private IAppSuggestProvider getService() {
        return IAppSuggestProvider.Stub.asInterface(mServiceWatcher.getBinder());
    }

    @Override
    public boolean handles(Intent intent) {
        IAppSuggestProvider service = getService();
        if (service == null) return false;

        try {
            return service.handles(intent);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        } catch (Exception e) {
            // never let remote service crash system server
            Log.e(TAG, "Exception from " + mServiceWatcher.getBestPackageName(), e);
        }
        return false;
    }

    @Override
    public List<ApplicationSuggestion> getSuggestions(Intent intent) {
        IAppSuggestProvider service = getService();
        if (service == null) return new ArrayList<>(0);

        try {
            return service.getSuggestions(intent);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        } catch (Exception e) {
            // never let remote service crash system server
            Log.e(TAG, "Exception from " + mServiceWatcher.getBestPackageName(), e);
        }
        return new ArrayList<>(0);
    }
}
