package org.cyanogenmod.platform.internal;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.android.server.SystemService;
import cyanogenmod.app.ICMDynamicLaunchablesManager;

/**
 * Service for launchables that launches intents to the Catapult launcher
 * or any other launcher that can updates itself
 *
 * Created by rsubramanian on 5/3/15.
 */
public class CMDynamicLaunchablesService extends SystemService {
    private static final String TAG = CMDynamicLaunchablesService.class.getSimpleName();
    private static final String SERVICE_NAME = "cmdynamiclaunchables";

    public CMDynamicLaunchablesService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(SERVICE_NAME, mService);
    }

    private final IBinder mService = new ICMDynamicLaunchablesManager.Stub() {

        private boolean mEnabled = false;
        @Override
        public void enableDynamicWidgetry(String pkg, boolean enable) {
            // permission check for package here, presumably
            mEnabled = enable;
        }

        @Override
        public boolean isEnabledDynamicWidgetry(String pkg) {
            return mEnabled;
        }

        // Change the layout used by the app to be one of a set of predefined templates
        // (each would contain views id'd as TEXT1, IMAGE1, TEXT2, IMAGE2)
        @Override
        public void setWidgetTemplateLayout(String pkg, int id) {}

        // Sets the various template values for the widget
        @Override
        public void populateWidgetTemplate(String pkg, Bundle values) {
            if (mEnabled) {
                Log.d(TAG, "populateWidgetTemplate: TEXT1 was set to " + values.getString("TEXT1") + "'");
                Intent i = new Intent();
                i.setClassName("com.cyngn.catapult", "com.cyngn.catapult.WidgetGeneratorService");
                // set text value here
                i.putExtra("TEXT1", values.getString("TEXT1"));
                Log.d(TAG, "populateWidgetTemplate: Launching Catapult service");
                mContext.startService(i);
            } else {
                Log.w(TAG, "populateWidgetTemplate: service not enabled");
            }
        }
    };
}
