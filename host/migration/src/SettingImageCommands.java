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
