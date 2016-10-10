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

import android.content.Context;
import android.util.AttributeSet;

import cyanogenmod.preference.SelfRemovingPreference;

/**
 * A link to a remote preference screen which can be used with a minimum amount
 * of information. Supports summary updates asynchronously.
 */
public class CMPartsPreference extends SelfRemovingPreference implements PartInfo.RemotePart {

    private static final String TAG = "CMPartsPreference";

    private final PartInfo mPart;

    private final Context mContext;

    public CMPartsPreference(Context context, AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.preferenceScreenStyle);
        mContext = context;
        mPart = PartsList.get(context).getPartInfo(getKey());
        if (mPart == null) {
            throw new RuntimeException("Part not found: " + getKey());
        }

        if (!mPart.isAvailable()) {
            setAvailable(false);
        }

        setIntent(mPart.getIntentForActivity());

        onRefresh(context, mPart);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mPart.registerRemote(mContext, this);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mPart.unregisterRemote(mContext, this);
    }

    @Override
    public void onRefresh(Context context, PartInfo info) {
        setTitle(mPart.getTitle());
        setSummary((CharSequence) mPart.getSummary());
    }
}
