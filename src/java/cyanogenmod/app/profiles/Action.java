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

package cyanogenmod.app.profiles;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An action that can be invoked when a CyanogenMod profile is activated.
 */
public class Action implements Parcelable {
    private String mKey;
    private String mTitle;
    private String mDescription;
    private PendingIntent mAction;
    private String mPackageName;

    public Action() {}

    public Action(Parcel in) {
        mKey = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mAction = in.readParcelable(PendingIntent.class.getClassLoader());
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String mKey) {
        this.mKey = mKey;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public PendingIntent getAction() {
        return mAction;
    }

    public void setAction(PendingIntent mAction) {
        this.mAction = mAction;
    }

    /**
     * @hide
     * Overridden by service to ensure
     * package name is not spoofed 
     */
    public void setPackage(String packageName) {
        mPackageName = packageName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(mKey);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeParcelable(mAction, 0);
    }

    public static final Parcelable.Creator<Action> CREATOR
            = new Parcelable.Creator<Action>() {
        @Override
        public Action createFromParcel(Parcel in) {
            return new Action(in);
        }

        @Override
        public Action[] newArray(int size) {
            return new Action[size];
        }
    };
}