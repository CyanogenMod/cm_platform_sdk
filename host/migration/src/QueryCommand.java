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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Essentially:
 * adb shell content query --uri content://settings/secure --projection name:value
 * --where \"name=\'new_setting\'\" --sort \"name ASC\"\n"
 */
public class QueryCommand extends Command {
    private static final String[] QUERY_SETTINGS = {
            "adb", "shell", "content", "query", "--uri" };
    private static final String PROJECTION = "name:value";

    private ArrayList<Setting> targetList;
    private String targetUri;

    protected QueryCommand(String targetUri,
                           ArrayList<Setting> targetList) {
        this.targetUri = targetUri;
        this.targetList = targetList;
    }

    @Override
    public void run() {
        System.out.println("\nQuerying settings for authority "
                + getAuthority() + " for target uri " + targetUri + "...");
        query(targetUri, targetList);
        synchronized (this) {
            notifyAll();
        }
    }

    private void query(String uri, ArrayList<Setting> arrayList) {
        String[] commands = QUERY_SETTINGS;
        List<String> commandList = new ArrayList<String>(
                QUERY_SETTINGS.length + 1);
        commandList.addAll(Arrays.asList(commands));
        commandList.add(SettingsConstants.CONTENT_URI + getAuthority() + uri);
        commandList.add("--projection");
        commandList.add(PROJECTION);
        commandList.add("--show-type"); //this is totally awesomely cm specific
        commandList.add("true");
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

            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("Row: ")) {
                    throw new IOException("Unable to read settings");
                }
                if (MigrationTest.DEBUG) {
                    System.out.println("LINE: " + line);
                }
                Setting setting = RowParser.parseAndPopulate(true, line);
                if (filter(uri, setting)) {
                    continue;
                }
                arrayList.add(setting);
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
