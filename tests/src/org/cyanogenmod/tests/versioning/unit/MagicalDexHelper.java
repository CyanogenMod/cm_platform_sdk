/**
 * Copyright (c) 2016, The CyanogenMod Project
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

package org.cyanogenmod.tests.versioning.unit;

import android.content.Context;
import dalvik.system.DexFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by adnan on 2/3/16.
 */
public class MagicalDexHelper {

    public static ArrayList<String> getLoadedClasses(Context context, String targetNameSpace) {
        ArrayList<String> listOfClasses = new ArrayList<String>();
        try {
            DexFile dexFile = new DexFile(new File(context.getPackageCodePath()));
            Enumeration<String> enumeration = dexFile.entries();

            while (enumeration.hasMoreElements()){
                String className = enumeration.nextElement();
                if (className.startsWith(targetNameSpace)) {
                    listOfClasses.add(className);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listOfClasses;
    }

    public static Class loadClassForNameSpace(Context context,
            String classToLoad) throws IOException {
        DexFile dexFile = new DexFile(new File(context.getPackageCodePath()));
        return dexFile.loadClass(classToLoad, context.getClassLoader());
    }
}
