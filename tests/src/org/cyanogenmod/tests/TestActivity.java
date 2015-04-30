/*
 * Copyright (C) 2008 The Android Open Source Project
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