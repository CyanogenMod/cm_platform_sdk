/**
 * Copyright (c) 2015, The CyanogenMod Project
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

package org.cyanogenmod.samples.customtiles;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.text.TextUtils;
import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;

import org.cyanogenmod.samples.customtiles.R;

/**
 * Created by Adnan on 4/30/15.
 */
public class TileReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (MainActivity.ACTION_TOGGLE_STATE.equals(intent.getAction())) {
            Intent newIntent = new Intent();
            newIntent.setAction(MainActivity.ACTION_TOGGLE_STATE);
            String label = "CustomTile " + States.STATE_OFF;

            int state = getCurrentState(intent);
            switch (state) {
                case States.STATE_OFF:
                    newIntent.putExtra(MainActivity.STATE, States.STATE_ON);
                    label = "CustomTile " + States.STATE_ON;
                    break;
                case States.STATE_ON:
                    newIntent.putExtra(MainActivity.STATE, States.STATE_OFF);
                    label = "CustomTile " + States.STATE_OFF;
                    break;
            }

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(context, 0,
                            newIntent , PendingIntent.FLAG_UPDATE_CURRENT);

            CustomTile customTile = new CustomTile.Builder(context)
                    .setOnClickIntent(pendingIntent)
                    .shouldCollapsePanel(false)
                    .setContentDescription("Generic content description")
                    .setLabel(label)
                    .setIcon(R.drawable.ic_launcher)
                    .build();

            CMStatusBarManager.getInstance(context)
                    .publishTile(MainActivity.CUSTOM_TILE_ID, customTile);
        }
    }

    private int getCurrentState(Intent intent) {
        return intent.getIntExtra(MainActivity.STATE, 0);
    }
}
