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

import android.content.Context;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.policy.IKeyguardService;
import cyanogenmod.os.Build;
import cyanogenmod.profiles.AirplaneModeSettings;
import cyanogenmod.profiles.BrightnessSettings;
import cyanogenmod.profiles.ConnectionSettings;
import cyanogenmod.profiles.LockSettings;
import cyanogenmod.profiles.RingModeSettings;
import cyanogenmod.profiles.StreamSettings;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * A class that represents a device profile.
 *
 * A {@link Profile} can serve a multitude of purposes, allowing the creator(user)
 * to set overrides for streams, triggers, screen lock, brightness, various other
 * settings.
 */
public final class Profile implements Parcelable, Comparable {

    private String mName;

    private int mNameResId;

    private UUID mUuid;

    private ArrayList<UUID> mSecondaryUuids = new ArrayList<UUID>();

    private Map<UUID, ProfileGroup> profileGroups = new HashMap<UUID, ProfileGroup>();

    private ProfileGroup mDefaultGroup;

    private boolean mStatusBarIndicator = false;

    private boolean mDirty;

    private static final String TAG = "Profile";

    private int mProfileType;

    private Map<Integer, StreamSettings> streams = new HashMap<Integer, StreamSettings>();

    private Map<String, ProfileTrigger> mTriggers = new HashMap<String, ProfileTrigger>();

    private Map<Integer, ConnectionSettings> connections = new HashMap<Integer, ConnectionSettings>();

    private RingModeSettings mRingMode = new RingModeSettings();

    private AirplaneModeSettings mAirplaneMode = new AirplaneModeSettings();

    private BrightnessSettings mBrightness = new BrightnessSettings();

    private LockSettings mScreenLockMode = new LockSettings();

    private int mExpandedDesktopMode = ExpandedDesktopMode.DEFAULT;

    private int mDozeMode = DozeMode.DEFAULT;

    /**
     * Lock modes of a device
     */
    public static class LockMode {
        /** Represents a default state lock mode (user choice) */
        public static final int DEFAULT = 0;
        /** Represents an insecure state lock mode, where the device has no security screen */
        public static final int INSECURE = 1;
        /** Represents a disabled state lock mode, where the devices lock screen can be removed */
        public static final int DISABLE = 2;
    }

    /**
     * Expanded desktop modes available on a device
     */
    public static class ExpandedDesktopMode {
        /** Represents a default state expanded desktop mode (user choice) */
        public static final int DEFAULT = 0;
        /** Represents an enabled expanded desktop mode */
        public static final int ENABLE = 1;
        /** Represents a disabled expanded desktop mode */
        public static final int DISABLE = 2;
    }

    /**
     * Doze modes available on a device
     */
    public static class DozeMode {
        /** Represents a default Doze mode (user choice) */
        public static final int DEFAULT = 0;
        /** Represents an enabled Doze mode */
        public static final int ENABLE = 1;
        /** Represents an disabled Doze mode */
        public static final int DISABLE = 2;
    }

    /**
     * Available trigger types on the device, usually hardware
     */
    public static class TriggerType {
        /** Represents a WiFi trigger type */
        public static final int WIFI = 0;
        /** Represents a Bluetooth trigger type */
        public static final int BLUETOOTH = 1;
    }

    /**
     * Various trigger states associated with a {@link TriggerType}
     */
    public static class TriggerState {
        /** A {@link TriggerState) for when the {@link TriggerType} connects */
        public static final int ON_CONNECT = 0;
        /** A {@link TriggerState) for when the {@link TriggerType} disconnects */
        public static final int ON_DISCONNECT = 1;
        /** A {@link TriggerState) for when the {@link TriggerType} is disabled */
        public static final int DISABLED = 2;
        /**
         * A {@link TriggerState) for when the {@link TriggerType#BLUETOOTH}
         * connects for A2DP session
         */
        public static final int ON_A2DP_CONNECT = 3;
        /**
         * A {@link TriggerState) for when the {@link TriggerType#BLUETOOTH}
         * disconnects from A2DP session
         */
        public static final int ON_A2DP_DISCONNECT = 4;
    }

    /**
     * A {@link Profile} type
     */
    public static class Type {
        /** Profile type which represents a toggle {@link Profile} */
        public static final int TOGGLE = 0;
        /** Profile type which represents a conditional {@link Profile} */
        public static final int CONDITIONAL = 1;
    }

    /**
     * A {@link ProfileTrigger} is a {@link TriggerType} which can be queried from the OS
     */
    public static class ProfileTrigger implements Parcelable {
        private int mType;
        private String mId;
        private String mName;
        private int mState;


        /**
         * Construct a {@link ProfileTrigger} based on its type {@link Profile.TriggerType} and if
         * the trigger should fire on a {@link Profile.TriggerState} change.
         *
         * Example:
         * <pre class="prettyprint">
         *   triggerId = trigger.getSSID();                  // Use the AP's SSID as identifier
         *   triggerName = trigger.getTitle();               // Use the AP's name as the trigger name
         *   triggerType = Profile.TriggerType.WIFI;         // This is a wifi trigger
         *   triggerState = Profile.TriggerState.ON_CONNECT; // On Connect of this, trigger
         *
         *   Profile.ProfileTrigger profileTrigger =
         *           new Profile.ProfileTrigger(triggerType, triggerId, triggerState, triggerName);
         * </pre>
         *
         * @param type a {@link Profile.TriggerType}
         * @param id an identifier for the ProfileTrigger
         * @param state {@link Profile.TriggerState} depending on the TriggerType
         * @param name an identifying name for the ProfileTrigger
         */
        public ProfileTrigger(int type, String id, int state, String name) {
            mType = type;
            mId = id;
            mState = state;
            mName = name;
        }

        private ProfileTrigger(Parcel in) {
            // Read parcelable version, make sure to define explicit changes
            // within {@link Build.PARCELABLE_VERSION);
            int parcelableVersion = in.readInt();
            int parcelableSize = in.readInt();
            int startPosition = in.dataPosition();

            // Pattern here is that all new members should be added to the end of
            // the writeToParcel method. Then we step through each version, until the latest
            // API release to help unravel this parcel
            if (parcelableVersion >= Build.CM_VERSION_CODES.BOYSENBERRY) {
                mType = in.readInt();
                mId = in.readString();
                mState = in.readInt();
                mName = in.readString();
            }

            in.setDataPosition(startPosition + parcelableSize);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            // Write parcelable version, make sure to define explicit changes
            // within {@link Build.PARCELABLE_VERSION);
            dest.writeInt(Build.PARCELABLE_VERSION);

            // Inject a placeholder that will store the parcel size from this point on
            // (not including the size itself).
            int sizePosition = dest.dataPosition();
            dest.writeInt(0);
            int startPosition = dest.dataPosition();

            dest.writeInt(mType);
            dest.writeString(mId);
            dest.writeInt(mState);
            dest.writeString(mName);

            // Go back and write size
            int parcelableSize = dest.dataPosition() - startPosition;
            dest.setDataPosition(sizePosition);
            dest.writeInt(parcelableSize);
            dest.setDataPosition(startPosition + parcelableSize);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * Get the {@link ProfileTrigger} {@link TriggerType}
         * @return {@link TriggerType}
         */
        public int getType() {
            return mType;
        }

        /**
         * Get the name associated with the {@link ProfileTrigger}
         * @return a string name
         */
        public String getName() {
            return mName;
        }

        /**
         * Get the id associated with the {@link ProfileTrigger}
         * @return an string identifier
         */
        public String getId() {
            return mId;
        }

        /**
         * Get the state associated with the {@link ProfileTrigger}
         * @return an integer indicating the state
         */
        public int getState() {
            return mState;
        }

        /**
         * @hide
         */
        public void getXmlString(StringBuilder builder, Context context) {
            final String itemType = mType == TriggerType.WIFI ? "wifiAP" : "btDevice";

            builder.append("<");
            builder.append(itemType);
            builder.append(" ");
            builder.append(getIdType(mType));
            builder.append("=\"");
            builder.append(mId);
            builder.append("\" state=\"");
            builder.append(mState);
            builder.append("\" name=\"");
            builder.append(mName);
            builder.append("\"></");
            builder.append(itemType);
            builder.append(">\n");
        }

        /**
         * @hide
         */
        public static ProfileTrigger fromXml(XmlPullParser xpp, Context context) {
            final String name = xpp.getName();
            final int type;

            if (name.equals("wifiAP")) {
                type = TriggerType.WIFI;
            } else if (name.equals("btDevice")) {
                type = TriggerType.BLUETOOTH;
            } else {
                return null;
            }

            String id = xpp.getAttributeValue(null, getIdType(type));
            int state = Integer.valueOf(xpp.getAttributeValue(null, "state"));
            String triggerName =  xpp.getAttributeValue(null, "name");
            if (triggerName == null) {
                triggerName = id;
            }

            return new ProfileTrigger(type, id, state, triggerName);
        }

        private static String getIdType(int type) {
            return type == TriggerType.WIFI ? "ssid" : "address";
        }

        /**
         * @hide
         */
        public static final Parcelable.Creator<ProfileTrigger> CREATOR
                = new Parcelable.Creator<ProfileTrigger>() {
            public ProfileTrigger createFromParcel(Parcel in) {
                return new ProfileTrigger(in);
            }

            @Override
            public ProfileTrigger[] newArray(int size) {
                return new ProfileTrigger[size];
            }
        };
    }

    /** @hide */
    public static final Parcelable.Creator<Profile> CREATOR = new Parcelable.Creator<Profile>() {
        public Profile createFromParcel(Parcel in) {
            return new Profile(in);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };

    public Profile(String name) {
        this(name, -1, UUID.randomUUID());
    }

    /** @hide */
    public Profile(String name, int nameResId, UUID uuid) {
        mName = name;
        mNameResId = nameResId;
        mUuid = uuid;
        mProfileType = Type.TOGGLE;  //Default to toggle type
        mDirty = false;
    }

    private Profile(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Get the {@link TriggerState} for a {@link ProfileTrigger} with a given id
     * @param type {@link TriggerType}
     * @param id string id of {@link ProfileTrigger}
     * @return {@link TriggerState}
     */
    public int getTriggerState(int type, String id) {
        ProfileTrigger trigger = id != null ? mTriggers.get(id) : null;
        if (trigger != null) {
            return trigger.mState;
        }
        return TriggerState.DISABLED;
    }

    /**
     * Get all the {@link ProfileTrigger}s for a given {@link TriggerType}
     * @param type {@link TriggerType}
     * @return an array list of {@link ProfileTrigger}s
     */
    public ArrayList<ProfileTrigger> getTriggersFromType(int type) {
        ArrayList<ProfileTrigger> result = new ArrayList<ProfileTrigger>();
        for (Entry<String, ProfileTrigger> profileTrigger:  mTriggers.entrySet()) {
            ProfileTrigger trigger = profileTrigger.getValue();
            if (trigger.getType() == type) {
                result.add(trigger);
            }
        }
        return result;
    }

    /**
     * Set a custom {@link ProfileTrigger}
     * @hide
     */
    public void setTrigger(int type, String id, int state, String name) {
        if (id == null
                || type < TriggerType.WIFI || type > TriggerType.BLUETOOTH
                || state < TriggerState.ON_CONNECT || state > TriggerState.ON_A2DP_DISCONNECT) {
            return;
        }

        ProfileTrigger trigger = mTriggers.get(id);

        if (state == TriggerState.DISABLED) {
            if (trigger != null) {
                mTriggers.remove(id);
            }
        } else if (trigger != null) {
            trigger.mState = state;
        } else {
            mTriggers.put(id, new ProfileTrigger(type, id, state, name));
        }

        mDirty = true;
    }

    /**
     * Set a {@link ProfileTrigger} on the {@link Profile}
     * @param trigger a {@link ProfileTrigger}
     */
    public void setTrigger(ProfileTrigger trigger) {
        setTrigger(trigger.getType(), trigger.getId(), trigger.getState(), trigger.getName());
    }

    public int compareTo(Object obj) {
        Profile tmp = (Profile) obj;
        if (mName.compareTo(tmp.mName) < 0) {
            return -1;
        } else if (mName.compareTo(tmp.mName) > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Add a {@link ProfileGroup} to the {@link Profile}
     * @param profileGroup
     * @hide
     */
    public void addProfileGroup(ProfileGroup profileGroup) {
        if (profileGroup == null) {
            return;
        }

        if (profileGroup.isDefaultGroup()) {
            /* we must not have more than one default group */
            if (mDefaultGroup != null) {
                return;
            }
            mDefaultGroup = profileGroup;
        }
        profileGroups.put(profileGroup.getUuid(), profileGroup);
        mDirty = true;
    }

    /**
     * Remove a {@link ProfileGroup} with a given {@link UUID}
     * @param uuid
     * @hide
     */
    public void removeProfileGroup(UUID uuid) {
        if (!profileGroups.get(uuid).isDefaultGroup()) {
            profileGroups.remove(uuid);
        } else {
            Log.e(TAG, "Cannot remove default group: " + uuid);
        }
    }

    /**
     * Get {@link ProfileGroup}s associated with the {@link Profile}
     * @return {@link ProfileGroup[]}
     * @hide
     */
    public ProfileGroup[] getProfileGroups() {
        return profileGroups.values().toArray(new ProfileGroup[profileGroups.size()]);
    }

    /**
     * Get a {@link ProfileGroup} with a given {@link UUID}
     * @param uuid
     * @return a {@link ProfileGroup}
     * @hide
     */
    public ProfileGroup getProfileGroup(UUID uuid) {
        return profileGroups.get(uuid);
    }

    /**
     * Get the default {@link ProfileGroup} associated with the {@link Profile}
     * @return the default {@link ProfileGroup}
     * @hide
     */
    public ProfileGroup getDefaultGroup() {
        return mDefaultGroup;
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        dest.writeInt(Build.PARCELABLE_VERSION);

        // Inject a placeholder that will store the parcel size from this point on
        // (not including the size itself).
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();

        // === BOYSENBERRY ===
        if (!TextUtils.isEmpty(mName)) {
            dest.writeInt(1);
            dest.writeString(mName);
        } else {
            dest.writeInt(0);
        }
        if (mNameResId != 0) {
            dest.writeInt(1);
            dest.writeInt(mNameResId);
        } else {
            dest.writeInt(0);
        }
        if (mUuid != null) {
            dest.writeInt(1);
            new ParcelUuid(mUuid).writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (mSecondaryUuids != null && !mSecondaryUuids.isEmpty()) {
            ArrayList<ParcelUuid> uuids = new ArrayList<ParcelUuid>(mSecondaryUuids.size());
            for (UUID u : mSecondaryUuids) {
                uuids.add(new ParcelUuid(u));
            }
            dest.writeInt(1);
            dest.writeParcelableArray(uuids.toArray(new Parcelable[uuids.size()]), flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mStatusBarIndicator ? 1 : 0);
        dest.writeInt(mProfileType);
        dest.writeInt(mDirty ? 1 : 0);
        if (profileGroups != null && !profileGroups.isEmpty()) {
            dest.writeInt(1);
            dest.writeTypedArray(profileGroups.values().toArray(
                    new ProfileGroup[0]), flags);
        } else {
            dest.writeInt(0);
        }
        if (streams != null && !streams.isEmpty()) {
            dest.writeInt(1);
            dest.writeTypedArray(streams.values().toArray(
                    new StreamSettings[0]), flags);
        } else {
            dest.writeInt(0);
        }
        if (connections != null && !connections.isEmpty()) {
            dest.writeInt(1);
            dest.writeTypedArray(connections.values().toArray(
                    new ConnectionSettings[0]), flags);
        } else {
            dest.writeInt(0);
        }
        if (mRingMode != null) {
            dest.writeInt(1);
            mRingMode.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (mAirplaneMode != null) {
            dest.writeInt(1);
            mAirplaneMode.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (mBrightness != null) {
            dest.writeInt(1);
            mBrightness.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (mScreenLockMode != null) {
            dest.writeInt(1);
            mScreenLockMode.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        dest.writeTypedArray(mTriggers.values().toArray(new ProfileTrigger[0]), flags);
        dest.writeInt(mExpandedDesktopMode);
        dest.writeInt(mDozeMode);

        // Go back and write size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    /** @hide */
    public void readFromParcel(Parcel in) {
        // Read parcelable version, make sure to define explicit changes
        // within {@link Build.PARCELABLE_VERSION);
        int parcelableVersion = in.readInt();
        int parcelableSize = in.readInt();
        int startPosition = in.dataPosition();

        // Pattern here is that all new members should be added to the end of
        // the writeToParcel method. Then we step through each version, until the latest
        // API release to help unravel this parcel
        if (parcelableVersion >= Build.CM_VERSION_CODES.BOYSENBERRY) {
            if (in.readInt() != 0) {
                mName = in.readString();
            }
            if (in.readInt() != 0) {
                mNameResId = in.readInt();
            }
            if (in.readInt() != 0) {
                mUuid = ParcelUuid.CREATOR.createFromParcel(in).getUuid();
            }
            if (in.readInt() != 0) {
                for (Parcelable parcel : in.readParcelableArray(null)) {
                    ParcelUuid u = (ParcelUuid) parcel;
                    mSecondaryUuids.add(u.getUuid());
                }
            }
            mStatusBarIndicator = (in.readInt() == 1);
            mProfileType = in.readInt();
            mDirty = (in.readInt() == 1);
            if (in.readInt() != 0) {
                for (ProfileGroup group : in.createTypedArray(ProfileGroup.CREATOR)) {
                    profileGroups.put(group.getUuid(), group);
                    if (group.isDefaultGroup()) {
                        mDefaultGroup = group;
                    }
                }
            }
            if (in.readInt() != 0) {
                for (StreamSettings stream : in.createTypedArray(StreamSettings.CREATOR)) {
                    streams.put(stream.getStreamId(), stream);
                }
            }
            if (in.readInt() != 0) {
                for (ConnectionSettings connection :
                        in.createTypedArray(ConnectionSettings.CREATOR)) {
                    connections.put(connection.getConnectionId(), connection);
                }
            }
            if (in.readInt() != 0) {
                mRingMode = RingModeSettings.CREATOR.createFromParcel(in);
            }
            if (in.readInt() != 0) {
                mAirplaneMode = AirplaneModeSettings.CREATOR.createFromParcel(in);
            }
            if (in.readInt() != 0) {
                mBrightness = BrightnessSettings.CREATOR.createFromParcel(in);
            }
            if (in.readInt() != 0) {
                mScreenLockMode = LockSettings.CREATOR.createFromParcel(in);
            }
            for (ProfileTrigger trigger : in.createTypedArray(ProfileTrigger.CREATOR)) {
                mTriggers.put(trigger.mId, trigger);
            }
            mExpandedDesktopMode = in.readInt();
            mDozeMode = in.readInt();
        }

        in.setDataPosition(startPosition + parcelableSize);
    }

    /**
     * Get the name associated with the {@link Profile}
     * @return a string name of the profile
     */
    public String getName() {
        return mName;
    }

    /**
     * Set a name for the {@link Profile}
     * @param name a string for the {@link Profile}
     */
    public void setName(String name) {
        mName = name;
        mNameResId = -1;
        mDirty = true;
    }

    /**
     * Get the {@link Type} of the {@link Profile}
     * @return
     */
    public int getProfileType() {
        return mProfileType;
    }

    /**
     * Set the {@link Type} for the {@link Profile}
     * @param type a type of profile
     */
    public void setProfileType(int type) {
        mProfileType = type;
        mDirty = true;
    }

    /**
     * Get the {@link UUID} associated with the {@link Profile}
     * @return the uuid for the profile
     */
    public UUID getUuid() {
        if (this.mUuid == null) this.mUuid = UUID.randomUUID();
        return this.mUuid;
    }

    /**
     * Get the secondary {@link UUID}s for the {@link Profile}
     * @return the secondary uuids for the Profile
     */
    public UUID[] getSecondaryUuids() {
        return mSecondaryUuids.toArray(new UUID[mSecondaryUuids.size()]);
    }

    /**
     * Set a list of secondary {@link UUID}s for the {@link Profile}
     * @param uuids
     */
    public void setSecondaryUuids(List<UUID> uuids) {
        mSecondaryUuids.clear();
        if (uuids != null) {
            mSecondaryUuids.addAll(uuids);
            mDirty = true;
        }
    }

    /**
     * Add a secondary {@link UUID} to the {@link Profile}
     * @param uuid
     */
    public void addSecondaryUuid(UUID uuid) {
        if (uuid != null) {
            mSecondaryUuids.add(uuid);
            mDirty = true;
        }
    }

    /**
     * @hide
     */
    public boolean getStatusBarIndicator() {
        return mStatusBarIndicator;
    }

    /**
     * @hide
     */
    public void setStatusBarIndicator(boolean newStatusBarIndicator) {
        mStatusBarIndicator = newStatusBarIndicator;
        mDirty = true;
    }

    /**
     * Check if the given {@link Profile} is a {@link Type#CONDITIONAL}
     * @return true if conditional
     */
    public boolean isConditionalType() {
        return(mProfileType == Type.CONDITIONAL ? true : false);
    }

    /**
     * @hide
     */
    public void setConditionalType() {
        mProfileType = Type.CONDITIONAL;
        mDirty = true;
    }

    /**
     * Get the {@link RingModeSettings} for the {@link Profile}
     * @return
     */
    public RingModeSettings getRingMode() {
        return mRingMode;
    }

    /**
     * Set the {@link RingModeSettings} for the {@link Profile}
     * @param descriptor
     */
    public void setRingMode(RingModeSettings descriptor) {
        mRingMode = descriptor;
        mDirty = true;
    }

    /**
     * Get the {@link LockSettings} for the {@link Profile}
     * @return
     */
    public LockSettings getScreenLockMode() {
        return mScreenLockMode;
    }

    /**
     * Set the {@link LockSettings} for the {@link Profile}
     * @param screenLockMode
     */
    public void setScreenLockMode(LockSettings screenLockMode) {
        mScreenLockMode = screenLockMode;
        mDirty = true;
    }

    /**
     * Get the {@link ExpandedDesktopMode} for the {@link Profile}
     * @return
     */
    public int getExpandedDesktopMode() {
        return mExpandedDesktopMode;
    }

    /**
     * Set the {@link ExpandedDesktopMode} for the {@link Profile}
     * @return
     */
    public void setExpandedDesktopMode(int expandedDesktopMode) {
        if (expandedDesktopMode < ExpandedDesktopMode.DEFAULT
                || expandedDesktopMode > ExpandedDesktopMode.DISABLE) {
            mExpandedDesktopMode = ExpandedDesktopMode.DEFAULT;
        } else {
            mExpandedDesktopMode = expandedDesktopMode;
        }
        mDirty = true;
    }

    /**
     * Get the {@link DozeMode} associated with the {@link Profile}
     * @return
     */
    public int getDozeMode() {
        return mDozeMode;
    }

    /**
     * Set the {@link DozeMode} associated with the {@link Profile}
     * @return
     */
    public void setDozeMode(int dozeMode) {
        if (dozeMode < DozeMode.DEFAULT
                || dozeMode > DozeMode.DISABLE) {
            mDozeMode = DozeMode.DEFAULT;
        } else {
            mDozeMode = dozeMode;
        }
        mDirty = true;
    }

    /**
     * Get the {@link AirplaneModeSettings} associated with the {@link Profile}
     * @return
     */
    public AirplaneModeSettings getAirplaneMode() {
        return mAirplaneMode;
    }

    /**
     * Set the {@link AirplaneModeSettings} associated with the {@link Profile}
     * @param descriptor
     */
    public void setAirplaneMode(AirplaneModeSettings descriptor) {
        mAirplaneMode = descriptor;
        mDirty = true;
    }

    /**
     * Get the {@link BrightnessSettings} associated with the {@link Profile}
     * @return
     */
    public BrightnessSettings getBrightness() {
        return mBrightness;
    }

    /**
     * Set the {@link BrightnessSettings} associated with the {@link Profile}
     * @return
     */
    public void setBrightness(BrightnessSettings descriptor) {
        mBrightness = descriptor;
        mDirty = true;
    }

    /** @hide */
    public boolean isDirty() {
        if (mDirty) {
            return true;
        }
        for (ProfileGroup group : profileGroups.values()) {
            if (group.isDirty()) {
                return true;
            }
        }
        for (StreamSettings stream : streams.values()) {
            if (stream.isDirty()) {
                return true;
            }
        }
        for (ConnectionSettings conn : connections.values()) {
            if (conn.isDirty()) {
                return true;
            }
        }
        if (mRingMode.isDirty()) {
            return true;
        }
        if (mAirplaneMode.isDirty()) {
            return true;
        }
        if (mBrightness.isDirty()) {
            return true;
        }
        return false;
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append("<profile ");
        if (mNameResId > 0) {
            builder.append("nameres=\"");
            builder.append(context.getResources().getResourceEntryName(mNameResId));
        } else {
            builder.append("name=\"");
            builder.append(TextUtils.htmlEncode(getName()));
        }
        builder.append("\" uuid=\"");
        builder.append(TextUtils.htmlEncode(getUuid().toString()));
        builder.append("\">\n");

        builder.append("<uuids>");
        for (UUID u : mSecondaryUuids) {
            builder.append("<uuid>");
            builder.append(TextUtils.htmlEncode(u.toString()));
            builder.append("</uuid>");
        }
        builder.append("</uuids>\n");

        builder.append("<profiletype>");
        builder.append(getProfileType() == Type.TOGGLE ? "toggle" : "conditional");
        builder.append("</profiletype>\n");

        builder.append("<statusbar>");
        builder.append(getStatusBarIndicator() ? "yes" : "no");
        builder.append("</statusbar>\n");

        if (mScreenLockMode != null) {
            builder.append("<screen-lock-mode>");
            mScreenLockMode.writeXmlString(builder, context);
            builder.append("</screen-lock-mode>\n");
        }

        builder.append("<expanded-desktop-mode>");
        builder.append(mExpandedDesktopMode);
        builder.append("</expanded-desktop-mode>\n");

        builder.append("<doze-mode>");
        builder.append(mDozeMode);
        builder.append("</doze-mode>\n");

        mAirplaneMode.getXmlString(builder, context);

        mBrightness.getXmlString(builder, context);

        mRingMode.getXmlString(builder, context);

        for (ProfileGroup pGroup : profileGroups.values()) {
            pGroup.getXmlString(builder, context);
        }
        for (StreamSettings sd : streams.values()) {
            sd.getXmlString(builder, context);
        }
        for (ConnectionSettings cs : connections.values()) {
            cs.getXmlString(builder, context);
        }
        if (!mTriggers.isEmpty()) {
            builder.append("<triggers>\n");
            for (ProfileTrigger trigger : mTriggers.values()) {
                trigger.getXmlString(builder, context);
            }
            builder.append("</triggers>\n");
        }

        builder.append("</profile>\n");
        mDirty = false;
    }

    private static List<UUID> readSecondaryUuidsFromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException,
            IOException {
        ArrayList<UUID> uuids = new ArrayList<UUID>();
        int event = xpp.next();
        while (event != XmlPullParser.END_TAG || !xpp.getName().equals("uuids")) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("uuid")) {
                    try {
                        uuids.add(UUID.fromString(xpp.nextText()));
                    } catch (NullPointerException e) {
                        Log.w(TAG, "Null Pointer - invalid UUID");
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "UUID not recognized");
                    }
                }
            }
            event = xpp.next();
        }
        return uuids;
    }

    private static void readTriggersFromXml(XmlPullParser xpp, Context context, Profile profile)
            throws XmlPullParserException, IOException {
        int event = xpp.next();
        while (event != XmlPullParser.END_TAG || !xpp.getName().equals("triggers")) {
            if (event == XmlPullParser.START_TAG) {
                ProfileTrigger trigger = ProfileTrigger.fromXml(xpp, context);
                if (trigger != null) {
                    profile.mTriggers.put(trigger.mId, trigger);
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while parsing triggers");
            }
            event = xpp.next();
        }
    }

    /** @hide */
    public void validateRingtones(Context context) {
        for (ProfileGroup pg : profileGroups.values()) {
            pg.validateOverrideUris(context);
        }
    }

    /** @hide */
    public static Profile fromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException, IOException {
        String value = xpp.getAttributeValue(null, "nameres");
        int profileNameResId = -1;
        String profileName = null;

        if (value != null) {
            profileNameResId = context.getResources().getIdentifier(value, "string",
                    "cyanogenmod.platform");
            if (profileNameResId > 0) {
                profileName = context.getResources().getString(profileNameResId);
            }
        }

        if (profileName == null) {
            profileName = xpp.getAttributeValue(null, "name");
        }

        UUID profileUuid = UUID.randomUUID();
        try {
            profileUuid = UUID.fromString(xpp.getAttributeValue(null, "uuid"));
        } catch (NullPointerException e) {
            Log.w(TAG,
                    "Null Pointer - UUID not found for "
                    + profileName
                    + ".  New UUID generated: "
                    + profileUuid.toString()
                    );
        } catch (IllegalArgumentException e) {
            Log.w(TAG,
                    "UUID not recognized for "
                    + profileName
                    + ".  New UUID generated: "
                    + profileUuid.toString()
                    );
        }

        Profile profile = new Profile(profileName, profileNameResId, profileUuid);
        int event = xpp.next();
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("uuids")) {
                    profile.setSecondaryUuids(readSecondaryUuidsFromXml(xpp, context));
                }
                if (name.equals("statusbar")) {
                    profile.setStatusBarIndicator(xpp.nextText().equals("yes"));
                }
                if (name.equals("profiletype")) {
                    profile.setProfileType(xpp.nextText().equals("toggle")
                            ? Type.TOGGLE : Type.CONDITIONAL);
                }
                if (name.equals("ringModeDescriptor")) {
                    RingModeSettings smd = RingModeSettings.fromXml(xpp, context);
                    profile.setRingMode(smd);
                }
                if (name.equals("airplaneModeDescriptor")) {
                    AirplaneModeSettings amd = AirplaneModeSettings.fromXml(xpp, context);
                    profile.setAirplaneMode(amd);
                }
                if (name.equals("brightnessDescriptor")) {
                    BrightnessSettings bd = BrightnessSettings.fromXml(xpp, context);
                    profile.setBrightness(bd);
                }
                if (name.equals("screen-lock-mode")) {
                    LockSettings lockMode = new LockSettings(Integer.valueOf(xpp.nextText()));
                    profile.setScreenLockMode(lockMode);
                }
                if (name.equals("expanded-desktop-mode")) {
                    profile.setExpandedDesktopMode(Integer.valueOf(xpp.nextText()));
                }
                if (name.equals("doze-mode")) {
                    profile.setDozeMode(Integer.valueOf(xpp.nextText()));
                }
                if (name.equals("profileGroup")) {
                    ProfileGroup pg = ProfileGroup.fromXml(xpp, context);
                    profile.addProfileGroup(pg);
                }
                if (name.equals("streamDescriptor")) {
                    StreamSettings sd = StreamSettings.fromXml(xpp, context);
                    profile.setStreamSettings(sd);
                }
                if (name.equals("connectionDescriptor")) {
                    ConnectionSettings cs = ConnectionSettings.fromXml(xpp, context);
                    profile.connections.put(cs.getConnectionId(), cs);
                }
                if (name.equals("triggers")) {
                    readTriggersFromXml(xpp, context, profile);
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while parsing profle:" + profileName);
            }
            event = xpp.next();
        }

        /* we just loaded from XML, so nothing needs saving */
        profile.mDirty = false;

        return profile;
    }

    /** @hide */
    public void doSelect(Context context, IKeyguardService keyguardService) {
        // Set stream volumes
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        for (StreamSettings sd : streams.values()) {
            if (sd.isOverride()) {
                am.setStreamVolume(sd.getStreamId(), sd.getValue(), 0);
            }
        }
        // Set connections
        for (ConnectionSettings cs : connections.values()) {
            if (cs.isOverride()) {
                cs.processOverride(context);
            }
        }
        // Set ring mode
        mRingMode.processOverride(context);
        // Set airplane mode
        mAirplaneMode.processOverride(context);

        // Set brightness
        mBrightness.processOverride(context);

        if (keyguardService != null) {
            // Set lock screen mode
            mScreenLockMode.processOverride(context, keyguardService);
        } else {
            Log.e(TAG, "cannot process screen lock override without a keyguard service.");
        }

        // Set expanded desktop
        // if (mExpandedDesktopMode != ExpandedDesktopMode.DEFAULT) {
        //     Settings.System.putIntForUser(context.getContentResolver(),
        //             Settings.System.EXPANDED_DESKTOP_STATE,
        //             mExpandedDesktopMode == ExpandedDesktopMode.ENABLE ? 1 : 0,
        //             UserHandle.USER_CURRENT);
        // }

        // Set doze mode
        if (mDozeMode != DozeMode.DEFAULT) {
            Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.DOZE_ENABLED,
                    mDozeMode == DozeMode.ENABLE ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }
    }

    /**
     * Get the settings for a stream id in the {@link Profile}
     * @return {@link StreamSettings}
     */
    public StreamSettings getSettingsForStream(int streamId){
        return streams.get(streamId);
    }

    /**
     * Set the {@link StreamSettings} for the {@link Profile}
     * @param descriptor
     */
    public void setStreamSettings(StreamSettings descriptor){
        streams.put(descriptor.getStreamId(), descriptor);
        mDirty = true;
    }

    /**
     * Get the {@link StreamSettings} for the {@link Profile}
     * @return {@link Collection<StreamSettings>}
     */
    public Collection<StreamSettings> getStreamSettings(){
        return streams.values();
    }

    /**
     * Get the settings for a connection id in the {@link Profile}
     * @return {@link ConnectionSettings}
     */
    public ConnectionSettings getSettingsForConnection(int connectionId){
        return connections.get(connectionId);
    }

    /**
     * Set the {@link ConnectionSettings} for the {@link Profile}
     * @param descriptor
     */
    public void setConnectionSettings(ConnectionSettings descriptor){
        connections.put(descriptor.getConnectionId(), descriptor);
    }

    /**
     * Get the {@link ConnectionSettings} for the {@link Profile}
     * @return {@link Collection<ConnectionSettings>}
     */
    public Collection<ConnectionSettings> getConnectionSettings(){
        return connections.values();
    }
}
