/*
 * Copyright (C) 2016 The CyanogenMod Project
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
package org.cyanogenmod.internal.cmparts;

import android.os.Parcel;
import android.os.Parcelable;

import cyanogenmod.os.Concierge;

public class PartInfo implements Parcelable {

    private final String mName;

    private String mTitle;

    private String mSummary;

    private String mFragmentClass;

    private int mIconRes;

    private boolean mAvailable = true;

    public PartInfo(String name, String title, String summary) {
        mName = name;
        mTitle = title;
        mSummary = summary;
    }

    public PartInfo(String name) {
        this(name, null, null);
    }

    public PartInfo(Parcel parcel) {
        Concierge.ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
        int parcelableVersion = parcelInfo.getParcelVersion();

        mName = parcel.readString();
        mTitle = parcel.readString();
        mSummary = parcel.readString();
        mFragmentClass = parcel.readString();
        mIconRes = parcel.readInt();
        mAvailable = parcel.readInt() == 1;
    }

    public String getName() {
        return mName;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public String getSummary() {
        return mSummary;
    }

    public String getFragmentClass() { return mFragmentClass; }

    public void setFragmentClass(String fragmentClass) { mFragmentClass = fragmentClass; };

    public int getIconRes() { return mIconRes; }

    public void setIconRes(int iconRes) { mIconRes = iconRes; }

    public boolean isAvailable() { return mAvailable; }

    public void setAvailable(boolean available) { mAvailable = available; }

    public void updateFrom(PartInfo other) {
        if (other == null) {
            return;
        }
        if (other.getName().equals(getName())) {
            return;
        }
        setTitle(other.getTitle());
        setSummary(other.getSummary());
        setFragmentClass(other.getFragmentClass());
        setIconRes(other.getIconRes());
        setAvailable(other.isAvailable());
    }

    @Override
    public String toString() {
        return String.format("PartInfo=[ name=%s title=%s summary=%s fragment=%s ]",
                mName, mTitle, mSummary, mFragmentClass);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        Concierge.ParcelInfo parcelInfo = Concierge.prepareParcel(out);

        out.writeString(mName);
        out.writeString(mTitle);
        out.writeString(mSummary);
        out.writeString(mFragmentClass);
        out.writeInt(mIconRes);
        out.writeInt(mAvailable ? 1 : 0);

        parcelInfo.complete();
    }

    public static final Parcelable.Creator<PartInfo> CREATOR =
            new Parcelable.Creator<PartInfo>() {
                @Override
                public PartInfo createFromParcel(Parcel in) {
                    return new PartInfo(in);
                }

                @Override
                public PartInfo[] newArray(int size) {
                    return new PartInfo[size];
                }
            };
}
