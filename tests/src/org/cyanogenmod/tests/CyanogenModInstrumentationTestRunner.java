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

package org.cyanogenmod.tests;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

/**
 * Created by adnan on 7/15/15.
 */
public class CyanogenModInstrumentationTestRunner extends InstrumentationTestRunner {

    @Override
    public void onCreate (final Bundle arguments) {
        super.onCreate(arguments);

        // temporary workaround for an incompatibility in current dexmaker (1.1)
        // implementation and Android >= 4.3 cf.
        // https://code.google.com/p/dexmaker/issues/detail?id=2 for details
        System.setProperty("dexmaker.dexcache", getTargetContext().getCacheDir().toString());
    }
}
