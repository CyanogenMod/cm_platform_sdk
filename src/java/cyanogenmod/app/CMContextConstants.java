/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cyanogenmod.app;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class CMContextConstants {

    /**
     * @hide
     */
    private CMContextConstants() {
        // Empty constructor
    }

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.CMStatusBarManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.CMStatusBarManager
     */
    public static final String CM_STATUS_BAR_SERVICE = "cmstatusbar";

    /**
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.CMStatusBarManager
     */
    public static final String CM_PROFILE_PLUGIN_SERVICE = "cmprofileplugin";
}
