package cyanogenmod.app.profiles;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Trigger implements Parcelable {

    private String mPackage;
    private String mTriggerId;
    private String mTriggerDisplayName;
    private Bundle mStates = new Bundle();
    private String mCurrentState;

    public static final class State implements Parcelable {
        private String mKey;
        private String mDescription;
        public String getKey() {
            return mKey;
        }
        public void setKey(String mKey) {
            this.mKey = mKey;
        }
        public String getDescription() {
            return mDescription;
        }
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
    }

    public Trigger(String triggerId, String triggerDisplayname) {
        this.mTriggerId = triggerId;
        this.mTriggerDisplayName = triggerDisplayname;
    }

    public void addState(State state) {
        mStates.putParcelable(state.getKey(), state);
    }

    public String getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(String mCurrentState) {
        this.mCurrentState = mCurrentState;
    }

    public Trigger(Parcel in) {
        mPackage = in.readString();
        mTriggerId = in.readString();
        mTriggerDisplayName = in.readString();
        mStates = in.readBundle();
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

    public String getPackage() {
        return mPackage;
    }

    /**
     * @hide
     * Overridden by service to ensure
     * package name is not spoofed 
     */
    public void setPackage(String mPackage) {
        this.mPackage = mPackage;
    }

    public String getTriggerId() {
        return mTriggerId;
    }

    public void setTriggerId(String mTriggerId) {
        this.mTriggerId = mTriggerId;
    }

    public String getTriggerDisplayName() {
        return mTriggerDisplayName;
    }

    public void setTriggerDisplayName(String mTriggerDisplayName) {
        this.mTriggerDisplayName = mTriggerDisplayName;
    }

    public Bundle getStates() {
        return mStates;
    }
}
