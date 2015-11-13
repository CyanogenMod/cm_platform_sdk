import java.util.ArrayList;

/**
 * Created by adnan on 11/16/15.
 */
public class SettingImageCommands implements CommandExecutor {
    private ArrayList<CommandWithTimeout> commandHistory = new ArrayList<CommandWithTimeout>();
    private String authority;

    public SettingImageCommands(String authority) {
        this.authority = authority;
    }

    @Override
    public void execute() {
        for (CommandWithTimeout commandWithTimeout : commandHistory) {
            commandWithTimeout.run();
        }
    }

    private void addCommand(CommandWithTimeout commandWithTimeout) {
        commandWithTimeout.prepend(authority);
        commandHistory.add(commandWithTimeout);
    }

    public void addQuery(String uri, ArrayList<Setting> settings) {
        QueryWithTimeout queryWithTimeout = new QueryWithTimeout(uri, settings);
        addCommand(queryWithTimeout);
    }

    public void addInsert(String uri, Setting setting) {
        InsertWithTimeout insertWithTimeout = new InsertWithTimeout(uri, setting);
        addCommand(insertWithTimeout);
    }
}
