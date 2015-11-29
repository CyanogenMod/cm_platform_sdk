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
 * Created by adnan on 11/29/15.
 */
public class AdbCommand extends Command {
    private static final int MAX_RETRIES = 20;
    private static final String[] ADB_REBOOT_BOOTLOADER = new String[] {
            "adb", "reboot", "bootloader"
    };
    private static final String[] ADB_CHECK_BOOT_COMPLETE = new String[] {
            "adb", "shell", "getprop", "sys.boot_completed"
    };

    private String[] baseCommand;

    public AdbCommand(int command) {
        switch (command) {
            case Types.REBOOT_BOOTLOADER:
                baseCommand = ADB_REBOOT_BOOTLOADER;
                break;
            case Types.CHECK_BOOT_COMPLETE:
                baseCommand = ADB_CHECK_BOOT_COMPLETE;
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
        String[] commands = commandList.toArray(new String[commandList.size()]);

        if (MigrationTest.DEBUG) {
            System.out.println("Using commands: " + Arrays.toString(commands));
        }
        try {
            Process process = Runtime.getRuntime().exec(commands);
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
            if (!baseCommand.equals(ADB_CHECK_BOOT_COMPLETE)) {
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
                process.destroy();
            } else {
                for (int i = 1; i < MAX_RETRIES; i++) {
                    process = Runtime.getRuntime().exec(commands);
                    in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    if ((rx = in.readLine()) != null) {
                        if (rx.equals("1")) {
                            in.close();
                            process.destroy();
                            try {
                                System.out.println("Device up detected...");
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                    try {
                        System.out.println("Waiting for device to come up...");
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    in.close();
                    process.destroy();
                }
            }
        } catch (IOException e) {
            System.err.println("Error ");
            e.printStackTrace();
        }
    }

    public final class Types {
        public static final int REBOOT_BOOTLOADER = 0;
        public static final int CHECK_BOOT_COMPLETE = 1;
    }
}
