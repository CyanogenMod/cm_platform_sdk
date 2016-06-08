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

package org.cyanogenmod.tests.externalviews.keyguardexternalviews;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Space;
import cyanogenmod.externalviews.KeyguardExternalViewProviderService;
import org.mockito.Mockito;

public class ViewProviderService extends KeyguardExternalViewProviderService {
    private ViewProvider mProvider;

    public ViewProviderService() {}

    @Override
    public KeyguardExternalViewProviderService.Provider createExternalView(Bundle options) {
        if (mProvider == null) {
            mProvider = Mockito.spy(new ViewProvider(options));
        }
        return mProvider;
    }

    public ViewProvider getProvider() {
        return mProvider;
    }

    public class ViewProvider extends KeyguardExternalViewProviderService.Provider {
        private ViewProvider mTracker;
        private View mView;

        public ViewProvider(Bundle options) {
            super(options);
        }

        public View getView() {
            return mView;
        }

        public ViewProvider getTracker() {
            return mTracker;
        }

        @Override
        public View onCreateView() {
            if (mTracker == null) {
                mTracker = Mockito.mock(ViewProvider.class);
            }
            mTracker.onCreateView();
            if (mView == null) {
                mView = new Space(getBaseContext());
            }
            return mView;
        }
        @Override
        public void onKeyguardShowing(boolean screenOn) {
            mTracker.onKeyguardShowing(screenOn);
        }
        @Override
        public void onKeyguardDismissed() {
            mTracker.onKeyguardDismissed();
        }
        @Override
        public void onBouncerShowing(boolean showing) {
            mTracker.onBouncerShowing(showing);
        }
        @Override
        public void onScreenTurnedOn() {
            mTracker.onScreenTurnedOn();
        }
        @Override
        public void onScreenTurnedOff() {
            mTracker.onScreenTurnedOff();
        }

        @Override
        protected void onAttach() {
            mTracker.onAttach();
        }

        @Override
        protected void onDetach() {
            mTracker.onDetach();
        }

        @Override
        protected void onLockscreenSlideOffsetChanged(float swipeProgress) {
            mTracker.onLockscreenSlideOffsetChanged(swipeProgress);
        }

        public boolean requestDismissImpl() {
            return requestDismiss();
        }

        public boolean requestDismissAndStartActivityImpl(Intent intent) {
            return requestDismissAndStartActivity(intent);
        }

        public void setInteractivityImpl(boolean interactive) {
            setInteractivity(interactive);
        }

        public void slideLockscreenInImpl() {
            slideLockscreenIn();
        }

        public Bundle getOptionsImpl() {
            return getOptions();
        }

        public void collapseNotificationPanelImpl() {
            mTracker.collapseNotificationPanelImpl();
            collapseNotificationPanel();
        }
    }
};