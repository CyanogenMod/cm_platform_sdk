package cyanogenmod.app.profiles;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class CustomTriggerInfo implements Parcelable {

    public String mPackage;
    public String mTriggerId;
    public String mTriggerDisplayName;
    public List<String> mStates;
    public String mCurrentState;

    public CustomTriggerInfo(String pkg, String triggerId, String triggerDisplayname,
                             List<String> triggerStates, String currentState) {
        this.mPackage = pkg;
        this.mTriggerId = triggerId;
        this.mTriggerDisplayName = triggerDisplayname;
        this.mStates = triggerStates;
        this.mCurrentState = currentState;
    }

    protected CustomTriggerInfo(Parcel in) {
        mPackage = in.readString();
        mTriggerId = in.readString();
        mTriggerDisplayName = in.readString();
        in.readStringList(mStates = new ArrayList<String>());
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
        dest.writeStringList(mStates);
        if (mCurrentState != null) {
            dest.writeInt(1);
            dest.writeString(mCurrentState);
        } else {
            dest.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<CustomTriggerInfo> CREATOR
            = new Parcelable.Creator<CustomTriggerInfo>() {
        @Override
        public CustomTriggerInfo createFromParcel(Parcel in) {
            return new CustomTriggerInfo(in);
        }

        @Override
        public CustomTriggerInfo[] newArray(int size) {
            return new CustomTriggerInfo[size];
        }
    };
}
