/*
 * Copyright 2016, The CyanogenMod Project
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

package org.cyanogenmod.internal.cmparts;

import org.cyanogenmod.internal.cmparts.IPartChangedCallback;
import org.cyanogenmod.internal.cmparts.PartInfo;

interface IPartsCatalog {

    String[] getPartsList();

    boolean isPartAvailable(String key);
	
	PartInfo getPartInfo(String key);

    void registerCallback(String key, IPartChangedCallback cb);
    void unregisterCallback(String key, IPartChangedCallback cb);
}
