/*
 * Copyright (c) 2011-2015 CyanogenMod Project
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

import android.annotation.NonNull;
import android.content.ComponentName;
import android.os.RemoteException;
import android.util.Slog;

import android.app.NotificationGroup;
import android.content.Context;
import android.os.IBinder;
import android.os.ParcelUuid;

import cyanogenmod.app.CMContextConstants;
import cyanogenmod.app.Profile;
import cyanogenmod.app.IProfileManager;

/** @hide */
public class ProfileManagerServiceBroker extends BrokerableCMSystemService<IProfileManager> {

    private static final String TAG = "CMProfileServiceBroker";
    private static final ComponentName TARGET_IMPLEMENTATION_COMPONENT =
            new ComponentName("org.cyanogenmod.cmprofileservice",
                    "org.cyanogenmod.cmprofileservice.CMProfileManagerService");
    // Enable the below for detailed logging of this class
    private static final boolean LOCAL_LOGV = false;

    private Context mContext;

    public ProfileManagerServiceBroker(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService(CMContextConstants.CM_PROFILE_SERVICE, new BinderService());
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.PROFILES;
    }

    @Override
    protected IProfileManager getIBinderAsIInterface(@NonNull IBinder service) {
        return IProfileManager.Stub.asInterface(service);
    }

    @Override
    protected IProfileManager getDefaultImplementation() {
        return mFailureImpl;
    }

    @Override
    protected ComponentName getServiceComponent() {
        return TARGET_IMPLEMENTATION_COMPONENT;
    }

    private final IProfileManager mFailureImpl = new IProfileManager() {

        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void resetAll() {
        }

        @Override
        @Deprecated
        public boolean setActiveProfileByName(String profileName) {
            return false;
        }

        @Override
        public boolean setActiveProfile(ParcelUuid profileParcelUuid) {
            return false;
        }

        @Override
        public boolean addProfile(Profile profile) {
            return false;
        }

        @Override
        @Deprecated
        public Profile getProfileByName(String profileName) {
            return null;
        }

        @Override
        public Profile getProfile(ParcelUuid profileParcelUuid) {
            return null;
        }

        @Override
        public Profile[] getProfiles() {
            return null;
        }

        @Override
        public Profile getActiveProfile() {
            return null;
        }

        @Override
        public boolean removeProfile(Profile profile) {
            return false;
        }

        @Override
        public void updateProfile(Profile profile) {
        }

        @Override
        public boolean profileExists(ParcelUuid profileUuid) {
            return false;
        }

        @Override
        @Deprecated
        public boolean profileExistsByName(String profileName) {
            return false;
        }

        @Override
        @Deprecated
        public boolean notificationGroupExistsByName(String notificationGroupName) {
            return false;
        }

        @Override
        public NotificationGroup[] getNotificationGroups() {
            return null;
        }

        @Override
        public void addNotificationGroup(NotificationGroup group) {
        }

        @Override
        public void removeNotificationGroup(NotificationGroup group) {
        }

        @Override
        public void updateNotificationGroup(NotificationGroup group) {
        }

        @Override
        public NotificationGroup getNotificationGroupForPackage(String pkg) {
            return null;
        }

        @Override
        public NotificationGroup getNotificationGroup(ParcelUuid uuid) {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    };

    private class BinderService extends IProfileManager.Stub {
        @Override
        public void resetAll() {
            enforceChangePermissions();
            try {
                getBrokeredService().resetAll();
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        @Override
        @Deprecated
        public boolean setActiveProfileByName(String profileName) {
            enforceChangePermissions();
            try {
                return getBrokeredService().setActiveProfileByName(profileName);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        public boolean setActiveProfile(ParcelUuid profileParcelUuid) {
            enforceChangePermissions();
            try {
                return getBrokeredService().setActiveProfile(profileParcelUuid);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        public boolean addProfile(Profile profile) {
            enforceChangePermissions();
            try {
                return getBrokeredService().addProfile(profile);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        @Deprecated
        public Profile getProfileByName(String profileName) {
            try {
                return getBrokeredService().getProfileByName(profileName);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public Profile getProfile(ParcelUuid profileParcelUuid) {
            try {
                return getBrokeredService().getProfile(profileParcelUuid);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public Profile[] getProfiles() {
            try {
                return getBrokeredService().getProfiles();
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public Profile getActiveProfile() {
            try {
                return getBrokeredService().getActiveProfile();
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public boolean removeProfile(Profile profile) {
            enforceChangePermissions();
            try {
                return getBrokeredService().removeProfile(profile);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        public void updateProfile(Profile profile) {
            enforceChangePermissions();
            try {
                getBrokeredService().updateProfile(profile);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        @Override
        public boolean profileExists(ParcelUuid profileUuid) {
            try {
                return getBrokeredService().profileExists(profileUuid);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        @Deprecated
        public boolean profileExistsByName(String profileName) {
            try {
                return getBrokeredService().profileExistsByName(profileName);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        @Deprecated
        public boolean notificationGroupExistsByName(String notificationGroupName) {
            try {
                return getBrokeredService().notificationGroupExistsByName(notificationGroupName);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        public NotificationGroup[] getNotificationGroups() {
            try {
                return getBrokeredService().getNotificationGroups();
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public void addNotificationGroup(NotificationGroup group) {
            enforceChangePermissions();
            try {
                getBrokeredService().addNotificationGroup(group);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        @Override
        public void removeNotificationGroup(NotificationGroup group) {
            enforceChangePermissions();
            try {
                getBrokeredService().removeNotificationGroup(group);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        @Override
        public void updateNotificationGroup(NotificationGroup group) {
            enforceChangePermissions();
            try {
                getBrokeredService().updateNotificationGroup(group);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        @Override
        public NotificationGroup getNotificationGroupForPackage(String pkg) {
            try {
                return getBrokeredService().getNotificationGroupForPackage(pkg);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public NotificationGroup getNotificationGroup(ParcelUuid uuid) {
            try {
                return getBrokeredService().getNotificationGroup(uuid);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        public boolean isEnabled() {
            try {
                return getBrokeredService().isEnabled();
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return false;
            }
        }
    }

    private void enforceChangePermissions() {
        mContext.enforceCallingOrSelfPermission(
                cyanogenmod.platform.Manifest.permission.MODIFY_PROFILES,
                "You do not have permissions to change the Profile Manager.");
    }
}
