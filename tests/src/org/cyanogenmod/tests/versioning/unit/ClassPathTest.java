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
import java.util.jar.JarEntry;

/**
 * Created by adnan on 9/22/15.
 */
public class ClassPathTest extends AndroidTestCase {

    private static final String CYANOGENMOD_NAMESPACE = "cyanogenmod";
    private static final String PATH_TO_SYSTEM_FRAMEWORK = "/system/framework";
    private static final String PLATFORM_JAR = "org.cyanogenmod.platform.jar";

    private ArrayList<String> mKnownSdkClasses;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mKnownSdkClasses = getLoadedClasses();
    }

    @SmallTest
    public void testClassLoaderGivesSDKClasses() {
        assertNotNull(mKnownSdkClasses);
        assertTrue(mKnownSdkClasses.size() > 0);
    }

    @LargeTest
    public void testClassPathIsClean() {
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
            String[] bootClassPathElements = splitString(bootClassPath, ':');
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

    @SmallTest
    public void testIntegrity() {
        File jar = new File(PATH_TO_SYSTEM_FRAMEWORK + "/" + PLATFORM_JAR);
        try {
            DexFile jarFile = new DexFile(jar);
            assertNotNull(jarFile);
            assertFalse(isJarClean(jarFile));
        } catch (IOException e) {
            throw new AssertionError("Unable to find jar! " + jar.getAbsolutePath() +
                    "\nException " + e.toString());
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

    private void processAndCompare(String name) throws Exception {
        if (mKnownSdkClasses.contains(name)) {
            throw new Exception("contains sdk class " + name);
        }
    }

    private boolean isJarClean(DexFile dexFile) {
        Enumeration<String> enumeration = dexFile.entries();

        while (enumeration.hasMoreElements()) {
            try {
                processAndCompare(enumeration.nextElement());
            } catch (Exception e) {
                throw new AssertionError("Jar file "
                        + dexFile.getName() + " " + e.getMessage());
            }
        }
        return true;
    }

    private static String[] splitString(String str, char sep) {
        int count = 1;
        int i = 0;
        while ((i=str.indexOf(sep, i)) >= 0) {
            count++;
            i++;
        }

        String[] res = new String[count];
        i=0;
        count = 0;
        int lastI=0;
        while ((i=str.indexOf(sep, i)) >= 0) {
            res[count] = str.substring(lastI, i);
            count++;
            i++;
            lastI = i;
        }
        res[count] = str.substring(lastI, str.length());
        return res;
    }
}
