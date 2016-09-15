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
package cyanogenmod.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;

import cyanogenmod.platform.R;

public class SelfRemovingListPreference extends ListPreference {

    private final boolean mAvailable;

    private final boolean mAutoSummary;

    public SelfRemovingListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAvailable = Constraints.checkConstraints(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.cm_ListPreference, 0, 0);

        try {
            mAutoSummary = a.getBoolean(R.styleable.cm_ListPreference_autoSummary, false);
        } finally {
            a.recycle();
        }
    }

    public SelfRemovingListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelfRemovingListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        if (!mAvailable) {
            preferenceManager.getPreferenceScreen().removePreference(this);
        }
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        if (mAutoSummary) {
            setSummary(getEntry());
        }
    }
}
