/*

MIT License

Copyright © 2023 HARDCODED JOY S.R.L. (https://hardcodedjoy.com)

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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.hardcodedjoy.audioinput.MicInput;
import com.hardcodedjoy.util.ColorUtil;
import com.hardcodedjoy.util.GuiUtil;
import com.hardcodedjoy.util.ThemeUtil;

public class MainActivity extends Activity {

    static private final int BUFFER_COUNT = 2;
    static private final int NUM_CHANNELS = 2;
    static private final int PLOT_BUFFER_MILLIS = 100;

    static private final int RQ_CODE_SETTINGS = 1;

    static private MicInput micInput = null;
    static private float[][] buffer = null;
    static private int bufferIndex;
    static private int indexInBuffer;
    static private PlotView[] pvPlots;

    private Settings settings;
    private LinearLayout llMenuOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(getSharedPreferences(getPackageName(), Context.MODE_PRIVATE));

        ThemeUtil.setResIdThemeLight(R.style.AppThemeLight);
        ThemeUtil.setResIdThemeDark(R.style.AppThemeDark);
        ThemeUtil.set(this, settings.getTheme());

        initBuffers();
        initGUI();
    }


    private void initBuffers() {
        if(buffer != null) { return; } // already initialized

        // buffer will contain data of both channels (left, right)
        int floats = (int)(PLOT_BUFFER_MILLIS * settings.getSampleRate() * NUM_CHANNELS / 1000);
        buffer = new float[BUFFER_COUNT][floats];
        bufferIndex = 0;
        indexInBuffer = 0;
    }


    private void initGUI() {

        // we use our own title bar in "layout_main"
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        llMenuOptions = findViewById(R.id.ll_menu_options);
        findViewById(R.id.iv_menu).setOnClickListener(view -> {
            if(llMenuOptions.getVisibility() == View.VISIBLE) {
                llMenuOptions.setVisibility(View.GONE);
            } else if(llMenuOptions.getVisibility() == View.GONE) {
                llMenuOptions.setVisibility(View.VISIBLE);
            }
        });
        llMenuOptions.setVisibility(View.GONE);

        initPlots();

        GuiUtil.setOnClickListenerToAllButtons(llMenuOptions, view -> {
            llMenuOptions.setVisibility(View.GONE);
            int id = view.getId();
            if(id == R.id.btn_start) { onStartMic(); }
            if(id == R.id.btn_stop) { onStopMic(); }
            if(id == R.id.btn_about) {
                startActivity(new Intent(this, AboutActivity.class));
            }
            if(id == R.id.btn_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, RQ_CODE_SETTINGS);
            }
        });
    }


    @SuppressLint("ClickableViewAccessibility")
    private synchronized void initPlots() {

        LinearLayout llPlots = findViewById(R.id.ll_plots);
        llPlots.removeAllViews();

        LinearLayout.LayoutParams params;
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        params.weight = 1.0f;

        pvPlots = new PlotView[2];
        for(int i=0; i<pvPlots.length; i++) {
            PlotView plotView = new PlotView(this);
            pvPlots[i] = plotView;
            plotView.setBuffer(buffer[0]);
            plotView.setNumChannels(2);
            plotView.setChannel(i);
            plotView.setBackgroundColor(ColorUtil.getColorBackground(this));
            plotView.setColor(ColorUtil.getColorForeground(this));
            plotView.setLineWidth(3);
            if(i < pvPlots.length-1) {
                params.setMargins(0, 10, 0, 10);
            } else {
                params.setMargins(0, 10, 0, 0);
            }
            llPlots.addView(plotView, params);
            plotView.setOnTouchListener((v, event) -> {
                llMenuOptions.setVisibility(View.GONE);
                return false;
            });
        }
    }

    synchronized private void updatePlots() {
        for(PlotView plot : pvPlots) {

            // still will draw from old buffer
            plot.invalidate();

            // switch to new buffer:
            plot.setBuffer(buffer[bufferIndex]);
        }
    }

    void onMicSample(float f) {
        buffer[bufferIndex][indexInBuffer++] = f * 50;
        indexInBuffer %= buffer[0].length;

        if(indexInBuffer == 0) { // that buffer full

            // switch buffer:
            bufferIndex++;
            bufferIndex %= buffer.length;

            runOnUiThread(this::updatePlots);
        }
    }

    private void onStartMic() {
        if(micInput != null) { return; } // already started
        requestPermissions();
    }

    private void onStopMic() {
        if(micInput == null) { return; } // already stopped
        micInput.stop();
        while(!micInput.isStopped()) {
            try { //noinspection BusyWait
                Thread.sleep(5);
            } catch (Exception e) { /**/ }
        }
        micInput = null;
    }

    private void requestPermissions() {
        if(Build.VERSION.SDK_INT < 23) {
            onPermissionsGranted();
            return;
        }

        String[] permissions = { Manifest.permission.RECORD_AUDIO };

        for(String permission : permissions) {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 1);
                return;
            }
        }
        onPermissionsGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        int n = permissions.length;
        for(int i=0; i<n; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }
        onPermissionsGranted();
    }

    @SuppressLint("MissingPermission")
    private void onPermissionsGranted() {
        int audioSource = MediaRecorder.AudioSource.DEFAULT;
        if(android.os.Build.VERSION.SDK_INT >= 24) {
            audioSource = MediaRecorder.AudioSource.UNPROCESSED;
        }

        int sampleRate = (int) settings.getSampleRate();

        int chFormat = AudioFormat.CHANNEL_IN_STEREO;

        micInput = new MicInput(audioSource, chFormat, sampleRate);

        boolean micInitOK = micInput.init(this);
        if(!micInitOK) {
            micInput = null;
            return;
        }

        micInput.connectOutputTo(this::onMicSample);
        micInput.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) { return; }
        if(requestCode == RQ_CODE_SETTINGS) { recreate(); return; }
        // ...
    }

    @Override
    public void onBackPressed() {
        if(llMenuOptions.getVisibility() == View.VISIBLE) {
            llMenuOptions.setVisibility(View.GONE);
            return;
        }
        onStopMic();
        buffer = null;
        super.onBackPressed();
        super.finish();
    }
}