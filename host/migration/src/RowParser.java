import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by adnan on 11/17/15.
 */
public class RowParser {
    private static final String REGEX = "=(.+)";
    private static Pattern p = Pattern.compile(REGEX);

    public static Setting parseAndPopulate(boolean fromCursor, String line) {
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
                        if (fromCursor) {
                            setting.setKeyType(
                                    Setting.SettingType.mapNumericToType(
                                            Integer.parseInt(value)));
                        } else {
                            setting.setKeyType(value);
                        }
                        break;
                    case 2:
                        setting.setValue(value);
                        break;
                    case 3:
                        //Who the fuck decided to do this?
                        if (fromCursor) {
                            setting.setValueType(
                                    Setting.SettingType.mapNumericToType(
                                            Integer.parseInt(value)));
                        } else {
                            setting.setValueType(value);
                        }
                        break;
                }
            }
        }
        return setting;
    }
}
