/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.os.Build;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ThemeVersion {
    private static final String THEME_VERSION_CLASS_NAME = "android.content.ThemeVersion";
    private static final String THEME_VERSION_FIELD_NAME = "THEME_VERSION";
    private static final String MIN_SUPPORTED_THEME_VERSION_FIELD_NAME =
            "MIN_SUPPORTED_THEME_VERSION";
    private static final int CM11 = 1;
    private static final int CM12_PRE_VERSIONING = 2;

    public static int getVersion() {
        int version;
        try {
            Class<?> themeVersionClass = Class.forName(THEME_VERSION_CLASS_NAME);
            Field themeVersionField = themeVersionClass.getField(THEME_VERSION_FIELD_NAME);
            version = (Integer) themeVersionField.get(null);
        } catch(Exception e) {
            // Field doesn't exist. Fallback to SDK level
            version = Build.VERSION.SDK_INT < 21 ? CM11 :
                    CM12_PRE_VERSIONING;
        }
        return version;
    }

    public static int getMinSupportedVersion() {
        int getMinSupportedVersion;
        try {
            Class<?> themeVersionClass = Class.forName(THEME_VERSION_CLASS_NAME);
            Field themeVersionField =
                    themeVersionClass.getField(MIN_SUPPORTED_THEME_VERSION_FIELD_NAME);
            getMinSupportedVersion = (Integer) themeVersionField.get(null);
        } catch(Exception e) {
            // Field doesn't exist. Fallback to SDK level
            getMinSupportedVersion = Build.VERSION.SDK_INT < 21 ? CM11 :
                    CM12_PRE_VERSIONING;
        }
        return getMinSupportedVersion;
    }

    public static ComponentVersion getComponentVersion(ThemeComponent component) {
        int version = getVersion();
        if (version == 1) {
            throw new UnsupportedOperationException();
        } else if (version == 2) {
            return ThemeVersionImpl2.getDeviceComponentVersion(component);
        } else {
            return ThemeVersionImpl3.getDeviceComponentVersion(component);
        }
    }

    public static List<ComponentVersion> getComponentVersions() {
        int version = getVersion();
        if (version == 1) {
            throw new UnsupportedOperationException();
        } else if (version == 2) {
            return ThemeVersionImpl2.getDeviceComponentVersions();
        } else {
            return ThemeVersionImpl3.getDeviceComponentVersions();
        }
    }

    public static class ComponentVersion {
        protected String name;
        protected ThemeComponent component;
        protected int minVersion;
        protected int currentVersion;

        protected ComponentVersion(ThemeComponent component, int targetVersion) {
            this(component, component.name(), targetVersion, targetVersion);
        }

        protected ComponentVersion(ThemeComponent component,
                                String name,
                                int minVersion,
                                int targetVersion) {
            this.component = component;
            this.name = name;
            this.minVersion = minVersion;
            this.currentVersion = targetVersion;
        }

        public ComponentVersion(ComponentVersion copy) {
            this(copy.component, copy.name, copy.minVersion, copy.currentVersion);
        }

        public String getName() {
            return name;
        }

        public ThemeComponent getComponent() {
            return component;
        }

        public int getMinVersion() {
            return minVersion;
        }

        public int getCurrentVersion() {
            return currentVersion;
        }
    }

    public static class ThemeVersionImpl2 {
        private static ArrayList<ComponentVersion> cVersions = new ArrayList<ComponentVersion>() {
            {
                add(new ComponentVersion(ThemeComponent.OVERLAY, 2));
                add(new ComponentVersion(ThemeComponent.BOOT_ANIM, 1));
                add(new ComponentVersion(ThemeComponent.WALLPAPER, 1));
                add(new ComponentVersion(ThemeComponent.LOCKSCREEN, 1));
                add(new ComponentVersion(ThemeComponent.ICON, 1));
                add(new ComponentVersion(ThemeComponent.FONT, 1));
                add(new ComponentVersion(ThemeComponent.SOUND, 1));
            }
        };

        public static ComponentVersion getDeviceComponentVersion(ThemeComponent component) {
            for(ComponentVersion compVersion : cVersions) {
                if (compVersion.component.equals(component)) {
                    return new ComponentVersion(compVersion);
                }
            }
            return null;
        }

        public static List<ComponentVersion> getDeviceComponentVersions() {
            ArrayList<ComponentVersion> versions = new ArrayList<ComponentVersion>();
            versions.addAll(cVersions);
            return versions;
        }
    }

    public static class ThemeVersionImpl3 {
        public static ComponentVersion getDeviceComponentVersion(ThemeComponent component) {
            for(android.content.ThemeVersion.ComponentVersion version :
                    android.content.ThemeVersion.ComponentVersion.values()) {
                ComponentVersion sdkVersionInfo = fwCompVersionToSdkVersion(version);
                if (sdkVersionInfo.component.equals(component)) {
                    return sdkVersionInfo;
                }
            }
            return null;
        }

        public static List<ComponentVersion> getDeviceComponentVersions() {
            List<ComponentVersion> versions = new ArrayList<ComponentVersion>();

            for(android.content.ThemeVersion.ComponentVersion version :
                    android.content.ThemeVersion.ComponentVersion.values()) {
                versions.add(fwCompVersionToSdkVersion(version));
            }

            return versions;
        }

        public static ComponentVersion fwCompVersionToSdkVersion(
                android.content.ThemeVersion.ComponentVersion version) {
            int ordinal = version.ordinal();
            ThemeComponent component = ThemeComponent.UNKNOWN;

            // The ordinals between framework and SDK must match up except the
            // because of the UNKNOWN value we offset by 1
            if (ordinal + 1 < ThemeComponent.values().length) {
                component = ThemeComponent.values()[ordinal + 1];
            }

            String name = version.name();
            int minVersion = version.minSupportedVersion;
            int targetVersion = version.currentVersion;

            return new ComponentVersion(component, name, minVersion, targetVersion);
        }
    }
}
