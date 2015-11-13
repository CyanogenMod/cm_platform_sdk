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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by adnan on 11/17/15.
 */
public class ReadCommand extends Command {
    private String targetFile;
    private ArrayList<Setting> targetList;
    private String targetUri;

    protected ReadCommand(String targetFile, String targetUri, ArrayList<Setting> targetList) {
        this.targetFile = targetFile;
        this.targetUri = targetUri;
        this.targetList = targetList;
    }

    @Override
    public void run() {
        System.out.println("\nReading settings for authority "
                + getAuthority() + " for target uri " + targetUri + " from file "
                + targetFile +"...");
        read(targetFile, targetUri, targetList);
        synchronized (this) {
            notifyAll();
        }
    }

    private void read(String fileName, String uri, ArrayList<Setting> arrayList) {
        try {
            BufferedReader in = new BufferedReader(
                    new FileReader(fileName));
            String line;
            //Skip first two lines of header
            for (int i = 0; i < 2; i++) {
                in.readLine();
            }
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("Row: ")) {
                    throw new IOException("Unable to read settings");
                }
                if (MigrationTest.DEBUG) {
                    System.out.println("LINE: " + line);
                }
                Setting setting = RowParser.parseAndPopulate(false, line);
                //Sanitize
                if (filter(uri, setting)) {
                    continue;
                }
                arrayList.add(setting);
            }
            in.close();
        } catch (IOException e) {
            System.err.println("Error ");
            e.printStackTrace();
        }
    }
}
