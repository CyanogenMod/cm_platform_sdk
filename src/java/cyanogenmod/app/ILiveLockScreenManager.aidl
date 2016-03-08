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

/** {@hide} */
interface ILiveLockScreenManager {
    oneway void enqueueLiveLockScreen(String pkg, int id, in LiveLockScreenInfo lls);
    oneway void cancelLiveLockScreen(String pkg, int id);

    LiveLockScreenInfo getCurrentLiveLockScreen();
    LiveLockScreenInfo getDefaultLiveLockScreen();
    oneway void setDefaultLiveLockScreen(in LiveLockScreenInfo llsInfo);

    oneway void setLiveLockScreenEnabled(boolean enabled);
    boolean getLiveLockScreenEnabled();

    boolean registerChangeListener(in ILiveLockScreenChangeListener listener);
    boolean unregisterChangeListener(in ILiveLockScreenChangeListener listener);
}