/*
 * Copyright (c) 2016 CyanogenMod Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.IPartnerInterface;
import cyanogenmod.app.PartnerInterface;
import cyanogenmod.app.suggest.ApplicationSuggestion;
import cyanogenmod.app.suggest.IAppSuggestManager;
import cyanogenmod.media.MediaRecorder;

import java.util.List;

public class AppSuggestServiceBroker extends BrokerableCMSystemService<IAppSuggestManager> {

    private static final boolean DEBUG = true;
    private static final String TAG = "AppSuggestServiceBroker";

    private final Context mContext;

    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.appsuggestservice",
                    "org.cyanogenmod.appsuggestservice.AppSuggestService");

    public AppSuggestServiceBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public IAppSuggestManager getIBinderAsIInterface(IBinder service) {
        return IAppSuggestManager.Stub.asInterface(service);
    }

    @Override
    protected IAppSuggestManager getDefaultImplementation() {
        return new IAppSuggestManager.NoOp();
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    @Override
    public void onStart() {
        publishService(new BinderService());
    }

    public void publishService(IBinder binder) {
        publishBinderService(CMContextConstants.CM_APP_SUGGEST_SERVICE, binder);
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.APP_SUGGEST;
    }

    private class BinderService extends IAppSuggestManager.Stub {
        @Override
        public boolean handles(Intent intent) throws RemoteException {
            return getBrokeredService().handles(intent);
        }

        @Override
        public List<ApplicationSuggestion> getSuggestions(Intent intent) throws RemoteException {
            return getBrokeredService().getSuggestions(intent);
        }
    }
}
