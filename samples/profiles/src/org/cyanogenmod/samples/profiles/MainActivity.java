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

package org.cyanogenmod.samples.profiles;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.profiles.ConnectionSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adnan on 8/4/15.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    private ProfileManager mProfileManager;
    private WifiManager mWifiManager;
    private Profile mProfile;
    private List<WifiTrigger> mTriggers = new ArrayList<WifiTrigger>();
    private Button mProfileButton;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mProfileManager = ProfileManager.getInstance(this);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        populateWifiTriggerList();

        mProfileButton = (Button) findViewById(R.id.create_bluetooth_on_wifi_trigger_connect);
        mTextView = (TextView) findViewById(R.id.create_bt_on_wifi_status);
        mProfileButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        mProfile = new Profile("Enable Bluetooth on WiFi connect");
        mProfile.setProfileType(Profile.Type.TOGGLE);

        final String triggerId;
        final String triggerName;
        final int triggerType;
        final int triggerState;

        WifiTrigger trigger = mTriggers.get(0);         // get first AP, doesn't matter what it is

        // Populate the arguments for the ProfileTrigger
        triggerId = trigger.getSSID();
        triggerName = trigger.getTitle();
        triggerType = Profile.TriggerType.WIFI;         // This is a wifi trigger
        triggerState = Profile.TriggerState.ON_CONNECT; // On Connect of this, trigger

        Profile.ProfileTrigger profileTrigger =
                new Profile.ProfileTrigger(triggerType, triggerId, triggerState, triggerName);

        ConnectionSettings connectionSettings = new ConnectionSettings(
                ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH,
                ConnectionSettings.BooleanState.STATE_ENABLED, true);

        mProfile.setConnectionSettings(connectionSettings);
        mProfile.setTrigger(profileTrigger);

        mProfileManager.addProfile(mProfile);

        mTextView.setText("Set up for AP " + triggerId + "\n" +
                          "With state pending on " + triggerState);
    }

    private void populateWifiTriggerList() {
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                WifiTrigger wifiTrigger = new WifiTrigger(config);
                mTriggers.add(wifiTrigger);
            }
        }
    }

    public static class WifiTrigger {
        public String mSSID;
        public WifiConfiguration mConfig;

        public WifiTrigger(WifiConfiguration config) {
            mConfig = config;
            loadConfig(config);
        }

        public String getSSID() {
            return mSSID;
        }

        public String getTitle() {
            return mSSID;
        }

        private void loadConfig(WifiConfiguration config) {
            mSSID = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
            mConfig = config;
        }

        public static String removeDoubleQuotes(String string) {
            final int length = string.length();
            if (length >= 2) {
                if (string.startsWith("\"") && string.endsWith("\"")) {
                    return string.substring(1, length - 1);
                }
            }
            return string;
        }
    }
}
