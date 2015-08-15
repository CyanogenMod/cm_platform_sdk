package com.example.test.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by test on 8/7/15.
 */
public class DeleteIntentReceiver extends BroadcastReceiver {

    public static final String DELETE_ACTION = "com.example.test.myapplication.ACTION_DELETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DELETE_ACTION.equals(intent.getAction())) {
            Toast.makeText(context, "WE WERE DELETED", Toast.LENGTH_SHORT).show();
        }
    }
}
