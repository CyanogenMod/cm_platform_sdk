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
