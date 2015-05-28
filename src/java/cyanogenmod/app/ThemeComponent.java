/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cyanogenmod.app;

/**
 * The id value here matches the framework. Unknown is given a -1 value since future
 * framework components will always be positive.
 * @hide
 */
public enum ThemeComponent {
    UNKNOWN(-1),
    OVERLAY(0),
    BOOT_ANIM(1),
    WALLPAPER(2),
    LOCKSCREEN(3),
    FONT(4),
    ICON(5),
    SOUND(6);

    public int id;
    ThemeComponent(int id) {
        this.id = id;
    }

}
