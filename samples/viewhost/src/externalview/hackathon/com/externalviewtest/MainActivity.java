package externalview.hackathon.com.externalviewtest;

import android.app.Activity;
import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import cyanogenmod.externalviews.ExternalView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ComponentName componentName = new ComponentName("org.cyanogenmod.samples.extview",
                "org.cyanogenmod.samples.extview.SampleProviderService");
        //LinearLayout linearLayout = (LinearLayout) findViewById(R.id.root);
//        ExternalView externalView = new ExternalView(this, componentName);
//        externalView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400));
//        externalView.setBackgroundColor(Color.RED);
//        linearLayout.addView(externalView, 0);
        //setContentView(externalView);
    }

}
