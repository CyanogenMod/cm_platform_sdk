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

package cyanogenmod.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * TODO: Comment me
 */
public class Trigger implements Parcelable {

    private String mPackage;
    private String mTriggerId;
    private String mTriggerDisplayName;
    private Bundle mStates = new Bundle();
    private String mCurrentState;

    /**
     *
     */
    public static final class State implements Parcelable {
        /**
         *
         */
        private String mKey;

        /**
         *
         */
        private String mDescription;

        /**
         *
         * @param key
         * @param description
         */
        public State(String key, String description) {
            mKey = key;
            mDescription = description;
        }

        /**
         *
         * @param in
         */
        public State(Parcel in) {
            mKey = in.readString();
            mDescription = in.readString();
        }

        /**
         *
         * @return
         */
        public String getKey() {
            return mKey;
        }

        /**
         *
         * @param mKey
         */
        public void setKey(String mKey) {
            this.mKey = mKey;
        }

        /**
         *
         * @return
         */
        public String getDescription() {
            return mDescription;
        }

        /**
         *
         * @param mDescription
         */
        public void setDescription(String mDescription) {
            this.mDescription = mDescription;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mKey);
            dest.writeString(mDescription);
        }

        public static final Parcelable.Creator<State> CREATOR
        = new Parcelable.Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                return new State(in);
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }

    /**
     *
     * @param triggerId
     * @param triggerDisplayname
     */
    public Trigger(String triggerId, String triggerDisplayname) {
        this.mTriggerId = triggerId;
        this.mTriggerDisplayName = triggerDisplayname;
    }

    /**
     *
     * @param state
     */
    public void addState(State state) {
        mStates.putParcelable(state.getKey(), state);
    }

    /**
     *
     * @return
     */
    public String getCurrentState() {
        return mCurrentState;
    }

    /**
     *
     * @param mCurrentState
     */
    public void setCurrentState(String mCurrentState) {
        this.mCurrentState = mCurrentState;
    }

    /**
     *
     * @param in
     */
    public Trigger(Parcel in) {
        mPackage = in.readString();
        mTriggerId = in.readString();
        mTriggerDisplayName = in.readString();
        mStates = in.readBundle(State.class.getClassLoader());
        if (in.readInt() == 1) {
            mCurrentState = in.readString();
        } else {
            mCurrentState = null;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackage);
        dest.writeString(mTriggerId);
        dest.writeString(mTriggerDisplayName);
        dest.writeBundle(mStates);
        dest.writeString(mCurrentState);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Trigger> CREATOR
            = new Parcelable.Creator<Trigger>() {
        @Override
        public Trigger createFromParcel(Parcel in) {
            return new Trigger(in);
        }

        @Override
        public Trigger[] newArray(int size) {
            return new Trigger[size];
        }
    };

    /**
     *
     * @return
     */
    public String getPackage() {
        return mPackage;
    }

    /**
     * @hide
     * Overridden by service to ensure
     * package name is not spoofed 
     */
    public void setPackage(String _package) {
        this.mPackage = _package;
    }

    /**
     *
     * @return
     */
    public String getTriggerId() {
        return mTriggerId;
    }

    /**
     *
     * @param triggerId
     */
    public void setTriggerId(String triggerId) {
        this.mTriggerId = triggerId;
    }

    /**
     *
     * @return
     */
    public String getTriggerDisplayName() {
        return mTriggerDisplayName;
    }

    /**
     *
     * @param triggerDisplayName
     */
    public void setTriggerDisplayName(String triggerDisplayName) {
        this.mTriggerDisplayName = triggerDisplayName;
    }

    /**
     *
     * @return
     */
    public Bundle getStates() {
        return mStates;
    }
}
