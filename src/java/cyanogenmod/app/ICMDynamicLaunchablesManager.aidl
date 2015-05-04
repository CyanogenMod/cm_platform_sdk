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
import android.os.Bundle;

import cyanogenmod.app.CustomTile;
import cyanogenmod.app.ICustomTileListener;

/** @hide */
interface ICMDynamicLaunchablesManager {
    // --- Methods below are for use by 3rd party applications to publish updates
    // to the launcher's dynamic

    // You probably need some permission like the PUBLISH_CUSTOM_TILE used by the notification tiles API

    // Note -- these APIs are for apps that don't want to generate their own widgets.

    void enableDynamicWidgetry(String pkg, boolean enable);

    boolean isEnabledDynamicWidgetry(String pkg);

    // Change the layout used by the app (should contain views id'd as TEXT1, IMAGE1, TEXT2, IMAGE2)
    void setWidgetTemplateLayout(String pkg, int id);

    // Sets the various template values for the widget
    void populateWidgetTemplate(String pkg, in Bundle values);

    /*
    void createCustomTileWithTag(String pkg, String opPkg, String tag, int id,
            in CustomTile tile, inout int[] idReceived, int userId);
    void removeCustomTileWithTag(String pkg, String tag, int id, int userId);

    // --- Methods below are for use by 3rd party applications
    // You need the BIND_QUICK_SETTINGS_TILE_LISTENER permission
    void registerListener(in ICustomTileListener listener, in ComponentName component, int userid);
    void unregisterListener(in ICustomTileListener listener, int userid);
    void removeCustomTileFromListener(in ICustomTileListener listener, String pkg, String tag, int id);
    */
}
