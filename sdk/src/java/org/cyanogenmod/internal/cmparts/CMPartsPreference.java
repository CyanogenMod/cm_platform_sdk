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
import android.os.Bundle;
import android.util.AttributeSet;

import cyanogenmod.preference.RemotePreference;

/**
 * A link to a remote preference screen which can be used with a minimum amount
 * of information. Supports summary updates asynchronously.
 */
public class CMPartsPreference extends RemotePreference {

    private static final String TAG = "CMPartsPreference";

    private final PartInfo mPart;

    private final Context mContext;

    public CMPartsPreference(Context context, AttributeSet attrs,
                            int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        mContext = context;
        mPart = PartsList.get(context).getPartInfo(getKey());
        if (mPart == null) {
            throw new RuntimeException("Part not found: " + getKey());
        }

        if (!mPart.isAvailable()) {
            setAvailable(false);
        }

        setIntent(mPart.getIntentForActivity());
    }

    public CMPartsPreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, com.android.internal.R.attr.preferenceScreenStyle, 0);
    }

    public CMPartsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.preferenceScreenStyle);
    }

    @Override
    protected void onRemoteUpdated(Bundle bundle) {
        PartInfo update = bundle.getParcelable(PartsList.EXTRA_PART);
        if (update != null && mPart.updateFrom(update)) {
            setTitle(mPart.getTitle());
            setSummary((CharSequence) mPart.getSummary());
        }
    }

    @Override
    protected String getRemoteKey(Bundle metaData) {
        // remote key is the same as ours
        return getKey();
    }
}
