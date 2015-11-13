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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by adnan on 11/13/15.
 */
public class Command implements Runnable {
    private String authority;

    /**
     * Override for execution
     */
    @Override
    public void run() {
    }

    /**
     * Copies from one stream to another.
     */
    protected static void copy(InputStream in, OutputStream out) {
        byte[] buffer = new byte[1024];
        int read;
        try {
            while ((read = in.read(buffer)) > -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static boolean filter(String uri, Setting setting) {
        switch (uri) {
            case SettingsConstants.SYSTEM:
                if (!CMSettings.System.isLegacySetting(setting.getKey())) {
                    return true;
                }
                break;
            case SettingsConstants.SECURE:
                if (SettingsConstants.Ignorables.SECURE_SETTINGS.contains(setting.getKey())) {
                    return true;
                }
                if (!CMSettings.Secure.isLegacySetting(setting.getKey())) {
                    return true;
                }
                break;
            case SettingsConstants.GLOBAL:
                if (!CMSettings.Global.isLegacySetting(setting.getKey())) {
                    return true;
                }
                break;
        }
        return false;
    }

    public void prepend(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
