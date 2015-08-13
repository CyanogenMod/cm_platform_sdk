package org.cyanogenmod.samples.extview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import cyanogenmod.externalviews.ExternalViewProviderService;

public class SampleProviderService extends ExternalViewProviderService {
    @Override
    protected ExternalViewProviderService.Provider createExternalView(Bundle options) {
        return new ProviderImpl(options);
    }

    private class ProviderImpl extends Provider {
        protected ProviderImpl(Bundle options) {
            super(options);
        }

        @Override
        protected View onCreateView() {
            return LayoutInflater.from(SampleProviderService.this).inflate(R.layout.main, null);
        }
    }
}
