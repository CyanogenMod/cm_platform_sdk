/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package org.cyanogenmod.internal.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.internal.widget.LockPatternUtils;

import cyanogenmod.platform.Manifest;

public class CmLockPatternUtils extends LockPatternUtils {

    /**
     * Third party keyguard component to be displayed within the keyguard
     */
    public static final String THIRD_PARTY_KEYGUARD_COMPONENT = "lockscreen.third_party";

    private Context mContext;

    public CmLockPatternUtils(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Sets a third party lock screen.
     * @param component
     */
    public void setThirdPartyKeyguard(ComponentName component)
            throws PackageManager.NameNotFoundException {
        if (component != null) {
            // Check that the package this component belongs to has the third party keyguard perm
            final PackageManager pm = mContext.getPackageManager();
            final boolean hasThirdPartyKeyguardPermission = pm.checkPermission(
                    Manifest.permission.THIRD_PARTY_KEYGUARD, component.getPackageName()) ==
                        PackageManager.PERMISSION_GRANTED;
            if (!hasThirdPartyKeyguardPermission) {
                throw new SecurityException("Package " + component.getPackageName() + " does not" +
                        "have " + Manifest.permission.THIRD_PARTY_KEYGUARD);
            }
        }

        setString(THIRD_PARTY_KEYGUARD_COMPONENT,
                component != null ? component.flattenToString() : "", getCurrentUser());
    }

    /**
     * Get the currently applied 3rd party keyguard component
     * @return
     */
    public ComponentName getThirdPartyKeyguardComponent() {
        String component = getString(THIRD_PARTY_KEYGUARD_COMPONENT, getCurrentUser());
        return component != null ? ComponentName.unflattenFromString(component) : null;
    }

    /**
     * @return Whether a third party keyguard is set
     */
    public boolean isThirdPartyKeyguardEnabled() {
        String component = getString(THIRD_PARTY_KEYGUARD_COMPONENT, getCurrentUser());
        return !TextUtils.isEmpty(component);
    }

    private int getCurrentUser() {
        return UserHandle.USER_CURRENT;
    }
}
