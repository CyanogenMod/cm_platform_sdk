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

import android.content.ComponentName;

import cyanogenmod.app.CustomTile;
import cyanogenmod.app.ICustomTileListener;

/** @hide */
interface ICMStatusBarManager {
    // --- Methods below are for use by 3rd party applications to publish quick
    // settings tiles to the status bar panel
    // You need the PUBLISH_CUSTOM_TILE permission
    void createCustomTileWithTag(String pkg, String opPkg, String tag, int id,
            in CustomTile tile, inout int[] idReceived, int userId);
    void removeCustomTileWithTag(String pkg, String tag, int id, int userId);

    // --- Methods below are for use by 3rd party applications
    // You need the BIND_QUICK_SETTINGS_TILE_LISTENER permission
    void registerListener(in ICustomTileListener listener, in ComponentName component, int userid);
    void unregisterListener(in ICustomTileListener listener, int userid);
    void removeCustomTileFromListener(in ICustomTileListener listener, String pkg, String tag, int id);
}
