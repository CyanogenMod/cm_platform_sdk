package org.cyanogenmod.samples.extview;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import cyanogenmod.externalviews.ExternalViewProvider;

public class SampleProviderService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new Provider(this).getBinder();
    }

    private class Provider extends ExternalViewProvider {
        public Provider(Context context) {
            super(context);
        }

        @Override
        protected View onCreateView() {
            return LayoutInflater.from(SampleProviderService.this)
                    .inflate(R.layout.main, null);
        }
    }
}
