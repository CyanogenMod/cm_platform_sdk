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
package cyanogenmod.externalviews;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.AttributeSet;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.security.InvalidParameterException;

public final class KeyguardExternalView extends ExternalView {

    public static final String EXTRA_GRANT_PERMISSION_RECEIVER = "grant_permission_result_receiver";
    public static final String EXTRA_PERMISSION_LIST = "permissions_name_list";
    public static final String EXTRA_GRANT_PERMISSION_RESULT_LIST = "permissions_grant_result_list";

    private static final String TAG = KeyguardExternalView.class.getSimpleName();
    private static final String CATEGORY_KEYGUARD_GRANT_PERMISSION
            = "org.cyanogenmod.intent.category.KEYGUARD_GRANT_PERMISSION";
    private boolean DEBUG = true;

    public KeyguardExternalView(Context context, AttributeSet attrs) {
        this(context,attrs,getComponentFromAttribute(attrs));
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context,attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        this(context,attrs);
    }

    public KeyguardExternalView(Context context, AttributeSet attributeSet,
            ComponentName componentName) {
        super(context,attributeSet,componentName,false);
        checkPermissions();
    }

    private void checkPermissions() {
        LinkedList<String> permissionsNotGranted = new LinkedList<String>();
        PackageInfo pkgInfo = null;
        try {
            pkgInfo = mContext.getPackageManager().
                    getPackageInfo(mExtensionComponent.getPackageName(),
                            PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            throw new InvalidParameterException("Package " + mExtensionComponent.getPackageName()
                    + " not found");
        }

        String[] requestedPermissions  = pkgInfo.requestedPermissions;
        int[] requestedPermissionsFlags = pkgInfo.requestedPermissionsFlags;

        for (int indx = 0 ; indx < requestedPermissions.length ; indx++) {
            if ((requestedPermissionsFlags[indx] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                try {
                    PermissionInfo pi = mContext.getPackageManager()
                        .getPermissionInfo(requestedPermissions[indx],0);

                    if (pi.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS) {
                        int permissionFlags = mContext.getPackageManager().getPermissionFlags(
                                requestedPermissions[indx],mExtensionComponent.getPackageName(),
                                    android.os.Process.myUserHandle());
                        //Checks if user does not want to be asked again
                        if ((permissionFlags & PackageManager.FLAG_PERMISSION_USER_FIXED) == 0) {
                            permissionsNotGranted.add(requestedPermissions[indx]);
                            if (DEBUG) {
                                Log.d(TAG,"Permission " + requestedPermissions[indx] + "not granted");
                            }
                        }
                    }
                } catch(NameNotFoundException e) {
                    //TODO: What to do if PM can't find the permission??
                    if (DEBUG) {
                        Log.w(TAG,"Permission " + requestedPermissions[indx] + " not found. "
                          + "Unable to confirm if has been granted");
                    }
                }
            }
        }

        if (permissionsNotGranted.size() == 0) {
            bindToService();
        } else {
            Intent rIntent = new Intent()
                    .setPackage(mExtensionComponent.getPackageName())
                    .addCategory(CATEGORY_KEYGUARD_GRANT_PERMISSION);

            List<ResolveInfo> resolveInfo = mContext.getPackageManager().
                    queryIntentActivities(rIntent,PackageManager.GET_RESOLVED_FILTER);

            if (resolveInfo.size() >= 1) {
                if (DEBUG) {
                    //TODO: Move this code out of the DEBUG block if
                    //we really must act if the condition is met
                    if (resolveInfo.size() >= 2) {
                        Log.w(TAG,"Got " + resolveInfo.size() + " resolvers! Defaulting to "
                                + resolveInfo.get(0).activityInfo.name);
                    }
                }

                Intent mIntent = new Intent()
                        .setComponent(new ComponentName(mExtensionComponent.getPackageName(),
                                resolveInfo.get(0).activityInfo.name))
                        .addCategory(CATEGORY_KEYGUARD_GRANT_PERMISSION)
                        .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                  | Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(EXTRA_GRANT_PERMISSION_RECEIVER,
                                new GrantPermissionResultReceiver())
                        .putExtra(EXTRA_PERMISSION_LIST,permissionsNotGranted.toArray(
                                new String[permissionsNotGranted.size()]));
                mContext.startActivity(mIntent);
            } else {
                //We are here because there are permissions not granted yet, but
                //the package does not provide an activity we can start to request
                //the permissions. No way to know this in advance so this is in fact
                //a runtime problem
                throw new RuntimeException("Unable to find an activity with category "
                        + CATEGORY_KEYGUARD_GRANT_PERMISSION + " in package "
                        + mExtensionComponent.getPackageName());
            }
        }
    }

    private class GrantPermissionResultReceiver extends ResultReceiver {
        public GrantPermissionResultReceiver() {
            super(null);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            int[] permissionsGranted = resultData.getIntArray(EXTRA_GRANT_PERMISSION_RESULT_LIST);
            String[] permissions = resultData.getStringArray(EXTRA_PERMISSION_LIST);
            for (int indx = 0 ; indx < permissionsGranted.length; indx++) {
                if (permissionsGranted[indx] != PackageManager.PERMISSION_GRANTED) {
                    //TODO: Do we want(need) to let the user know that some functionality
                    //might not work properly (or at all!) because the permission was revoked?
                    if (DEBUG) Log.w(TAG,"Permission " + permissions[indx] + " was not granted");
                }
            }
            bindToService();
        }
    }
}
