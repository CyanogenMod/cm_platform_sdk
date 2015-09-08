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

package org.cyanogenmod.platform.internal;

import android.content.Intent;
import cyanogenmod.app.suggest.ApplicationSuggestion;

import java.util.List;

/**
 * App Suggestion Manager's interface for Applicaiton Suggestion Providers.
 *
 * @hide
 */
public interface AppSuggestProviderInterface {
    boolean handles(Intent intent);
    List<ApplicationSuggestion> getSuggestions(Intent intent);
}
