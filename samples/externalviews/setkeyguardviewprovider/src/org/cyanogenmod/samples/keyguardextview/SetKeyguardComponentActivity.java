/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package org.cyanogenmod.samples.setkeyguardextview;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.cyanogenmod.internal.util.CmLockPatternUtils;

public class SetKeyguardComponentActivity extends Activity {
    private static final String TAG = SetKeyguardComponentActivity.class.getSimpleName();
    private static final String KEYGUARD_PACKAGE = "org.cyanogenmod.samples.keyguardextview";
    private static final String KEYGUARD_COMPONENT =
            KEYGUARD_PACKAGE + ".SampleKeyguardProviderService";

    private Button mClearKeyguardButton;
    private Button mSetKeyguardButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_keyguard);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mClearKeyguardButton = (Button) findViewById(R.id.clear_keyguard);
        mClearKeyguardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setKeyguardComponent(null);
            }
        });

        mSetKeyguardButton = (Button) findViewById(R.id.set_keyguard);
        mSetKeyguardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    setKeyguardComponent(new ComponentName(KEYGUARD_PACKAGE, KEYGUARD_COMPONENT));
            }
        });
    }

    private void setKeyguardComponent(ComponentName cn) {
        CmLockPatternUtils lockPatternUtils =
                new CmLockPatternUtils(this);
        try {
            lockPatternUtils.setThirdPartyKeyguard(cn);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to set third party keyguard component", e);
        }
    }
}