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

package org.cyanogenmod.tests.versioning;

import android.widget.Toast;
import cyanogenmod.os.Build;

import org.cyanogenmod.tests.TestActivity;

public class VersioningTest extends TestActivity {
    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("test retrieve version") {
                public void run() {
                    Toast.makeText(VersioningTest.this,
                            "Current API version is " + Build.CM_VERSION.SDK_INT + " which is "
                                    + Build.getNameForSDKInt(Build.CM_VERSION.SDK_INT),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("test target version larger") {
                public void run() {
                    int currentapiVersion = Build.CM_VERSION.SDK_INT;
                    if (currentapiVersion >= Build.CM_VERSION_CODES.APRICOT){
                        Toast.makeText(VersioningTest.this,
                                "Current API version is greater or equal to "
                                        + Build.getNameForSDKInt(Build.CM_VERSION_CODES.APRICOT),
                                Toast.LENGTH_LONG).show();
                    } else{
                        Toast.makeText(VersioningTest.this,
                                "Current API version is below target SKD version "
                                        + Build.CM_VERSION_CODES.APRICOT,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
    };
}
