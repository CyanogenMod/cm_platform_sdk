package org.cyanogenmod.tests.telephony;

import android.telephony.SubscriptionManager;
import android.widget.Toast;
import org.cyanogenmod.tests.TestActivity;

import cyanogenmod.app.CMTelephonyManager;

/**
 * Created by adnan on 8/6/15.
 */
public class CMTelephonyTest extends TestActivity {
    @Override
    protected String tag() {
        return null;
    }

    @Override
    protected Test[] tests() {
        return mTests;
    }

    private Test[] mTests = new Test[] {
            new Test("test retreive list of subscription information") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    Toast.makeText(CMTelephonyTest.this, "Test retrieve info "
                                    + cmTelephonyManager.getSubInformation().toString(),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("test is default subscription active") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    Toast.makeText(CMTelephonyTest.this, "Test default sub active "
                                    + cmTelephonyManager.isSubActive(
                                    SubscriptionManager.getDefaultSubscriptionId()),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("test setSubState on default subscription") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    cmTelephonyManager.setSubState(SubscriptionManager.getDefaultSubscriptionId(), true);
                }
            },
            new Test("test is data enabled on default sub") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    Toast.makeText(CMTelephonyTest.this, "Test default sub data "
                                    + cmTelephonyManager.isDataConnectionSelectedOnSub(
                                    SubscriptionManager.getDefaultSubscriptionId()),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("test is data enabled") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    Toast.makeText(CMTelephonyTest.this, "Test data enabled "
                                    + cmTelephonyManager.isDataConnectionEnabled(),
                            Toast.LENGTH_SHORT).show();
                }
            },
            new Test("test setDataConnectionState") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    cmTelephonyManager.setDataConnectionState(true);
                }
            },
            new Test("test setDataConnectionSelectedOnSub") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    cmTelephonyManager.setDataConnectionSelectedOnSub(
                            SubscriptionManager.getDefaultSubscriptionId());
                }
            },
            new Test("test setDefaultPhoneSub") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    cmTelephonyManager.setDefaultPhoneSub(
                            SubscriptionManager.getDefaultSubscriptionId());
                }
            },
            new Test("test setDefaultSmsSub") {
                public void run() {
                    CMTelephonyManager cmTelephonyManager =
                            CMTelephonyManager.getInstance(CMTelephonyTest.this);
                    cmTelephonyManager.setDefaultSmsSub(
                            SubscriptionManager.getDefaultSubscriptionId());
                }
            },
    };
}
