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
    private static final int MAX_RETRIES = 20;
    private static final String[] ADB_REBOOT_BOOTLOADER = new String[] {
            "adb", "reboot", "bootloader"
    };
    private static final String[] ADB_CHECK_BOOT_COMPLETE = new String[] {
            "adb", "shell", "getprop", "sys.boot_completed"
    };
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
            case Types.ADB_REBOOT_BOOTLOADER:
                baseCommand = ADB_REBOOT_BOOTLOADER;
                break;
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
            if (baseArg != null && baseArg.equals(REBOOT)) {
                List<String> secondCommandList = new ArrayList<String>(
                        ADB_CHECK_BOOT_COMPLETE.length + 1);
                secondCommandList.addAll(Arrays.asList(ADB_CHECK_BOOT_COMPLETE));
                String[] secondCommands = secondCommandList.toArray(
                        new String[secondCommandList.size()]);
                if (MigrationTest.DEBUG) {
                    System.out.println("Using commands: " + Arrays.toString(secondCommands));
                }
                Process process2;
                BufferedReader in2;
                String line2;
                for (int i = 1; i < MAX_RETRIES; i++) {
                    process2 = Runtime.getRuntime().exec(secondCommands);
                    in2 = new BufferedReader(
                            new InputStreamReader(process2.getInputStream()));
                    if ((line2 = in2.readLine()) != null) {
                        if (line2.equals("1")) {
                            in2.close();
                            process2.destroy();
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
                    in2.close();
                    process2.destroy();
                }
            }
            process.destroy();
        } catch (IOException e) {
            System.err.println("Error ");
            e.printStackTrace();
        }
    }

    public final class Types {
        public static final int ADB_REBOOT_BOOTLOADER = -1;
        public static final int FASTBOOT_FLASH = 0;
        public static final int FASTBOOT_DEVICES = 1;
        public static final int FASTBOOT_REBOOT = 2;
    }
}
