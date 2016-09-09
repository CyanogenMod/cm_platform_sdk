/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.internal.cmparts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;

import org.cyanogenmod.platform.internal.R;

public class CMPartsPreference extends Preference
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "CMPartsPreference";

    public static final String CMPARTS_ACTION = "org.cyanogenmod.cmparts.PART";
    public static final String CATALOG_ACTION = "org.cyanogenmod.cmparts.CATALOG";

    private static final String CMPARTS_PART_PREFIX = "cmparts:";

    private static final String CMPARTS_EXTRA = "part";

    private final Intent mIntent;
    private final String mPart;

    private IPartsCatalog mCatalog;

    private final OnPartChangedCallback mCallback = new OnPartChangedCallback();

    public CMPartsPreference(Context context) {
        this(context, null);
    }

    public CMPartsPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CMPartsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CMPartsPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                   int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        
        mPart = CMPARTS_PART_PREFIX + getKey();

        mIntent = new Intent(CMPARTS_ACTION);
        mIntent.putExtra(CMPARTS_EXTRA, mPart);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        connectCatalog();
    }

    @Override
    public void onDetached() {
        super.onDetached();
        disconnectCatalog();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        getContext().startActivity(mIntent);
        return true;
    }

    private void connectCatalog() {
        Intent i = new Intent(CATALOG_ACTION);
        i.setPackage("org.cyanogenmod.cmparts");
        getContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void disconnectCatalog() {
        try {
            mCatalog.unregisterCallback(mPart, mCallback);
        } catch (RemoteException e) {
        }
        getContext().unbindService(mConnection);
    }

    private void updatePart(PartInfo info) {
        if (info != null) {
            setTitle(info.getTitle());
            setSummary(info.getSummary());
        }
    }

    private class OnPartChangedCallback extends IPartChangedCallback.Stub {
        @Override
        public void onPartChanged(PartInfo info) throws RemoteException {
            updatePart(info);
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mCatalog = IPartsCatalog.Stub.asInterface(iBinder);
            try {
                mCatalog.registerCallback(mPart, mCallback);
                updatePart(mCatalog.getPartInfo(mPart));
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register callback!", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCatalog = null;
        }
    };
}
