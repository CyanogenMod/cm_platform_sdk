package com.example.test.myapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;
import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.profiles.ConnectionSettings;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CMStatusBarManager mCMStatusBarManager;
    private Button mPublishRemoteViewButton;

    private ProfileManager mProfileMangager;
    private WifiManager mWifiManager;
    private Profile mProfile;
    private List<WifiTrigger> mTriggers = new ArrayList<WifiTrigger>();
    private Button mProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // CUSTOM TILES
        mCMStatusBarManager = CMStatusBarManager.getInstance(this);
        mPublishRemoteViewButton = (Button) findViewById(R.id.publish_remote_view_tile);
        mPublishRemoteViewButton.setOnClickListener(this);


        // PROFILES
        mProfileButton = (Button) findViewById(R.id.publish_ap_triggered_profile);
        mProfileButton.setOnClickListener(this);
        mProfileManager = ProfileManager.getInstance(this);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        populateWifiTriggerList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.publish_remote_view_tile:
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("tel:2813308004"));

                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.tile_remote_view);
                remoteViews.setOnClickPendingIntent(R.id.remote_view_button, pendingIntent);

                CustomTile.RemoteExpandedStyle remoteExpandedStyle = new CustomTile.RemoteExpandedStyle();
                remoteExpandedStyle.setRemoteViews(remoteViews);

                Intent deleteIntent = new Intent();
                deleteIntent.setAction(DeleteIntentReceiver.DELETE_ACTION);

                CustomTile customTile = new CustomTile.Builder(this)
                        .setDeleteIntent(PendingIntent.getBroadcast(this, 0, deleteIntent, 0))
                        .setLabel("Remote Tile")
                        .setIcon(R.mipmap.ic_launcher)
                        .setContentDescription("Remote Expanded Style Tile")
                        .setExpandedStyle(remoteExpandedStyle)
                        .build();

                mCMStatusBarManager.publishTile(1337, customTile);
                break;
            case R.id.publish_ap_triggered_profile:
                mProfile = new Profile("Enable Bluetooth on WiFi connect");
                mProfile.setProfileType(Profile.Type.TOGGLE);

                final String triggerId;
                final String triggerName;
                final int triggerType;
                final int triggerState;

                WifiTrigger trigger = mTriggers.get(0);  // get first AP, doesn't matter what it is

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
                mProfileManager.setActiveProfile(mProfile.getUuid());

                Toast.makeText(this, "Set up for AP " + triggerId + "\n" +
                        "With state pending on " + triggerState, Toast.LENGTH_SHORT).show();
                break;
        }
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
