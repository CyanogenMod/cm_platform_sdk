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
 * An trigger that can be utilized for a CyanogenMod profile.
 */
public class Trigger implements Parcelable {

    private String mTriggerId;
    private String mTriggerDisplayName;
    private Bundle mStates = new Bundle();
    private String mCurrentState;

    /**
     * {@link State} object for the {@link Trigger} which allows actions to be fired
     */
    public static final class State implements Parcelable {
        private String mKey;
        private String mDescription;

        /**
         *
         * @param key an identifier key to be used for the state
         * @param description a description to be used for the state
         */
        public State(String key, String description) {
            mKey = key;
            mDescription = description;
        }

        /**
         * Unflatten the State from a parcel.
         * @param in
         */
        public State(Parcel in) {
            mKey = in.readString();
            mDescription = in.readString();
        }

        /**
         * Get the key associated with the {@link State}
         * @return
         */
        public String getKey() {
            return mKey;
        }

        /**
         * Set a key for the {@link State} to be used as a state identifier
         * @param key
         */
        public void setKey(String key) {
            this.mKey = key;
        }

        /**
         * Get the description associated with the {@link State}
         * @return a description
         */
        public String getDescription() {
            return mDescription;
        }

        /**
         * Set the description associated with the {@link State}
         * @param description
         */
        public void setDescription(String description) {
            this.mDescription = description;
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
     * Constructs an Trigger object with default values.
     */
    public Trigger() {
        // Empty constructor
    }

    /**
     * Construct a Trigger object with specific default values
     * @param triggerId a unique identifier that can be referenced
     * @param triggerDisplayname title text for the Trigger
     */
    public Trigger(String triggerId, String triggerDisplayname) {
        this.mTriggerId = triggerId;
        this.mTriggerDisplayName = triggerDisplayname;
    }

    /**
     * Unflatten a {@link Trigger} object from a parcel
     * @param in
     */
    public Trigger(Parcel in) {
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
        dest.writeString(mTriggerId);
        dest.writeString(mTriggerDisplayName);
        dest.writeBundle(mStates);
        dest.writeString(mCurrentState);
    }

    @Override
    public Trigger clone() {
        Trigger trigger = new Trigger();
        cloneInto(trigger);
        return trigger;
    }

    /**
     * Copy all of this into that
     * @hide
     */
    public void cloneInto(Trigger that) {
        that.mTriggerId = this.mTriggerId;
        that.mTriggerDisplayName = this.mTriggerDisplayName;
        that.mCurrentState = this.mCurrentState;
        that.mStates = this.mStates;
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
     * Add a {@link State} to the {@link Trigger}
     * @param state
     */
    public void addState(State state) {
        mStates.putParcelable(state.getKey(), state);
    }

    /**
     * Get the current state of the {@link Trigger}
     * @return
     */
    public String getCurrentState() {
        return mCurrentState;
    }

    /**
     * Set the current state for the {@link Trigger}
     * @param mCurrentState
     */
    public void setCurrentState(String mCurrentState) {
        this.mCurrentState = mCurrentState;
    }

    /**
     * Get the string identifier associated with the {@link Trigger}
     * @return
     */
    public String getTriggerId() {
        return mTriggerId;
    }

    /**
     * Set an identifier for the {@link Trigger}
     * @param triggerId
     */
    public void setTriggerId(String triggerId) {
        this.mTriggerId = triggerId;
    }

    /**
     * Get the title text associated with the {@link Trigger}
     * @return
     */
    public String getTriggerDisplayName() {
        return mTriggerDisplayName;
    }

    /**
     * Set the title text to be displayed to the user in Settings for this trigger
     * @param triggerDisplayName
     */
    public void setTriggerDisplayName(String triggerDisplayName) {
        this.mTriggerDisplayName = triggerDisplayName;
    }

    /**
     * Get the {@link State}s associated with the {@link Trigger}
     * @return states
     */
    public Bundle getStates() {
        return mStates;
    }
}
