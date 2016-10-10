/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.internal.cmparts;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.os.UserHandle;
import android.util.AttributeSet;

import cyanogenmod.platform.Manifest;
import cyanogenmod.preference.SelfRemovingPreference;

import static org.cyanogenmod.internal.cmparts.PartsList.ACTION_PART_CHANGED;
import static org.cyanogenmod.internal.cmparts.PartsList.ACTION_REFRESH_PART;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART;
import static org.cyanogenmod.internal.cmparts.PartsList.EXTRA_PART_KEY;

/**
 * A link to a remote preference screen which can be used with a minimum amount
 * of information. Supports summary updates asynchronously.
 */
public class CMPartsPreference extends SelfRemovingPreference {

    private static final String TAG = "CMPartsPreference";

    private final PartInfo mPart;

    private final IntentFilter mPartChangedFilter;

    public CMPartsPreference(Context context, AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.preferenceScreenStyle);

        mPart = PartsList.getPartInfo(context, getKey());
        if (mPart == null) {
            throw new RuntimeException("Part not found: " + getKey());
        }

        if (!mPart.isAvailable()) {
            setAvailable(false);
        }

        update();

        mPartChangedFilter = new IntentFilter(ACTION_PART_CHANGED);
        mPartChangedFilter.addDataScheme("cmparts");
        mPartChangedFilter.addDataAuthority("cyanogenmod", null);
        mPartChangedFilter.addDataPath(mPart.getName(), PatternMatcher.PATTERN_LITERAL);
        setIntent(mPart.getIntentForActivity());

        requestUpdate();
    }

    @Override
    public void onAttached() {
        super.onAttached();
        getContext().registerReceiver(mPartChangedReceiver, mPartChangedFilter);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        getContext().unregisterReceiver(mPartChangedReceiver);
    }

    private void update() {
        setTitle(mPart.getTitle());
        setSummary((CharSequence) mPart.getSummary());
    }

    private void refreshPartFromBundle(Bundle result) {
        if (mPart.getName().equals(result.getString(EXTRA_PART_KEY))) {
            PartInfo updated = (PartInfo) result.getParcelable(EXTRA_PART);

            if (mPart.updateFrom(updated)) {
                update();
            }
        }
    }

    private void requestUpdate() {
        final Intent i = new Intent(ACTION_REFRESH_PART);
        i.setComponent(PartsList.CMPARTS_REFRESHER);

        i.putExtra(EXTRA_PART_KEY, mPart.getName());

        // Send an ordered broadcast to request a refresh and receive the reply
        // on the BroadcastReceiver.
        getContext().sendOrderedBroadcastAsUser(i, UserHandle.ALL,
                Manifest.permission.BIND_CORE_SERVICE,
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        refreshPartFromBundle(getResultExtras(true));
                    }
                }, null, Activity.RESULT_OK, null, null);
    }

    /**
     * Receiver for asynchronous updates
     */
    private final BroadcastReceiver mPartChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshPartFromBundle(intent.getExtras());
        }
    };
}
