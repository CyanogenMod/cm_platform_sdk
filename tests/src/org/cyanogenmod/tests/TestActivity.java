package org.cyanogenmod.tests;

import android.app.ListActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public abstract class TestActivity extends ListActivity
{
    Test[] mTests;

    protected abstract String tag();
    protected abstract Test[] tests();

    protected abstract class Test {
        protected String name;
        protected Test(String n) {
            name = n;
        }
        protected abstract void run();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mTests = tests();

        String[] labels = new String[mTests.length];
        for (int i=0; i<mTests.length; i++) {
            labels[i] = mTests[i].name;
        }

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Test t = mTests[position];
        android.util.Log.d(tag(), "Test: " + t.name);
        t.run();
    }

}