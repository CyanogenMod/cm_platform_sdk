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
public class QueryWithTimeout extends CommandWithTimeout {
    private static final String[] QUERY_SETTINGS = {
            "adb", "shell", "busybox", "sh", "system/bin/content", "query", "--uri" };
    private static final String REGEX = "=(.+)";
    private static final String PROJECTION = "name:value";

    private String targetAuthority;
    private String targetUri;
    private ArrayList<Setting> targetList;

    public QueryWithTimeout(String targetAuthority, String targetUri,
            ArrayList<Setting> targetList) {
        this.targetAuthority = targetAuthority;
        this.targetUri = targetUri;
        this.targetList = targetList;
    }

    @Override
    public void run() {
        System.out.println("\nQuerying settings for authority "
                + targetAuthority + " for target uri " + targetUri + "...");
        query(targetAuthority, targetUri, targetList);
        synchronized (this) {
            notifyAll();
        }
    }

    private void query(String authority, String uri, ArrayList<Setting> arrayList) {
        String[] commands = QUERY_SETTINGS;
        List<String> commandList = new ArrayList<String>(
                QUERY_SETTINGS.length + 1);
        commandList.addAll(Arrays.asList(commands));
        commandList.add(MigrationTest.CONTENT_URI + authority + uri);
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
            Pattern p = Pattern.compile(REGEX);
            while ((line = in.readLine()) != null) {
                if (MigrationTest.DEBUG) {
                    System.out.println("LINE: " + line);
                }
                if (line.startsWith("Row: ")) {
                    Setting setting = new Setting();
                    String[] splitStrings = line.split(",");
                    for (int i = 0; i < 4; i++) {
                        Matcher matcher = p.matcher(splitStrings[i]);
                        while (matcher.find()) {
                            String value = matcher.group(0).replace("=", "").trim();
                            switch (i) {
                                case 0:
                                    setting.setKey(value);
                                    break;
                                case 1:
                                    //Seriously?
                                    setting.setKeyType(
                                            Setting.SettingType.mapNumericToType(
                                                    Integer.parseInt(value)));
                                    break;
                                case 2:
                                    setting.setValue(value);
                                    break;
                                case 3:
                                    //Who the fuck decided to do this?
                                    setting.setValueType(
                                            Setting.SettingType.mapNumericToType(
                                                    Integer.parseInt(value)));
                                    break;
                            }
                        }
                    }
                    arrayList.add(setting);
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
