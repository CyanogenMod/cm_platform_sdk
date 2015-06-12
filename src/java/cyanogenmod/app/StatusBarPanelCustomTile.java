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

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

/**
 * Class encapsulating a Custom Tile. Sent by the StatusBarManagerService to clients including
 * the status bar panel and any {@link cyanogenmod.app.CustomTileListenerService} clients.
 */
public class StatusBarPanelCustomTile implements Parcelable {

    private final String pkg;
    private final int id;
    private final String tag;
    private final String key;

    private final int uid;
    private final String resPkg;
    private final String opPkg;
    private final int initialPid;
    private final CustomTile customTile;
    private final UserHandle user;
    private final long postTime;

    public StatusBarPanelCustomTile(String pkg, String resPkg, String opPkg, int id, String tag,
                                 int uid, int initialPid, CustomTile customTile, UserHandle user) {
        this(pkg, resPkg, opPkg, id, tag, uid, initialPid, customTile, user,
                System.currentTimeMillis());
    }

    public StatusBarPanelCustomTile(String pkg, String resPkg, String opPkg, int id, String tag,
                                 int uid, int initialPid, CustomTile customTile, UserHandle user,
                                 long postTime) {
        if (pkg == null) throw new NullPointerException();
        if (customTile == null) throw new NullPointerException();

        this.pkg = pkg;
        this.resPkg = resPkg;
        this.opPkg = opPkg;
        this.id = id;
        this.tag = tag;
        this.uid = uid;
        this.initialPid = initialPid;
        this.customTile = customTile;
        this.user = user;
        this.postTime = postTime;
        this.key = key();
    }


    public StatusBarPanelCustomTile(Parcel in) {
        this.pkg = in.readString();
        this.resPkg = in.readString();
        this.opPkg = in.readString();
        this.id = in.readInt();
        if (in.readInt() != 0) {
            this.tag = in.readString();
        } else {
            this.tag = null;
        }
        this.uid = in.readInt();
        this.initialPid = in.readInt();
        this.customTile = new CustomTile(in);
        this.user = UserHandle.readFromParcel(in);
        this.postTime = in.readLong();
        this.key = key();
    }

    private String key() {
        return user.getIdentifier() + "|" + pkg + "|" + id + "|" + tag + "|" + uid;
    }

    public static final Creator<StatusBarPanelCustomTile> CREATOR
            = new Creator<StatusBarPanelCustomTile>()
    {
        public StatusBarPanelCustomTile createFromParcel(Parcel parcel)
        {
            return new StatusBarPanelCustomTile(parcel);
        }

        public StatusBarPanelCustomTile[] newArray(int size)
        {
            return new StatusBarPanelCustomTile[size];
        }
    };

    /** The {@link cyanogenmod.app.CustomTile} supplied to
     * {@link cyanogenmod.app.CMStatusBarManager#publishTile(int, cyanogenmod.app.CustomTile)}.
     */
    public CustomTile getCustomTile() {
        return customTile;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.pkg);
        out.writeString(this.resPkg);
        out.writeString(this.opPkg);
        out.writeInt(this.id);
        if (this.tag != null) {
            out.writeInt(1);
            out.writeString(this.tag);
        } else {
            out.writeInt(0);
        }
        out.writeInt(this.uid);
        out.writeInt(this.initialPid);
        this.customTile.writeToParcel(out, flags);
        user.writeToParcel(out, flags);
        out.writeLong(this.postTime);
    }

    @Override
    public StatusBarPanelCustomTile clone() {
        return new StatusBarPanelCustomTile(this.pkg, this.resPkg, this.opPkg,
                this.id, this.tag, this.uid, this.initialPid,
                this.customTile.clone(), this.user, this.postTime);
    }

    /**
     * Returns a userHandle for the instance of the app that posted this notification.
     */
    public int getUserId() {
        return this.user.getIdentifier();
    }

    /** The package of the app that posted the notification */
    public String getPackage() {
        return pkg;
    }

    /** The id supplied to CMStatusBarManager */
    public int getId() {
        return id;
    }

    /** The tag supplied to CMStatusBarManager or null if no tag was specified. */
    public String getTag() {
        return tag;
    }

    /**
     * A unique instance key for this notification record.
     */
    public String getKey() {
        return key;
    }

    /** The notifying app's calling uid. @hide */
    public int getUid() {
        return uid;
    }

    /** The package used for load resources from. @hide */
    public String getResPkg() {
        return resPkg;
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
     * The {@link android.os.UserHandle} for whom this CustomTile is intended.
     */
    public UserHandle getUser() {
        return user;
    }

    /** The time (in {@link System#currentTimeMillis} time) the CustomTile was published, */
    public long getPostTime() {
        return postTime;
    }
}
