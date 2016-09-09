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

    @Override
    public String toString() {
        return String.format("PartInfo=[ name=%s title=%s summary=%s ]",
                mName, mTitle, mSummary);
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
