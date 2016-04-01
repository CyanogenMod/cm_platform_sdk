/*
** Copyright (C) 2016 The CyanogenMod Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package cyanogenmod.app;

import cyanogenmod.app.ILiveLockScreenChangeListener;
import cyanogenmod.app.LiveLockScreenInfo;

/**
 * Interface to be implemented by services that support the
 * {@link LiveLockScreenManager#SERVICE_INTERFACE}
 * @hide
 */
interface ILiveLockScreenManagerProvider {

    /**
     * Enqueue a Live lock screen to be displayed.
     */
    void enqueueLiveLockScreen(String pkg, int id, in LiveLockScreenInfo lls,
             inout int[] idReceived, int userId);

    /**
     * Cancel displaying a Live lock screen.
     */
    void cancelLiveLockScreen(String pkg, int id, int userId);

    /**
     * Get the current Live lock screen that should be displayed.
     */
    LiveLockScreenInfo getCurrentLiveLockScreen();

    /**
     * Called when the default Live lock screen has changed.
     */
    oneway void updateDefaultLiveLockScreen(in LiveLockScreenInfo llsInfo);

    /**
     * Get the enabled state of the Live lock screen feature.
     */
    boolean getLiveLockScreenEnabled();

    /**
     * Registers an ILiveLockScreenChangeListener that will be called when the current Live lock
     * screen changes.
     */
    boolean registerChangeListener(in ILiveLockScreenChangeListener listener);

    /**
     * Unregisters a previously registered ILiveLockScreenChangeListener.
     */
    boolean unregisterChangeListener(in ILiveLockScreenChangeListener listener);
}