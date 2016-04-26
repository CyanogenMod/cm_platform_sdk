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

package org.cyanogenmod.platform.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.SystemService;
import cyanogenmod.app.CMContextConstants;

import org.cyanogenmod.internal.themes.IIconCacheManager;
import org.cyanogenmod.internal.util.ThemeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;

/** @hide */
public class IconCacheManagerService extends CMSystemService {
    private static final String TAG = IconCacheManagerService.class.getSimpleName();

    private static final long MAX_ICON_CACHE_SIZE = 33554432L; // 32MB
    private static final long PURGED_ICON_CACHE_SIZE = 25165824L; // 24 MB

    private long mIconCacheSize = 0L;
    private Context mContext;

    public IconCacheManagerService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getFeatureDeclaration() {
        return CMContextConstants.Features.THEMES;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "registerIconCache cmiconcache: " + this);
        publishBinderService(CMContextConstants.CM_ICON_CACHE_SERVICE, mService);
    }

    private void purgeIconCache() {
        Log.d(TAG, "Purging icon cahe of size " + mIconCacheSize);
        File cacheDir = new File(ThemeUtils.SYSTEM_THEME_ICON_CACHE_DIR);
        File[] files = cacheDir.listFiles();
        Arrays.sort(files, mOldestFilesFirstComparator);
        for (File f : files) {
            if (!f.isDirectory()) {
                final long size = f.length();
                if(f.delete()) mIconCacheSize -= size;
            }
            if (mIconCacheSize <= PURGED_ICON_CACHE_SIZE) break;
        }
    }

    private Comparator<File> mOldestFilesFirstComparator = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            return (int) (lhs.lastModified() - rhs.lastModified());
        }
    };

    private IBinder mService = new IIconCacheManager.Stub() {
        @Override
        public boolean cacheComposedIcon(Bitmap icon, String fileName) throws RemoteException {
            final long token = Binder.clearCallingIdentity();
            boolean success;
            FileOutputStream os;
            final File cacheDir = new File(ThemeUtils.SYSTEM_THEME_ICON_CACHE_DIR);
            if (cacheDir.listFiles().length == 0) {
                mIconCacheSize = 0;
            }
            try {
                File outFile = new File(cacheDir, fileName);
                os = new FileOutputStream(outFile);
                icon.compress(Bitmap.CompressFormat.PNG, 90, os);
                os.close();
                FileUtils.setPermissions(outFile,
                        FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IROTH,
                        -1, -1);
                mIconCacheSize += outFile.length();
                if (mIconCacheSize > MAX_ICON_CACHE_SIZE) {
                    purgeIconCache();
                }
                success = true;
            } catch (Exception e) {
                success = false;
                Log.w(TAG, "Unable to cache icon " + fileName, e);
            }
            Binder.restoreCallingIdentity(token);
            return success;
        }

    };
}
