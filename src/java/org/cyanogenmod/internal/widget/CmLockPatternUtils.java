package org.cyanogenmod.internal.widget;

import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.internal.widget.LockPatternUtils;

import cyanogenmod.platform.Manifest;

public class CmLockPatternUtils extends LockPatternUtils {

    /**
     * Third party keyguard component to be displayed within the keyguard
     */
    public static final String THIRD_PARTY_KEYGUARD_COMPONENT = "lockscreen.third_party";

    private Context mContext;

    public CmLockPatternUtils(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Sets a third party lLock screen.
     * @param component
     */
    public void setThirdPartyKeyguard(ComponentName component)
            throws PackageManager.NameNotFoundException {
        if (component != null) {
            // Check that the package this component belongs to has the third party keyguard perm
            final PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(component.getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            boolean hasThirdPartyKeyguardPermission = false;
            for (String perm : pi.requestedPermissions) {
                if (Manifest.permission.THIRD_PARTY_KEYGUARD.equals(perm)) {
                    hasThirdPartyKeyguardPermission = true;
                    break;
                }
            }
            if (!hasThirdPartyKeyguardPermission) {
                throw new SecurityException("Package " + component.getPackageName() + " does not" +
                        "have " + Manifest.permission.THIRD_PARTY_KEYGUARD);
            }
        }

        setString(THIRD_PARTY_KEYGUARD_COMPONENT,
                component != null ? component.flattenToString() : "", getCurrentUser());
    }

    public ComponentName getThirdPartyKeyguardComponent() {
        String component = getString(THIRD_PARTY_KEYGUARD_COMPONENT, getCurrentUser());
        return component != null ? ComponentName.unflattenFromString(component) : null;
    }

    /**
     * @return Whether a third party keyguard is set
     */
    public boolean isThirdPartyKeyguardEnabled() {
        String component = getString(THIRD_PARTY_KEYGUARD_COMPONENT, getCurrentUser());
        return !TextUtils.isEmpty(component);
    }

    private int getCurrentUser() {
        return UserHandle.USER_CURRENT;
    }
}
