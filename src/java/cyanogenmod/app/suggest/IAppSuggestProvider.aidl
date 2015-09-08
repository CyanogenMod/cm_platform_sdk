// IApplicationSuggestion.aidl
package cyanogenmod.app.suggest;

import android.content.Intent;

import cyanogenmod.app.suggest.ApplicationSuggestion;

/**
 * @hide
 */
interface IAppSuggestProvider {
    boolean handles(in Intent intent);

    List<ApplicationSuggestion> getSuggestions(in Intent intent);
}