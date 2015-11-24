/*
 * Copyright (C) 2015 The CyanogenMod Project
 * This code has been modified.  Portions copyright (C) 2010, T-Mobile USA, Inc.
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

package cyanogenmod.content;

/**
 * CyanogenMod specific intent definition class.
 */
public class Intent {

    /**
     * Activity Action: Start action associated with long press on the recents key.
     * <p>Input: {@link #EXTRA_LONG_PRESS_RELEASE} is set to true if the long press
     * is released
     * <p>Output: Nothing
     * @hide
     */
    public static final String ACTION_RECENTS_LONG_PRESS =
            "cyanogenmod.intent.action.RECENTS_LONG_PRESS";

    /**
     * This field is part of the intent {@link #ACTION_RECENTS_LONG_PRESS}.
     * The type of the extra is a boolean that indicates if the long press
     * is released.
     * @hide
     */
    public static final String EXTRA_RECENTS_LONG_PRESS_RELEASE =
            "cyanogenmod.intent.extra.RECENTS_LONG_PRESS_RELEASE";

    /**
     * Intent filter to update protected app component's settings
     */
    public static final String ACTION_PROTECTED = "cyanogenmod.intent.action.PACKAGE_PROTECTED";

    /**
     * Intent filter to notify change in state of protected application.
     */
    public static final String ACTION_PROTECTED_CHANGED =
            "cyanogenmod.intent.action.PROTECTED_COMPONENT_UPDATE";

    /**
     * This field is part of the intent {@link #ACTION_PROTECTED_CHANGED}.
     * Intent extra field for the state of protected application
     */
    public static final String EXTRA_PROTECTED_STATE =
            "cyanogenmod.intent.extra.PACKAGE_PROTECTED_STATE";

    /**
     * This field is part of the intent {@link #ACTION_PROTECTED_CHANGED}.
     * Intent extra field to indicate protected component value
     */
    public static final String EXTRA_PROTECTED_COMPONENTS =
            "cyanogenmod.intent.extra.PACKAGE_PROTECTED_COMPONENTS";
}
