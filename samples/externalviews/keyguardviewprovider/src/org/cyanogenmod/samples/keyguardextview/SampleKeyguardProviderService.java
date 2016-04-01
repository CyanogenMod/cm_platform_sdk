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

package org.cyanogenmod.samples.keyguardextview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import cyanogenmod.externalviews.KeyguardExternalViewProviderService;

public class SampleKeyguardProviderService extends KeyguardExternalViewProviderService {

    @Override
    protected KeyguardExternalViewProviderService.Provider createExternalView(Bundle options) {
        return new ProviderImpl(options);
    }

    private class ProviderImpl extends Provider {
        ImageView mImageView;
        Animation mPulseAnimation;

        protected ProviderImpl(Bundle options) {
            super(options);
        }

        /**
         * Create a view that will be displayed within the system's lock screen (aka keyguard)
         * @return The view to be displayed
         */
        @Override
        protected View onCreateView() {
            View view = LayoutInflater.from(SampleKeyguardProviderService.this)
                    .inflate(R.layout.main, null);
            mImageView = (ImageView) view.findViewById(R.id.cid);
            return view;
        }

        /**
         * Called when the keyguard is being shown
         * @param screenOn True if the screen is currently on
         */
        @Override
        protected void onKeyguardShowing(boolean screenOn) {

        }

        /**
         * Called when the user has unlocked their device and the keyguard is dismissed
         */
        @Override
        protected void onKeyguardDismissed() {

        }

        /**
         * Called when the state of the bouncer being shown changes
         * @param showing True if the bouncer is showing
         */
        @Override
        protected void onBouncerShowing(boolean showing) {

        }

        /**
         * Called when the screen has been turned on
         */
        @Override
        protected void onScreenTurnedOn() {
            mImageView.startAnimation(mPulseAnimation);
        }

        /**
         * Called when the screen has been turned off
         */
        @Override
        protected void onScreenTurnedOff() {
            mImageView.clearAnimation();
        }

        @Override
        protected void onLockscreenSlideOffsetChanged(float slideProgress) {

        }

        /**
         * Called when the view has been attached to a window
         */
        @Override
        protected void onAttach() {
            super.onAttach();
            // If this is an interactive component, now is a good time to
            // call setInteractivity(true);
            setInteractivity(false);
            mPulseAnimation = AnimationUtils.loadAnimation(SampleKeyguardProviderService.this,
                    R.anim.pulsing_anim);
            mImageView.startAnimation(mPulseAnimation);
        }

        /**
         * Called when the view has been detached from the window
         */
        @Override
        protected void onDetach() {
            super.onDetach();
        }
    }
}
