/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.tests.profiles;

import android.app.PendingIntent;
import android.content.Intent;
import cyanogenmod.app.Action;
import cyanogenmod.app.ProfilePluginManager;
import cyanogenmod.app.Trigger;
import org.cyanogenmod.tests.TestActivity;
import org.cyanogenmod.tests.customtiles.CMStatusBarTest;

public class ProfilesPluginTest extends TestActivity {
    private ProfilePluginManager mProfilePluginManager;

    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        mProfilePluginManager = ProfilePluginManager.getInstance(this);
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("Create action and register with service") {
                public void run() {
                    PendingIntent intent = PendingIntent.getActivity(ProfilesPluginTest.this, 0,
                            new Intent(ProfilesPluginTest.this, CMStatusBarTest.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
                    Action newAction = new Action();
                    newAction.setTitle("Action Title");
                    newAction.setDescription("Action description");
                    newAction.setAction(intent);
                    mProfilePluginManager.registerAction(newAction);
                }
            },
            new Test("Create trigger and register with service") {
                public void run() {
                    Trigger trigger = new Trigger();
                    trigger.setTriggerId("FirstTrigger");
                    trigger.setTriggerDisplayName("My Trigger");
                    trigger.setCurrentState("State");
                    trigger.addState(new Trigger.State("test", "state"));
                    trigger.addState(new Trigger.State("test1", "state1"));
                    mProfilePluginManager.registerTrigger(trigger);
                }
            }
    };
}
