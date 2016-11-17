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

package cyanogenmod.hardware;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Touchscreen gestures API
 *
 * A device may implement several touchscreen gestures for use while
 * the display is turned off, such as drawing alphabets and shapes.
 * These gestures can be interpreted by userspace to activate certain
 * actions and launch certain apps, such as to skip music tracks,
 * to turn on the flashlight, or to launch the camera app.
 *
 * This *should always* be supported by the hardware directly.
 * A lot of recent touch controllers have a firmware option for this.
 *
 * A TouchscreenGesture is referenced by it's identifier and carries an
 * associated path.
 * Identifiers list:
 *  0: Circle
 *  1: Two fingers downwards
 *  2: Leftwards arrow
 *  3: Rightwards arrow
 *  4: Letter "C"
 *  5: Letter "e"
 *  6: Letter "S"
 *  7: Letter "V"
 *  8: Letter "W"
 *  9: Letter "Z"
 */
public class TouchscreenGesture implements Parcelable {

    public static final int ID_CIRCLE = 0;
    public static final int ID_TWO_FINGERS_DOWNWARDS = 1;
    public static final int ID_LEFTWARDS_ARROW = 2;
    public static final int ID_RIGHTWARDS_ARROW = 3;
    public static final int ID_LETTER_C = 4;
    public static final int ID_LETTER_E = 5;
    public static final int ID_LETTER_S = 6;
    public static final int ID_LETTER_V = 7;
    public static final int ID_LETTER_W = 8;
    public static final int ID_LETTER_Z = 9;

    public final int id;
    public final String path;
    public final int keycode;

    public TouchscreenGesture(int id, String path, int keycode) {
        this.id = id;
        this.path = path;
        this.keycode = keycode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(path);
        parcel.writeInt(keycode);
    }

    /** @hide */
    public static final Parcelable.Creator<TouchscreenGesture> CREATOR =
            new Parcelable.Creator<TouchscreenGesture>() {

        public TouchscreenGesture createFromParcel(Parcel in) {
            return new TouchscreenGesture(in.readInt(), in.readString(), in.readInt());
        }

        @Override
        public TouchscreenGesture[] newArray(int size) {
            return new TouchscreenGesture[size];
        }
    };
}
