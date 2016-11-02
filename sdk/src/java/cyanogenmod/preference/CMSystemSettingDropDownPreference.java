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
import android.util.AttributeSet;

import cyanogenmod.providers.CMSettings;


public class CMSystemSettingDropDownPreference extends SelfRemovingDropDownPreference {
    public CMSystemSettingDropDownPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CMSystemSettingDropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean persistString(String value) {
        if (shouldPersist()) {
            if (value == getPersistedString(null)) {
                // It's already there, so the same as persisting
                return true;
            }
            CMSettings.System.putString(getContext().getContentResolver(), getKey(), value);
            return true;
        }
        return false;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        String value = CMSettings.System.getString(getContext().getContentResolver(), getKey());
        return value == null ? defaultReturnValue : value;
    }

    @Override
    protected boolean isPersisted() {
        return CMSettings.System.getString(getContext().getContentResolver(), getKey()) != null;
    }

    public int getIntValue(int defValue) {
        return getValue() == null ? defValue : Integer.valueOf(getValue());
    }
}
