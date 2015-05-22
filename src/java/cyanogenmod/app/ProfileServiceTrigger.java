/**
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

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

public class ProfileServiceTrigger implements Parcelable {
    private final String pkg;
    private final String key;

    private final int uid;
    private final String opPkg;
    private final int initialPid;
    private final Trigger trigger;
    private final UserHandle user;

    public ProfileServiceTrigger(String pkg, String opPkg, int uid,
            int initialPid, Trigger trigger, UserHandle user) {
        if (pkg == null) throw new NullPointerException();
        if (trigger == null) throw new NullPointerException();

        this.pkg = pkg;
        this.opPkg = opPkg;
        this.uid = uid;
        this.initialPid = initialPid;
        this.trigger = trigger;
        this.user = user;
        this.key = key();
    }

    public ProfileServiceTrigger(Parcel in) {
        this.pkg = in.readString();
        this.opPkg = in.readString();
        this.uid = in.readInt();
        this.initialPid = in.readInt();
        this.trigger = new Trigger(in);
        this.user = UserHandle.readFromParcel(in);
        this.key = key();
    }

    private String key() {
        return user.getIdentifier() + "|" + pkg  + "|" + trigger.getTriggerId() + "|" + uid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.pkg);
        out.writeString(this.opPkg);
        out.writeInt(this.uid);
        out.writeInt(this.initialPid);
        this.trigger.writeToParcel(out, flags);
        user.writeToParcel(out, flags);
    }

    @Override
    protected ProfileServiceTrigger clone() {
        return new ProfileServiceTrigger(this.pkg, this.opPkg,
                this.uid, this.initialPid, this.trigger.clone(), this.user);
    }

    public static final Parcelable.Creator<ProfileServiceTrigger> CREATOR
            = new Parcelable.Creator<ProfileServiceTrigger>() {
        @Override
        public ProfileServiceTrigger createFromParcel(Parcel in) {
            return new ProfileServiceTrigger(in);
        }

        @Override
        public ProfileServiceTrigger[] newArray(int size) {
            return new ProfileServiceTrigger[size];
        }
    };

    /**
     * Returns the trigger that was passed into the ProfileServiceTrigger
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * Returns a userHandle for the instance of the app that posted this trigger.
     */
    public int getUserId() {
        return this.user.getIdentifier();
    }

    /** The package of the app that posted the trigger */
    public String getPackage() {
        return pkg;
    }

    /**
     * A unique instance key for this profile service trigger record.
     */
    public String getKey() {
        return key;
    }

    /** The notifying app's calling uid. @hide */
    public int getUid() {
        return uid;
    }

    /** The package used for AppOps tracking. @hide */
    public String getOpPkg() {
        return opPkg;
    }

    /** @hide */
    public int getInitialPid() {
        return initialPid;
    }

    /**
     * The {@link android.os.UserHandle} for whom this Trigger is intended.
     */
    public UserHandle getUser() {
        return user;
    }
}
