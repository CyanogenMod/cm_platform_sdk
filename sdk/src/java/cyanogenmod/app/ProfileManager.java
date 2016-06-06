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

package cyanogenmod.app;

import java.util.UUID;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.app.NotificationGroup;
import android.content.Context;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import cyanogenmod.app.IProfileManager;

import com.android.internal.R;
import cyanogenmod.providers.CMSettings;


/**
 * <p>
 * The ProfileManager allows you to create {@link Profile}s and {@link ProfileGroup}s to create
 * specific behavior states depending on triggers from hardware devices changing states, such as:
 *
 * <pre class="prettyprint">
 *     WiFi being enabled
 *     WiFi connecting to a certain AP
 *     Bluetooth connecting to a certain device
 *     Bluetooth disconnecting to a certain device
 *     NFC tag being scanned
 * </pre>
 *
 * <p>
 * Depending on these triggers, you can override connection settings, lockscreen modes, media
 * stream volumes and various other settings.
 *
 * <p>
 * To get the instance of this class, utilize ProfileManager#getInstance(Context context)
 *
 * <p>
 * This manager requires the MODIFY_PROFILES permission.
 *
 * @see cyanogenmod.app.Profile
 * @see cyanogenmod.app.ProfileGroup
 */
public class ProfileManager {

    private static IProfileManager sService;

    private Context mContext;

    private static final String TAG = "ProfileManager";

    /**
     * <p>Broadcast Action: A new profile has been selected. This can be triggered by the user
     * or by calls to the ProfileManagerService / Profile.</p>
     */
    public static final String INTENT_ACTION_PROFILE_SELECTED =
            "cyanogenmod.platform.intent.action.PROFILE_SELECTED";

    /**
     * <p>Broadcast Action: Current profile has been updated. This is triggered every time the
     * currently active profile is updated, instead of selected.</p>
     * <p> For instance, this includes profile updates caused by a locale change, which doesn't
     * trigger a profile selection, but causes its name to change.</p>
     */
    public static final String INTENT_ACTION_PROFILE_UPDATED =
            "cyanogenmod.platform.intent.action.PROFILE_UPDATED";


    /**
     * @hide
     */
    public static final String INTENT_ACTION_PROFILE_TRIGGER_STATE_CHANGED =
            "cyanogenmod.platform.intent.action.INTENT_ACTION_PROFILE_TRIGGER_STATE_CHANGED";

    /**
     * @hide
     */
    public static final String EXTRA_TRIGGER_ID = "trigger_id";

    /**
     * @hide
     */
    public static final String EXTRA_TRIGGER_TYPE = "trigger_type";


    /**
     * @hide
     */
    public static final String EXTRA_TRIGGER_STATE = "trigger_state";

    /**
     * Extra for {@link #INTENT_ACTION_PROFILE_SELECTED} and {@link #INTENT_ACTION_PROFILE_UPDATED}:
     * The name of the newly activated or updated profile
     */
    public static final String EXTRA_PROFILE_NAME = "name";

    /**
     * Extra for {@link #INTENT_ACTION_PROFILE_SELECTED} and {@link #INTENT_ACTION_PROFILE_UPDATED}:
     * The string representation of the UUID of the newly activated or updated profile
     */
    public static final String EXTRA_PROFILE_UUID = "uuid";

    /**
     * Extra for {@link #INTENT_ACTION_PROFILE_SELECTED}:
     * The name of the previously active profile
     */
    public static final String EXTRA_LAST_PROFILE_NAME = "lastName";

    /**
     * Extra for {@link #INTENT_ACTION_PROFILE_SELECTED}:
     * The string representation of the UUID of the previously active profile
     */
    public static final String EXTRA_LAST_PROFILE_UUID = "lastUuid";

    /**
     * Activity Action: Shows a profile picker.
     * <p>
     * Input: {@link #EXTRA_PROFILE_EXISTING_UUID}, {@link #EXTRA_PROFILE_SHOW_NONE},
     * {@link #EXTRA_PROFILE_TITLE}.
     * <p>
     * Output: {@link #EXTRA_PROFILE_PICKED_UUID}.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PROFILE_PICKER =
            "cyanogenmod.platform.intent.action.PROFILE_PICKER";

    /**
     * Constant for NO_PROFILE
     */
    public static final UUID NO_PROFILE =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * Given to the profile picker as a boolean. Whether to show an item for
     * deselect the profile. If the "None" item is picked,
     * {@link #EXTRA_PROFILE_PICKED_UUID} will be {@link #NO_PROFILE}.
     *
     * @see #ACTION_PROFILE_PICKER
     */
    public static final String EXTRA_PROFILE_SHOW_NONE =
            "cyanogenmod.platform.intent.extra.profile.SHOW_NONE";

    /**
     * Given to the profile picker as a {@link UUID} string representation. The {@link UUID}
     * representation of the current profile, which will be used to show a checkmark next to
     * the item for this {@link UUID}. If the item is {@link #NO_PROFILE} then "None" item
     * is selected if {@link #EXTRA_PROFILE_SHOW_NONE} is enabled. Otherwise, the current
     * profile is selected.
     *
     * @see #ACTION_PROFILE_PICKER
     */
    public static final String EXTRA_PROFILE_EXISTING_UUID =
            "cyanogenmod.platform.extra.profile.EXISTING_UUID";

    /**
     * Given to the profile picker as a {@link CharSequence}. The title to
     * show for the profile picker. This has a default value that is suitable
     * in most cases.
     *
     * @see #ACTION_PROFILE_PICKER
     */
    public static final String EXTRA_PROFILE_TITLE =
            "cyanogenmod.platform.intent.extra.profile.TITLE";

    /**
     * Returned from the profile picker as a {@link UUID} string representation.
     * <p>
     * It will be one of:
     * <li> the picked profile,
     * <li> null if the "None" item was picked.
     *
     * @see #ACTION_PROFILE_PICKER
     */
    public static final String EXTRA_PROFILE_PICKED_UUID =
            "cyanogenmod.platform.intent.extra.profile.PICKED_UUID";

    /**
     * Broadcast intent action indicating that Profiles has been enabled or disabled.
     * One extra provides this state as an int.
     *
     * @see #EXTRA_PROFILES_STATE
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String PROFILES_STATE_CHANGED_ACTION =
        "cyanogenmod.platform.app.profiles.PROFILES_STATE_CHANGED";

    /**
     * The lookup key for an int that indicates whether Profiles are enabled or
     * disabled. Retrieve it with {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #PROFILES_STATE_DISABLED
     * @see #PROFILES_STATE_ENABLED
     */
    public static final String EXTRA_PROFILES_STATE = "profile_state";

    /**
     * Set the resource id theme to use for the dialog picker activity.<br/>
     * The default theme is <code>com.android.internal.R.Theme_Holo_Dialog_Alert</code>.
     *
     * @see #ACTION_PROFILE_PICKER
     */
    public static final String EXTRA_PROFILE_DIALOG_THEME =
            "cyanogenmod.platform.intent.extra.profile.DIALOG_THEME";

    /**
     * Profiles are disabled.
     *
     * @see #PROFILES_STATE_CHANGED_ACTION
     */
    public static final int PROFILES_STATE_DISABLED = 0;

    /**
     * Profiles are enabled.
     *
     * @see #PROFILES_STATE_CHANGED_ACTION
     */
    public static final int PROFILES_STATE_ENABLED = 1;

    private static ProfileManager sProfileManagerInstance;
    private ProfileManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                cyanogenmod.app.CMContextConstants.Features.PROFILES) && sService == null) {
            Log.wtf(TAG, "Unable to get ProfileManagerService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.ProfileManager}
     * @param context
     * @return {@link ProfileManager}
     */
    public static ProfileManager getInstance(Context context) {
        if (sProfileManagerInstance == null) {
            sProfileManagerInstance = new ProfileManager(context);
        }
        return sProfileManagerInstance;
    }

    /** @hide */
    static public IProfileManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(CMContextConstants.CM_PROFILE_SERVICE);
        sService = IProfileManager.Stub.asInterface(b);
        return sService;
    }

    @Deprecated
    public void setActiveProfile(String profileName) {
        try {
            getService().setActiveProfileByName(profileName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Set the active {@link Profile} by {@link UUID}
     * @param profileUuid the {@link UUID} associated with the profile
     */
    public void setActiveProfile(UUID profileUuid) {
        try {
            getService().setActiveProfile(new ParcelUuid(profileUuid));
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get the active {@link Profile}
     * @return active {@link Profile}
     */
    public Profile getActiveProfile() {
        try {
            return getService().getActiveProfile();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Add a {@link Profile} that can be selected by the user
     * @param profile a {@link Profile} object
     */
    public void addProfile(Profile profile) {
        try {
            getService().addProfile(profile);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Remove a {@link Profile} from user selection
     * @param profile a {@link Profile} object
     */
    public void removeProfile(Profile profile) {
        try {
            getService().removeProfile(profile);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Update a {@link Profile} object
     * @param profile a {@link Profile} object
     */
    public void updateProfile(Profile profile) {
        try {
            getService().updateProfile(profile);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get the {@link Profile} object by its literal name
     * @param profileName name associated with the profile
     * @return profile a {@link Profile} object
     */
    @Deprecated
    public Profile getProfile(String profileName) {
        try {
            return getService().getProfileByName(profileName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Get a {@link Profile} via {@link UUID}
     * @param profileUuid {@link UUID} associated with the profile
     * @return {@link Profile}
     */
    public Profile getProfile(UUID profileUuid) {
        try {
            return getService().getProfile(new ParcelUuid(profileUuid));
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Get the profile names currently available to the user
     * @return {@link String[]} of profile names
     */
    public String[] getProfileNames() {
        try {
            Profile[] profiles = getService().getProfiles();
            String[] names = new String[profiles.length];
            for (int i = 0; i < profiles.length; i++) {
                names[i] = profiles[i].getName();
            }
            return names;
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Get the {@link Profile}s currently available to the user
     * @return {@link Profile[]}
     */
    public Profile[] getProfiles() {
        try {
            return getService().getProfiles();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Check if a {@link Profile} exists via its literal name
     * @param profileName a profile name
     * @return whether or not the profile exists
     */
    public boolean profileExists(String profileName) {
        try {
            return getService().profileExistsByName(profileName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            // To be on the safe side, we'll return "true", to prevent duplicate profiles
            // from being created.
            return true;
        }
    }

    /**
     * Check if a {@link Profile} exists via its {@link UUID}
     * @param profileUuid the profiles {@link UUID}
     * @return whether or not the profile exists
     */
    public boolean profileExists(UUID profileUuid) {
        try {
            return getService().profileExists(new ParcelUuid(profileUuid));
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            // To be on the safe side, we'll return "true", to prevent duplicate profiles
            // from being created.
            return true;
        }
    }

    /**
     * Check if a NotificationGroup exists
     * @param notificationGroupName the name of the notification group
     * @return whether or not the notification group exists
     * @hide
     */
    public boolean notificationGroupExists(String notificationGroupName) {
        try {
            return getService().notificationGroupExistsByName(notificationGroupName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            // To be on the safe side, we'll return "true", to prevent duplicate notification
            // groups from being created.
            return true;
        }
    }

    /**
     * Get the currently available NotificationGroups
     * @return NotificationGroup
     * @hide
     */
    public NotificationGroup[] getNotificationGroups() {
        try {
            return getService().getNotificationGroups();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Add a NotificationGroup to the available list
     * @param group NotificationGroup
     * @hide
     */
    public void addNotificationGroup(NotificationGroup group) {
        try {
            getService().addNotificationGroup(group);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Remove a NotificationGroup from the available list
     * @param group NotificationGroup
     * @hide
     */
    public void removeNotificationGroup(NotificationGroup group) {
        try {
            getService().removeNotificationGroup(group);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Update a NotificationGroup from the available list
     * @param group NotificationGroup
     * @hide
     */
    public void updateNotificationGroup(NotificationGroup group) {
        try {
            getService().updateNotificationGroup(group);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get a NotificationGroup for a specific package
     * @param pkg name of the package
     * @hide
     */
    public NotificationGroup getNotificationGroupForPackage(String pkg) {
        try {
            return getService().getNotificationGroupForPackage(pkg);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Get a NotificationGroup from the available list via {@link UUID}
     * @param uuid {@link UUID} of the notification group
     * @hide
     */
    public NotificationGroup getNotificationGroup(UUID uuid) {
        try {
            return getService().getNotificationGroup(new ParcelUuid(uuid));
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Get an active {@link ProfileGroup} via its package name
     * @param packageName the package name associated to the profile group
     * @return {@link ProfileGroup}
     * @hide
     */
    public ProfileGroup getActiveProfileGroup(String packageName) {
        NotificationGroup notificationGroup = getNotificationGroupForPackage(packageName);
        if (notificationGroup == null) {
            ProfileGroup defaultGroup = getActiveProfile().getDefaultGroup();
            return defaultGroup;
        }
        return getActiveProfile().getProfileGroup(notificationGroup.getUuid());
    }

    /**
     * Reset all profiles, groups, and notification groups to default state
     */
    public void resetAll() {
        try {
            getService().resetAll();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } catch (SecurityException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Check if profiles are currently activated in the system
     * @return whether profiles are enabled
     */
    public boolean isProfilesEnabled() {
        try {
            return getService().isEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }
}
