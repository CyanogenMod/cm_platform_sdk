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

import cyanogenmod.os.Build;

import cyanogenmod.os.Concierge;
import cyanogenmod.os.Concierge.ParcelInfo;

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
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(in);
        int parcelableVersion = parcelInfo.getParcelVersion();

        // tmp variables for final
        String tmpResPkg = null;
        String tmpPkg = null;
        String tmpOpPkg = null;
        int tmpId = -1;
        String tmpTag = null;
        int tmpUid = -1;
        int tmpPid = -1;
        CustomTile tmpCustomTile = null;
        UserHandle tmpUser = null;
        long tmpPostTime = -1;

        // Pattern here is that all new members should be added to the end of
        // the writeToParcel method. Then we step through each version, until the latest
        // API release to help unravel this parcel
        if (parcelableVersion >= Build.CM_VERSION_CODES.APRICOT) {
            // default
            tmpPkg = in.readString();
            tmpOpPkg = in.readString();
            tmpId = in.readInt();
            if (in.readInt() != 0) {
                tmpTag = in.readString();
            } else {
                tmpTag = null;
            }
            tmpUid = in.readInt();
            tmpPid = in.readInt();
            tmpCustomTile = new CustomTile(in);
            tmpUser = UserHandle.readFromParcel(in);
            tmpPostTime = in.readLong();
        }

        if (parcelableVersion >= Build.CM_VERSION_CODES.BOYSENBERRY) {
            tmpResPkg = in.readString();
        }

        // Assign finals
        this.resPkg = tmpResPkg;
        this.pkg = tmpPkg;
        this.opPkg = tmpOpPkg;
        this.id = tmpId;
        this.tag = tmpTag;
        this.uid = tmpUid;
        this.initialPid = tmpPid;
        this.customTile = tmpCustomTile;
        this.user = tmpUser;
        this.postTime = tmpPostTime;
        this.key = key();

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    private String key() {
        return user.getIdentifier() + "|" + pkg + "|" + id + "|" + tag + "|" + uid;
    }

    /** @hide */
    public String persistableKey() {
        return user.getIdentifier() + "|" + pkg + "|" + tag;
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
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(out);

        // ==== APRICOT ===
        out.writeString(this.pkg);
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

        // ==== BOYSENBERRY =====
        out.writeString(this.resPkg);

        // Complete the parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public StatusBarPanelCustomTile clone() {
        return new StatusBarPanelCustomTile(this.pkg, this.resPkg, this.opPkg,
                this.id, this.tag, this.uid, this.initialPid,
                this.customTile.clone(), this.user, this.postTime);
    }

    /**
     * Returns a userHandle for the instance of the app that posted this tile.
     */
    public int getUserId() {
        return this.user.getIdentifier();
    }

    /** The package of the app that posted the tile */
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
     * A unique instance key for this tile record.
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
