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

/**
 * Class encapsulating an {@link Action}. Sent by the ProfilePluginService to the Settings
 * application's instance of Profiles.
 * @hide
 */
public class ProfileServiceAction implements Parcelable {
    private final String pkg;
    private final String key;
    private final int uid;
    private final String opPkg;
    private final int initialPid;
    private final Action action;
    private final UserHandle user;

    public ProfileServiceAction(String pkg, String opPkg, int uid,
            int initialPid, Action action, UserHandle user) {
        if (pkg == null) throw new NullPointerException();
        if (action == null) throw new NullPointerException();

        this.pkg = pkg;
        this.opPkg = opPkg;
        this.uid = uid;
        this.initialPid = initialPid;
        this.action = action;
        this.user = user;
        this.key = key();
    }

    public ProfileServiceAction(Parcel in) {
        this.pkg = in.readString();
        this.opPkg = in.readString();
        this.uid = in.readInt();
        this.initialPid = in.readInt();
        this.action = new Action(in);
        this.user = UserHandle.readFromParcel(in);
        this.key = key();
    }

    private String key() {
        return user.getIdentifier() + "|" + pkg  + "|" + action.getDescription() + "|" + uid;
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
        this.action.writeToParcel(out, flags);
        user.writeToParcel(out, flags);
    }

    @Override
    protected ProfileServiceAction clone() {
        return new ProfileServiceAction(this.pkg, this.opPkg,
                this.uid, this.initialPid, this.action.clone(), this.user);
    }

    /**
     * Returns the action that was passed into the ProfileServiceAction
     */
    public Action getAction() {
        return action;
    }

    /**
     * Returns a userHandle for the instance of the app that posted this action.
     */
    public int getUserId() {
        return this.user.getIdentifier();
    }

    /** The package of the app that posted the action */
    public String getPackage() {
        return pkg;
    }

    /**
     * A unique instance key for this profile service action record.
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
     * The {@link android.os.UserHandle} for whom this Action is intended.
     */
    public UserHandle getUser() {
        return user;
    }
}
