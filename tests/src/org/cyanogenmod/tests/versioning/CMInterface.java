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

package org.cyanogenmod.tests.versioning;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by adnan on 2/19/16.
 */
public class CMInterface {
    private String mIfaceName;
    private Map<String, Integer> mInterfaceMap;

    public CMInterface(String iFace) {
        mIfaceName = iFace;
        mInterfaceMap = new HashMap<String, Integer>();
    }

    public void addMethod(String methodName, int transactionId) {
        mInterfaceMap.put(methodName, transactionId);
    }

    public Integer getTransactionId(String methodName) {
        return mInterfaceMap.get(methodName);
    }
}
