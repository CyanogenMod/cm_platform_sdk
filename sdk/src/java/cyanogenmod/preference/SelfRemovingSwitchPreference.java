/*
 * Copyright (C) 2016 The CyanogenMod project
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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;

/**
 * A SwitchPreference which can automatically remove itself from the hierarchy
 * based on constraints set in XML.
 */
public class SelfRemovingSwitchPreference extends SwitchPreference {

    private final boolean mAvailable;

    public SelfRemovingSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAvailable = Constraints.checkConstraints(context, attrs);
    }

    public SelfRemovingSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelfRemovingSwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        if (!mAvailable) {
            preferenceManager.getPreferenceScreen().removePreference(this);
        }
    }
}
