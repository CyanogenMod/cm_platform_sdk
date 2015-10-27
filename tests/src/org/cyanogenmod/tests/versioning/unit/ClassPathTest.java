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

package org.cyanogenmod.tests.versioning.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;
import dalvik.system.DexFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by adnan on 9/22/15.
 */
public class ClassPathTest extends AndroidTestCase {

    private static final String CYANOGENMOD_NAMESPACE = "cyanogenmod";
    private static final String PATH_TO_SYSTEM_FRAMEWORK = "/system/framework";

    private ArrayList<String> mKnownSdkClasses;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mKnownSdkClasses = getLoadedClasses();
    }

    @SmallTest
    public void testClassLoaderGivesSDKClasses() {
        /**
         * Verify we can retrieve our sdk classes from this package
         */
        assertNotNull(mKnownSdkClasses);
        assertTrue(mKnownSdkClasses.size() > 0);
    }

    @LargeTest
    public void testBootClassPathIsClean() {
        File path = new File(PATH_TO_SYSTEM_FRAMEWORK);
        File[] files = path.listFiles();
        assertNotNull(files);

        /**
         * Everything in the in the boot class path needs to not
         * contain the sdk. Verify integrity of runtime below.
         */
        final String bootClassPath = System.getenv("BOOTCLASSPATH");

        ArrayList<String> classPathJars = new ArrayList<String>();

        if (bootClassPath != null) {
            String[] bootClassPathElements = bootClassPath.split(":");
            for (String element : bootClassPathElements) {
                classPathJars.add(element);
            }
        } else {
            throw new AssertionError("No BOOTCLASSPATH defined! ");
        }

        for (String classPathJar : classPathJars) {
            try {
                File jar = new File(classPathJar);
                DexFile dexFile = new DexFile(jar);
                assertTrue(isJarClean(dexFile));
            } catch (IOException e) {
                throw new AssertionError("Unable to find jar! " + classPathJar +
                        "\nException " + e.toString());
            }
        }
    }

    private ArrayList<String> getLoadedClasses() {
        ArrayList<String> listOfClasses = new ArrayList<String>();
        try {
            DexFile dexFile = new DexFile(new File(mContext.getPackageCodePath()));
            Enumeration<String> enumeration = dexFile.entries();

            while (enumeration.hasMoreElements()){
                String className = enumeration.nextElement();
                if (className.startsWith(CYANOGENMOD_NAMESPACE)) {
                    listOfClasses.add(className);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listOfClasses;
    }

    private void processAndCompare(String name) throws ClassPathException {
        if (mKnownSdkClasses.contains(name)) {
            throw new ClassPathException(name);
        }
    }

    private boolean isJarClean(DexFile dexFile) throws AssertionError {
        Enumeration<String> enumeration = dexFile.entries();

        while (enumeration.hasMoreElements()) {
            try {
                processAndCompare(enumeration.nextElement());
            } catch (ClassPathException e) {
                throw new AssertionError("Jar file "
                        + dexFile.getName() + " " + e.getMessage());
            }
        }
        return true;
    }
}
