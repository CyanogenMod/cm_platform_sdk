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

/**
 * Created by adnan on 11/17/15.
 */
public class FastbootCommand extends Command {
    private static final String FASTBOOT_COMMAND = "fastboot";
    private static final String REBOOT = "reboot";
    private static final String DEVICES = "devices";
    private static final String FLASH = "flash";

    private String[] baseCommand;
    private String baseArg;
    private String image;
    private String targetImage;

    public FastbootCommand(int command, String[] args) {
        switch (command) {
            case Types.FASTBOOT_FLASH:
                baseCommand = new String[] { FASTBOOT_COMMAND };
                baseArg = FLASH;
                image = args[0];
                targetImage = args[1];
                break;
            case Types.FASTBOOT_DEVICES:
                baseCommand = new String[] { FASTBOOT_COMMAND };
                baseArg = DEVICES;
                break;
            case Types.FASTBOOT_REBOOT:
                baseCommand = new String[] { FASTBOOT_COMMAND };
                baseArg = REBOOT;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation " + command);
        }
    }

    @Override
    public void run() {
        List<String> commandList = new ArrayList<String>(
                baseCommand.length + 1);
        commandList.addAll(Arrays.asList(baseCommand));
        if (baseArg != null && baseArg.length() > 0) {
            commandList.add(baseArg);
        }
        if (image != null &&image.length() > 0) {
            commandList.add(image);
        }
        if (targetImage != null &&targetImage.length() > 0) {
            commandList.add(targetImage);
        }
        String[] commands = commandList.toArray(new String[commandList.size()]);

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

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            in.close();
            err.close();
            //Gross
            process.destroy();
        } catch (IOException e) {
            System.err.println("Error ");
            e.printStackTrace();
        }
    }

    public final class Types {
        public static final int FASTBOOT_FLASH = 0;
        public static final int FASTBOOT_DEVICES = 1;
        public static final int FASTBOOT_REBOOT = 2;
    }
}
