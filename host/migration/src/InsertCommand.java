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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Essentially:
 * adb shell content insert --uri content://settings/secure
 * --bind name:s:new_setting --bind value:s:new_value
 */
public class InsertCommand extends Command {
    private static final String[] INSERT_SETTINGS = {
            "adb", "shell", "content", "insert", "--uri" };

    private String targetUri;
    private Setting targetSetting;

    public InsertCommand(String targetUri, Setting targetSetting) {
        this.targetUri = targetUri;
        this.targetSetting = targetSetting;
    }

    @Override
    public void run() {
        System.out.println("\nWriting setting " + targetSetting.getKey() + " for authority "
                + getAuthority() + " for target uri " + targetUri + "...");
        insert(targetUri, targetSetting);
        synchronized (this) {
            notifyAll();
        }
    }

    private void insert(String uri, Setting setting) {
        String[] commands = INSERT_SETTINGS;
        List<String> commandList = new ArrayList<String>(
                INSERT_SETTINGS.length + 1);
        commandList.addAll(Arrays.asList(commands));
        commandList.add(SettingsConstants.CONTENT_URI + getAuthority() + uri);
        commandList.add("--bind name:" + setting.getKeyType() + ":" + setting.getKey());
        commandList.add("--bind value:" + setting.getValueType() + ":"
                + "\"" + setting.getValue() + "\"");
        commands = commandList.toArray(new String[commandList.size()]);
        if (MigrationTest.DEBUG) {
            System.out.println("Using commands: " + Arrays.toString(commands));
        }
        try {
            final Process process = Runtime.getRuntime().exec(commands);
            final InputStream err = process.getErrorStream();

            // Send error output to stderr.
            Thread errThread = new Thread() {
                @Override
                public void run() {
                    copy(err, System.err);
                }
            };
            errThread.setDaemon(true);
            errThread.start();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String rx = in.readLine();
            if (rx != null) {
                if (MigrationTest.DEBUG) {
                    System.out.println("Received response " + rx);
                }
            }
            in.close();
            err.close();
            process.destroy();
        } catch (IOException e) {
            System.err.println("Error ");
            e.printStackTrace();
        }
    }
}
