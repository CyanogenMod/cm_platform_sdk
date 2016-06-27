/*
 * Copyright (c) 2011-2015 CyanogenMod Project
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

package org.cyanogenmod.platform.internal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Message;
import android.util.ArraySet;
import com.android.internal.policy.IKeyguardService;
import cyanogenmod.providers.CMSettings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ActivityManagerNative;
import android.app.NotificationGroup;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.os.ParcelUuid;

import com.android.server.SystemService;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileGroup;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.app.IProfileManager;

import java.util.Collection;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** @hide */
public class ProfileManagerService extends CMSystemService {

    private static final String TAG = "CMProfileService";
    // Enable the below for detailed logging of this class
    private static final boolean LOCAL_LOGV = false;

    public static final String KEYGUARD_PACKAGE = "com.android.systemui";
    public static final String KEYGUARD_CLASS = "com.android.systemui.keyguard.KeyguardService";

    /* package */ static final File PROFILE_FILE =
            new File(Environment.getSystemSecureDirectory(), "profiles.xml");

    private static final int MSG_SEND_PROFILE_STATE = 10;

    private Map<UUID, Profile> mProfiles;

    // Match UUIDs and names, used for reverse compatibility
    private Map<String, UUID> mProfileNames;

    private Map<UUID, NotificationGroup> mGroups;

    private Profile mActiveProfile;

    // Well-known UUID of the wildcard group
    private static final UUID mWildcardUUID =
            UUID.fromString("a126d48a-aaef-47c4-baed-7f0e44aeffe5");
    private NotificationGroup mWildcardGroup;

    private Context mContext;
    private Handler mHandler;
    private boolean mDirty;
    private BackupManager mBackupManager;
    private ProfileTriggerHelper mTriggerHelper;
    private Profile mEmptyProfile;

    private Runnable mBindKeyguard = new Runnable() {
        @Override
        public void run() {
            bindKeyguard();
        }
    };
    private IKeyguardService mKeyguardService;
    private final ServiceConnection mKeyguardConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (LOCAL_LOGV) Log.v(TAG, "*** Keyguard connected (yay!)");
            mKeyguardService = IKeyguardService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (LOCAL_LOGV) Log.v(TAG, "*** Keyguard disconnected, retrying connection soon.");
            mKeyguardService = null;
            // system UI died? retry connection in 5s
            mHandler.removeCallbacks(mBindKeyguard);
            mHandler.postDelayed(mBindKeyguard, 5000);
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                persistIfDirty();
                initialize();
            } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
                persistIfDirty();
            }
        }
    };

    private final Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEND_PROFILE_STATE:
                    Intent newState = new Intent(ProfileManager.PROFILES_STATE_CHANGED_ACTION);
                    newState.putExtra(ProfileManager.EXTRA_PROFILES_STATE, msg.arg1);

                    mContext.sendBroadcastAsUser(newState, UserHandle.ALL);

                    if (ProfileManager.PROFILES_STATE_ENABLED == msg.arg1) {
                        maybeApplyActiveProfile();
                    }
                    return true;
            }
            return false;
        }
    };

    private void maybeApplyActiveProfile() {
        final List<Profile.ProfileTrigger> wiFiTriggers
                = mActiveProfile.getTriggersFromType(Profile.TriggerType.WIFI);
        final List<Profile.ProfileTrigger> blueToothTriggers
                = mActiveProfile.getTriggersFromType(Profile.TriggerType.BLUETOOTH);

        boolean selectProfile = false;
        if (wiFiTriggers.size() == 0 && blueToothTriggers.size() == 0) {
            selectProfile = true;
        } else {
            final String activeSSID = getActiveSSID();
            if (activeSSID != null) {
                for (Profile.ProfileTrigger trigger : wiFiTriggers) {
                    if (trigger.getState() == Profile.TriggerState.ON_CONNECT
                            && trigger.getId().equals(activeSSID)) {
                        selectProfile = true;
                        break;
                    }
                }
            }
            if (!selectProfile && blueToothTriggers.size() > 0) {
                final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                final Set<String> connectedBTDevices = new ArraySet<>();
                for (BluetoothDevice device : pairedDevices) {
                    if (device.isConnected()) connectedBTDevices.add(device.getAddress());
                }
                for (Profile.ProfileTrigger trigger : blueToothTriggers) {
                    if (connectedBTDevices.contains(trigger.getId())
                            && trigger.getState() == Profile.TriggerState.ON_CONNECT) {
                        selectProfile = true;
                        break;
                    }
                }
            }
        }

        if (selectProfile) mActiveProfile.doSelect(mContext, mKeyguardService);
    }

    private String getActiveSSID() {
        final WifiManager wifiManager
                = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiinfo = wifiManager.getConnectionInfo();
        if (wifiinfo == null) {
            return null;
        }
        final WifiSsid ssid = wifiinfo.getWifiSsid();
        if (ssid == null) {
            return null;
        }
        return ssid.toString();
    }

    private class ProfilesObserver extends ContentObserver {
        public ProfilesObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            int state = CMSettings.System.getInt(mContext.getContentResolver(),
                    CMSettings.System.SYSTEM_PROFILES_ENABLED,
                    ProfileManager.PROFILES_STATE_ENABLED);
            mHandler.obtainMessage(MSG_SEND_PROFILE_STATE, state, 0 /* unused */).sendToTarget();
        }
    }

    public ProfileManagerService(Context context) {
        super(context);
        mContext = context;
        mHandler = new Handler(mHandlerCallback);
        if (context.getPackageManager().hasSystemFeature(
                CMContextConstants.Features.PROFILES)) {
            publishBinderService(CMContextConstants.CM_PROFILE_SERVICE, mService);
        } else {
            Log.wtf(TAG, "CM profile service started by system server but feature xml not" +
                    " declared. Not publishing binder service!");
        }
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.PROFILES;
    }

    @Override
    public void onStart() {
        mBackupManager = new BackupManager(mContext);

        mWildcardGroup = new NotificationGroup(
                mContext.getString(org.cyanogenmod.platform.internal.R.string.wildcardProfile),
                org.cyanogenmod.platform.internal.R.string.wildcardProfile,
                mWildcardUUID);

        initialize();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        mContext.registerReceiver(mIntentReceiver, filter);
    }

    private void bindKeyguard() {
        if (mKeyguardService == null) {
            Intent intent = new Intent();
            intent.setClassName(KEYGUARD_PACKAGE, KEYGUARD_CLASS);
            if (!mContext.bindServiceAsUser(intent, mKeyguardConnection,
                    Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
                Log.e(TAG, "error binding to keyguard service");
            }
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            bindKeyguard();
        } else if (phase == PHASE_BOOT_COMPLETED) {
            mContext.getContentResolver().registerContentObserver(
                    CMSettings.System.getUriFor(CMSettings.System.SYSTEM_PROFILES_ENABLED),
                    false, new ProfilesObserver(mHandler), UserHandle.USER_ALL);
        }
    }

    private void initialize() {
        initialize(false);
    }

    private void initialize(boolean skipFile) {
        mTriggerHelper = new ProfileTriggerHelper(mContext, mHandler, this);
        mProfiles = new HashMap<UUID, Profile>();
        mProfileNames = new HashMap<String, UUID>();
        mGroups = new HashMap<UUID, NotificationGroup>();
        mEmptyProfile = new Profile("EmptyProfile");
        mDirty = false;

        boolean init = skipFile;

        if (!skipFile) {
            try {
                loadFromFile();
            } catch (XmlPullParserException e) {
                init = true;
            } catch (IOException e) {
                init = true;
            }
        }

        if (init) {
            try {
                initialiseStructure();
            } catch (Throwable ex) {
                Log.e(TAG, "Error loading xml from resource: ", ex);
            }
        }
    }

    private final IBinder mService = new IProfileManager.Stub() {

        @Override
        public void resetAll() {
            long token = clearCallingIdentity();
            enforceChangePermissions();
            initialize(true);
            restoreCallingIdentity(token);
        }

        @Override
        @Deprecated
        public boolean setActiveProfileByName(String profileName) {
            if (!isEnabled()) {
                Log.w(TAG, "Unable to set active profile because profiles are disabled.");
                return false;
            }
            if (!mProfileNames.containsKey(profileName)) {
                // Since profileName could not be casted into a UUID, we can call it a string.
                Log.w(TAG, "Unable to find profile to set active, based on string: " + profileName);
                return false;
            }

            if (LOCAL_LOGV) {
                Log.v(TAG, "setActiveProfile(String) found profile name in mProfileNames.");
            }
            /*
             * We need to clear the caller's identity in order to
             * - allow the profile switch to execute actions
             *   not included in the caller's permissions
             * - broadcast INTENT_ACTION_PROFILE_SELECTED
             */
            long token = clearCallingIdentity();
            setActiveProfileInternal(mProfiles.get(mProfileNames.get(profileName)), true);
            restoreCallingIdentity(token);
            return true;
        }

        @Override
        public boolean setActiveProfile(ParcelUuid profileParcelUuid) {
            if (!isEnabled()) {
                Log.w(TAG, "Unable to set active profile because profiles are disabled.");
                return false;
            }
            /*
             * We need to clear the caller's identity in order to
             * - allow the profile switch to execute actions
             *   not included in the caller's permissions
             * - broadcast INTENT_ACTION_PROFILE_SELECTED
             */
            long token = clearCallingIdentity();
            boolean ret = setActiveProfileInternal(profileParcelUuid.getUuid(), true);
            restoreCallingIdentity(token);
            return ret;
        }

        @Override
        public boolean addProfile(Profile profile) {
            enforceChangePermissions();
            addProfileInternal(profile);
            long token = clearCallingIdentity();
            persistIfDirty();
            restoreCallingIdentity(token);
            return true;
        }

        @Override
        @Deprecated
        public Profile getProfileByName(String profileName) {
            if (mProfileNames.containsKey(profileName)) {
                return mProfiles.get(mProfileNames.get(profileName));
            } else if (mProfiles.containsKey(UUID.fromString((profileName)))) {
                return mProfiles.get(UUID.fromString(profileName));
            } else {
                return null;
            }
        }

        @Override
        public Profile getProfile(ParcelUuid profileParcelUuid) {
            UUID profileUuid = profileParcelUuid.getUuid();
            return getProfileInternal(profileUuid);
        }

        @Override
        public Profile[] getProfiles() {
            Profile[] profiles = getProfileList().toArray(new Profile[mProfiles.size()]);
            Arrays.sort(profiles);
            return profiles;
        }

        @Override
        public Profile getActiveProfile() {
            if (!isEnabled()) {
                // Profiles are not enabled, return empty profile
                return mEmptyProfile;
            }
            return getActiveProfileInternal();
        }

        @Override
        public boolean removeProfile(Profile profile) {
            enforceChangePermissions();
            if (mProfileNames.remove(profile.getName()) != null
                    && mProfiles.remove(profile.getUuid()) != null) {
                mDirty = true;
                long token = clearCallingIdentity();
                persistIfDirty();
                restoreCallingIdentity(token);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void updateProfile(Profile profile) {
            enforceChangePermissions();
            Profile old = mProfiles.get(profile.getUuid());

            if (old == null) {
                return;
            }

            mProfileNames.remove(old.getName());
            mProfileNames.put(profile.getName(), profile.getUuid());
            mProfiles.put(profile.getUuid(), profile);
            /* no need to set mDirty, if the profile was actually changed,
             * it's marked as dirty by itself */
            long token = clearCallingIdentity();
            persistIfDirty();

            // Also update if we changed the active profile
            if (mActiveProfile != null && mActiveProfile.getUuid().equals(profile.getUuid())) {
                setActiveProfileInternal(profile, true);
            }
            restoreCallingIdentity(token);
        }

        @Override
        public boolean profileExists(ParcelUuid profileUuid) {
            return mProfiles.containsKey(profileUuid.getUuid());
        }

        @Override
        @Deprecated
        public boolean profileExistsByName(String profileName) {
            for (Map.Entry<String, UUID> entry : mProfileNames.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(profileName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @Deprecated
        public boolean notificationGroupExistsByName(String notificationGroupName) {
            for (NotificationGroup group : mGroups.values()) {
                if (group.getName().equalsIgnoreCase(notificationGroupName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public NotificationGroup[] getNotificationGroups() {
            return mGroups.values().toArray(new NotificationGroup[mGroups.size()]);
        }

        @Override
        public void addNotificationGroup(NotificationGroup group) {
            enforceChangePermissions();
            addNotificationGroupInternal(group);
            long token = clearCallingIdentity();
            persistIfDirty();
            restoreCallingIdentity(token);
        }

        @Override
        public void removeNotificationGroup(NotificationGroup group) {
            enforceChangePermissions();
            mDirty |= mGroups.remove(group.getUuid()) != null;
            // Remove the corresponding ProfileGroup from all the profiles too if
            // they use it.
            for (Profile profile : mProfiles.values()) {
                profile.removeProfileGroup(group.getUuid());
            }
            long token = clearCallingIdentity();
            persistIfDirty();
            restoreCallingIdentity(token);
        }

        @Override
        public void updateNotificationGroup(NotificationGroup group) {
            enforceChangePermissions();
            NotificationGroup old = mGroups.get(group.getUuid());
            if (old == null) {
                return;
            }

            mGroups.put(group.getUuid(), group);
            /* no need to set mDirty, if the group was actually changed,
             * it's marked as dirty by itself */
            long token = clearCallingIdentity();
            persistIfDirty();
            restoreCallingIdentity(token);
        }

        @Override
        public NotificationGroup getNotificationGroupForPackage(String pkg) {
            for (NotificationGroup group : mGroups.values()) {
                if (group.hasPackage(pkg)) {
                    return group;
                }
            }
            return null;
        }

        @Override
        public NotificationGroup getNotificationGroup(ParcelUuid uuid) {
            if (uuid.getUuid().equals(mWildcardGroup.getUuid())) {
                return mWildcardGroup;
            }
            return mGroups.get(uuid.getUuid());
        }

        @Override
        public boolean isEnabled() {
            long token = clearCallingIdentity();
            try {
                return CMSettings.System.getIntForUser(mContext.getContentResolver(),
                        CMSettings.System.SYSTEM_PROFILES_ENABLED,
                        ProfileManager.PROFILES_STATE_ENABLED,
                        UserHandle.USER_CURRENT) == ProfileManager.PROFILES_STATE_ENABLED;
            } finally {
                restoreCallingIdentity(token);
            }
        }
    };

    private void addProfileInternal(Profile profile) {
        // Make sure this profile has all of the correct groups.
        for (NotificationGroup group : mGroups.values()) {
            ensureGroupInProfile(profile, group, false);
        }
        ensureGroupInProfile(profile, mWildcardGroup, true);
        mProfiles.put(profile.getUuid(), profile);
        mProfileNames.put(profile.getName(), profile.getUuid());
        mDirty = true;
    }

    private void ensureGroupInProfile(Profile profile,
                                      NotificationGroup group, boolean defaultGroup) {
        if (profile.getProfileGroup(group.getUuid()) != null) {
            return;
        }

        /* enforce a matchup between profile and notification group, which not only
         * works by UUID, but also by name for backwards compatibility */
        for (ProfileGroup pg : profile.getProfileGroups()) {
            if (pg.matches(group, defaultGroup)) {
                return;
            }
        }

        /* didn't find any, create new group */
        profile.addProfileGroup(new ProfileGroup(group.getUuid(), defaultGroup));
    }

    private Profile getProfileInternal(UUID profileUuid) {
        // use primary UUID first
        if (mProfiles.containsKey(profileUuid)) {
            return mProfiles.get(profileUuid);
        }
        // if no match was found: try secondary UUID
        for (Profile p : mProfiles.values()) {
            for (UUID uuid : p.getSecondaryUuids()) {
                if (profileUuid.equals(uuid)) {
                    return p;
                }
            }
        }
        // nothing found
        return null;
    }

    /* package */ Collection<Profile> getProfileList() {
        return mProfiles.values();
    }

    private String getXmlString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<profiles>\n<active>");
        builder.append(TextUtils.htmlEncode(mActiveProfile.getUuid().toString()));
        builder.append("</active>\n");

        for (Profile p : mProfiles.values()) {
            p.getXmlString(builder, mContext);
        }
        for (NotificationGroup g : mGroups.values()) {
            g.getXmlString(builder, mContext);
        }
        builder.append("</profiles>\n");
        return builder.toString();
    }

    private synchronized void persistIfDirty() {
        boolean dirty = mDirty;
        if (!dirty) {
            for (Profile profile : mProfiles.values()) {
                if (profile.isDirty()) {
                    dirty = true;
                    break;
                }
            }
        }
        if (!dirty) {
            for (NotificationGroup group : mGroups.values()) {
                if (group.isDirty()) {
                    dirty = true;
                    break;
                }
            }
        }
        if (dirty) {
            try {
                Log.d(TAG, "Saving profile data...");
                FileWriter fw = new FileWriter(PROFILE_FILE);
                fw.write(getXmlString());
                fw.close();
                Log.d(TAG, "Save completed.");
                mDirty = false;
                mBackupManager.dataChanged();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void enforceChangePermissions() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.MODIFY_PROFILES,
                "You do not have permissions to change the Profile Manager.");
    }

    // Called by SystemBackupAgent after files are restored to disk.
    void settingsRestored() {
        initialize();
        for (Profile p : mProfiles.values()) {
            p.validateRingtones(mContext);
        }
        persistIfDirty();
    }

    private void loadFromFile() throws XmlPullParserException, IOException {
        XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
        XmlPullParser xpp = xppf.newPullParser();
        FileReader fr = new FileReader(PROFILE_FILE);
        xpp.setInput(fr);
        loadXml(xpp, mContext);
        fr.close();
        persistIfDirty();
    }

    private void loadXml(XmlPullParser xpp, Context context) throws
            XmlPullParserException, IOException {
        int event = xpp.next();
        String active = null;
        while (event != XmlPullParser.END_TAG || !"profiles".equals(xpp.getName())) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("active")) {
                    active = xpp.nextText();
                    Log.d(TAG, "Found active: " + active);
                } else if (name.equals("profile")) {
                    Profile prof = Profile.fromXml(xpp, context);
                    addProfileInternal(prof);
                    // Failsafe if no active found
                    if (active == null) {
                        active = prof.getUuid().toString();
                    }
                } else if (name.equals("notificationGroup")) {
                    NotificationGroup ng = NotificationGroup.fromXml(xpp, context);
                    addNotificationGroupInternal(ng);
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while reading " + PROFILE_FILE);
            }
            event = xpp.next();
        }
        // Don't do initialisation on startup. The AudioManager doesn't exist yet
        // and besides, the volume settings will have survived the reboot.
        try {
            // Try / catch block to detect if XML file needs to be upgraded.
            setActiveProfileInternal(UUID.fromString(active), false);
        } catch (IllegalArgumentException e) {
            if (mProfileNames.containsKey(active)) {
                setActiveProfileInternal(mProfileNames.get(active), false);
            } else {
                // Final fail-safe: We must have SOME profile active.
                // If we couldn't select one by now, we'll pick the first in the set.
                setActiveProfileInternal(mProfiles.values().iterator().next(), false);
            }
            // This is a hint that we probably just upgraded the XML file. Save changes.
            mDirty = true;
        }
    }

    private void initialiseStructure() throws XmlPullParserException, IOException {
        XmlResourceParser xml = mContext.getResources().getXml(
                org.cyanogenmod.platform.internal.R.xml.profile_default);
        try {
            loadXml(xml, mContext);
            mDirty = true;
            persistIfDirty();
        } finally {
            xml.close();
        }
    }

    private boolean setActiveProfileInternal(UUID profileUuid, boolean doInit) {
        if (!mProfiles.containsKey(profileUuid)) {
            Log.e(TAG, "Cannot set active profile to: "
                    + profileUuid.toString() + " - does not exist.");
            return false;
        }

        if (LOCAL_LOGV) Log.v(TAG, "setActiveProfile(UUID, boolean) found UUID in mProfiles.");
        setActiveProfileInternal(mProfiles.get(profileUuid), doInit);
        return true;
    }

    /* package */ Profile getActiveProfileInternal() {
        return mActiveProfile;
    }

    /* package */ void setActiveProfileInternal(Profile newActiveProfile, boolean doInit) {
        /*
         * NOTE: Since this is not a public function, and all public functions
         * take either a string or a UUID, the active profile should always be
         * in the collection.  If writing another setActiveProfile which receives
         * a Profile object, run enforceChangePermissions, add the profile to the
         * list, and THEN add it.
         */

        enforceChangePermissions();

        Log.d(TAG, "Set active profile to: " + newActiveProfile.getUuid().toString()
                + " - " + newActiveProfile.getName());

        Profile lastProfile = mActiveProfile;
        mActiveProfile = newActiveProfile;
        mDirty = true;

        if (doInit) {
            if (LOCAL_LOGV) Log.v(TAG, "setActiveProfile(Profile, boolean) - Running init");
            // Call profile's "doSelect"
            mActiveProfile.doSelect(mContext, mKeyguardService);

            // Notify other applications of newly selected profile.
            Intent broadcast = new Intent(ProfileManager.INTENT_ACTION_PROFILE_SELECTED);
            broadcast.putExtra(ProfileManager.EXTRA_PROFILE_NAME,
                    mActiveProfile.getName());
            broadcast.putExtra(ProfileManager.EXTRA_PROFILE_UUID,
                    mActiveProfile.getUuid().toString());
            broadcast.putExtra(ProfileManager.EXTRA_LAST_PROFILE_NAME,
                    lastProfile.getName());
            broadcast.putExtra(ProfileManager.EXTRA_LAST_PROFILE_UUID,
                    lastProfile.getUuid().toString());

            mContext.sendBroadcastAsUser(broadcast, UserHandle.ALL);
            persistIfDirty();
        } else if (lastProfile != mActiveProfile && ActivityManagerNative.isSystemReady()) {
            // Something definitely changed: notify.
            Intent broadcast = new Intent(ProfileManager.INTENT_ACTION_PROFILE_UPDATED);
            broadcast.putExtra(ProfileManager.EXTRA_PROFILE_NAME,
                    mActiveProfile.getName());
            broadcast.putExtra(ProfileManager.EXTRA_PROFILE_UUID,
                    mActiveProfile.getUuid().toString());
            mContext.sendBroadcastAsUser(broadcast, UserHandle.ALL);
        }
    }

    private void addNotificationGroupInternal(NotificationGroup group) {
        if (mGroups.put(group.getUuid(), group) == null) {
            // If the above is true, then the ProfileGroup shouldn't exist in
            // the profile. Ensure it is added.
            for (Profile profile : mProfiles.values()) {
                ensureGroupInProfile(profile, group, false);
            }
        }
        mDirty = true;
    }
}
