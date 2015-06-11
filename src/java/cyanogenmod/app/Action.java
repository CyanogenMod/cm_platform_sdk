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

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An action that can be invoked when a CyanogenMod profile is activated.
 */
public class Action implements Parcelable {
    private String mTitle;
    private String mDescription;
    private PendingIntent mAction;
    private Bundle mStates = new Bundle();

    /**
     * Constructs an Action object with default values.
     */
    public Action() {
        // Empty constructor
    }

    /**
     * Unflatten the Action from a parcel.
     */
    public Action(Parcel in) {
        mTitle = in.readString();
        mDescription = in.readString();
        mAction = in.readParcelable(PendingIntent.class.getClassLoader());
        mStates = in.readBundle(State.class.getClassLoader());
    }

    /**
     * {@link State} object for the {@link Action} which allows toggling between descriptions when
     * the {@link Action} is fired and cycles.
     */
    public static final class State implements Parcelable {
        private String mKey;
        private String mDescription;

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

        public static final Parcelable.Creator<State> CREATOR = new Parcelable.Creator<State>() {
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
     * Get the title associated with the {@link Action}
     * @return title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Set the title associated with the {@link Action}
     * @param title
     */
    public void setTitle(String title) {
        this.mTitle = title;
    }

    /**
     * Get the description associated with the {@link Action}
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Set the description associated with the {@link Action}
     * @param description
     */
    public void setDescription(String description) {
        this.mDescription = description;
    }

    /**
     * Get the {@link PendingIntent} associated with the {@link Action}
     * @return pending intent to be fired
     */
    public PendingIntent getAction() {
        return mAction;
    }

    /**
     * Set the {@link PendingIntent} that will be fired when the {@link Action} is
     * fired.
     * @param action
     */
    public void setAction(PendingIntent action) {
        this.mAction = action;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeParcelable(mAction, 0);
        dest.writeBundle(mStates);
    }

    @Override
    protected Action clone() {
        Action that = new Action();
        cloneInto(that);
        return that;
    }

    /**
     * Copy all of this into that
     * @hide
     */
    public void cloneInto(Action that) {
        that.mTitle = this.mTitle;
        that.mDescription = this.mDescription;
        that.mAction = this.mAction;
        that.mStates = this.mStates;
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

    /**
     * Add a {@link State} to the {@link Action}
     * @param state
     */
    public void addState(State state) {
        mStates.putParcelable(state.getKey(), state);
    }

    /**
     * Get the {@link State}s associated with the {@link Action}
     * @return states
     */
    public Bundle getStates() {
        return mStates;
    }
}