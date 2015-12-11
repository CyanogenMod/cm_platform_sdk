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
package cyanogenmod.externalviews;

import android.content.Context;
import android.content.ComponentName;
import android.util.AttributeSet;

public final class KeyguardExternalView extends ExternalView {

    public static final String EXTRA_PERMISSION_LIST = "permissions_list";
    public static final String CATEGORY_KEYGUARD_GRANT_PERMISSION
            = "org.cyanogenmod.intent.category.KEYGUARD_GRANT_PERMISSION";

    public KeyguardExternalView(Context context, AttributeSet attrs) {
        super(context,attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context,attrs,defStyleAttr,defStyleRes);
    }

    public KeyguardExternalView(Context context, AttributeSet attributeSet,
            ComponentName componentName) {
        super(context,attributeSet,componentName);
    }

}
