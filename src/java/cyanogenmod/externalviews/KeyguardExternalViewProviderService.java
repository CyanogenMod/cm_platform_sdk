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

import android.os.Bundle;
import android.view.WindowManager;

/**
 * TODO: unhide once documented and finalized
 * @hide
 */
public abstract class KeyguardExternalViewProviderService extends ExternalViewProviderService {

    private static final String TAG = KeyguardExternalViewProviderService.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected abstract class Provider extends ExternalViewProviderService.Provider {
        protected Provider(Bundle options) {
            super(options);
        }

        /*package*/ final int getWindowType() {
            return WindowManager.LayoutParams.TYPE_KEYGUARD_PANEL;
        }

        /*package*/ final int getWindowFlags() {
            return WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
    }
}
