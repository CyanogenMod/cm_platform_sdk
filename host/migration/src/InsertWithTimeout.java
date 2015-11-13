import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Essentially:
 * adb shell content insert --uri content://settings/secure
 * --bind name:s:new_setting --bind value:s:new_value
 */
public class InsertWithTimeout extends CommandWithTimeout {
    private static final String[] INSERT_SETTINGS = {
            "adb", "shell", "busybox", "sh", "system/bin/content", "insert", "--uri" };

    private String targetAuthority;
    private String targetUri;
    private Setting targetSetting;

    public InsertWithTimeout(String targetAuthority, String targetUri, Setting targetSetting) {
        this.targetAuthority = targetAuthority;
        this.targetUri = targetUri;
        this.targetSetting = targetSetting;
    }

    @Override
    public void run() {
        System.out.println("\nWriting setting " + targetSetting.getKey() + " for authority "
                + targetAuthority + " for target uri " + targetUri + "...");
        insert(targetAuthority, targetUri, targetSetting);
        synchronized (this) {
            notifyAll();
        }
    }

    private void insert(String authority, String uri, Setting setting) {
        String[] commands = INSERT_SETTINGS;
        List<String> commandList = new ArrayList<String>(
                INSERT_SETTINGS.length + 1);
        commandList.addAll(Arrays.asList(commands));
        commandList.add(MigrationTest.CONTENT_URI + authority + uri);
        commandList.add("--bind name:" + setting.getKeyType() + ":" + setting.getKey());
        commandList.add("--bind value:" + setting.getValueType() + ":"
                + setting.getValue());
        commands = commandList.toArray(new String[commandList.size()]);
        System.out.println("Using commands: " + Arrays.toString(commands));
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
                System.out.println("Received response " + rx);
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
