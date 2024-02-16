/*

MIT License

Copyright © 2024 HARDCODED JOY S.R.L. (https://hardcodedjoy.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

package com.hardcodedjoy.dev.audioinput;

import android.annotation.SuppressLint;
import android.widget.EditText;

import com.hardcodedjoy.appbase.contentview.ContentView;
import com.hardcodedjoy.appbase.contentview.CvSettingsBase;
import com.hardcodedjoy.appbase.gui.GuiLinker;
import com.hardcodedjoy.appbase.gui.SetGetter;

@SuppressLint("ViewConstructor")
public class CvSettings extends CvSettingsBase {

    @Override
    public void init() {
        super.init();

        //noinspection unused
        Settings settings = (Settings) ContentView.settings;
        addSettings(R.layout.settings);

        EditText etSampleRate = findViewById(R.id.et_sample_rate);

        GuiLinker.link(etSampleRate, new SetGetter() {
            @Override
            public void set(String value) { settings.setSampleRate(value); }
            @Override
            public String get() { return "" + settings.getSampleRate(); }
        });
    }
}