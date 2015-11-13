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

import java.util.ArrayList;

/**
 * Created by adnan on 11/16/15.
 */
public class SettingImageCommands implements CommandExecutor {
    private ArrayList<Command> commandHistory = new ArrayList<Command>();
    private String authority;

    public SettingImageCommands(String authority) {
        this.authority = authority;
    }

    @Override
    public void execute() {
        for (Command commandWithTimeout : commandHistory) {
            commandWithTimeout.run();
        }
    }

    private void addCommand(Command commandWithTimeout) {
        commandWithTimeout.prepend(authority);
        commandHistory.add(commandWithTimeout);
    }

    public void addQuery(String uri, ArrayList<Setting> settings) {
        QueryCommand queryCommand = new QueryCommand(uri, settings);
        addCommand(queryCommand);
    }

    public void addInsert(String uri, Setting setting) {
        InsertCommand insertCommand = new InsertCommand(uri, setting);
        addCommand(insertCommand);
    }

    public void addRead(String fileName, String uri, ArrayList<Setting> settings) {
        ReadCommand readCommand = new ReadCommand(fileName, uri, settings);
        addCommand(readCommand);
    }

    public void addFastboot(int command, String[] arguments) {
        FastbootCommand fastbootCommand = new FastbootCommand(command, arguments);
        addCommand(fastbootCommand);
    }
}
