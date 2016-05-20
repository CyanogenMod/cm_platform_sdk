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

package cyanogenmod.externalviews;

import android.graphics.Rect;

/** @hide */
interface IExternalViewProvider
{
    oneway void onAttach(in IBinder windowToken);
    oneway void onStart();
    oneway void onResume();
    oneway void onPause();
    oneway void onStop();
    oneway void onDetach();

    void alterWindow(in int x, in int y, in int width, in int height, in boolean visible, in Rect clipRect);

    oneway void enableTouchEvents();
    oneway void disableTouchEvents();
}
