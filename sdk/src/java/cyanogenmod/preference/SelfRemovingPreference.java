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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;

/**
 * A Preference which can automatically remove itself from the hierarchy
 * based on constraints set in XML.
 */
public class SelfRemovingPreference extends Preference {

    private final ConstraintsHelper mConstraints;


    public SelfRemovingPreference(Context context, AttributeSet attrs,
                                  int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        mConstraints = new ConstraintsHelper(context, attrs, this);
    }

    public SelfRemovingPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mConstraints = new ConstraintsHelper(context, attrs, this);
    }

    public SelfRemovingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConstraints = new ConstraintsHelper(context, attrs, this);
    }

    public SelfRemovingPreference(Context context) {
        super(context);
        mConstraints = new ConstraintsHelper(context, null, this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mConstraints.applyConstraints();
    }

    protected void setAvailable(boolean available) {
        mConstraints.setAvailable(available);
    }
}
