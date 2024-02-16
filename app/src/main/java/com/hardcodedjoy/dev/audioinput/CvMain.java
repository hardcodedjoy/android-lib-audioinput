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

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.LinearLayout;

import com.hardcodedjoy.appbase.activity.PermissionUtil;
import com.hardcodedjoy.appbase.contentview.ContentView;
import com.hardcodedjoy.appbase.contentview.CvTMLL;
import com.hardcodedjoy.appbase.gui.ThemeUtil;
import com.hardcodedjoy.appbase.popup.Option;
import com.hardcodedjoy.audioinput.AudioCable;
import com.hardcodedjoy.audioinput.MicInput;

import java.util.Vector;

@SuppressLint("ViewConstructor")
public class CvMain extends CvTMLL {

    static private final int BUFFER_COUNT = 2;
    static private final int NUM_CHANNELS = 2;
    static private final int PLOT_BUFFER_MILLIS = 100;

    private final Settings settings;

    private MicInput micInput;
    private float[][] buffer;
    private int bufferIndex;
    private int indexInBuffer;
    private PlotView[] pvPlots;

    public CvMain() {
        // add initialization code here (that must run only one time)

        settings = (Settings) ContentView.settings;
        initBuffers();

        Vector<Option> ops = new Vector<>();
        ops.add(new Option(R.drawable.ic_media_rec_2, getString(R.string.start), this::onStartMic));
        ops.add(new Option(R.drawable.ic_media_stop_2, getString(R.string.stop), this::onStopMic));
        addMenuOptions(ops, 0);

        initPlots();
    }

    private void initBuffers() {
        if(buffer != null) { return; } // already initialized

        // buffer will contain data of both channels (left, right)
        int floats = (int)(PLOT_BUFFER_MILLIS * settings.getSampleRate() * NUM_CHANNELS / 1000);
        buffer = new float[BUFFER_COUNT][floats];
        bufferIndex = 0;
        indexInBuffer = 0;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initPlots() {

        LinearLayout llPlots = findViewById(R.id.appbase_ll_content);
        llPlots.removeAllViews();
        llPlots.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params;
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        params.weight = 1.0f;

        pvPlots = new PlotView[2];
        for(int i=0; i<pvPlots.length; i++) {
            PlotView plotView = new PlotView(getActivity());
            pvPlots[i] = plotView;
            plotView.setBuffer(buffer[0]);
            plotView.setNumChannels(2);
            plotView.setChannel(i);

            plotView.setBackgroundColor(ThemeUtil.getColor(
                    getActivity(), android.R.attr.colorBackground));

            plotView.setColor(ThemeUtil.getColor(
                    getActivity(), android.R.attr.colorForeground));

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

    private void onStartMic() {
        if(micInput != null) { return; } // already started

        PermissionUtil.runWithPermission(Manifest.permission.RECORD_AUDIO, () -> {
            int audioSource = MediaRecorder.AudioSource.DEFAULT;
            if(android.os.Build.VERSION.SDK_INT >= 24) {
                audioSource = MediaRecorder.AudioSource.UNPROCESSED;
            }

            int sampleRate = (int) settings.getSampleRate();

            int chFormat = AudioFormat.CHANNEL_IN_STEREO;

            micInput = new MicInput(audioSource, chFormat, sampleRate);

            boolean micInitOK = micInput.init(getActivity());
            if(!micInitOK) {
                micInput = null;
                return;
            }

            micInput.connectOutputTo(new AudioCable() {
                @Override
                public void send(float[] sample) { onMicSample(sample); }
                @Override
                public void endOfFrame() {}
                @Override
                public void endOfStream() {}
            });
            micInput.start();
        });
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

    private void onMicSample(float[] sample) {
        for(float f : sample) {
            buffer[bufferIndex][indexInBuffer++] = f * 50;
        }
        indexInBuffer %= buffer[0].length;

        if(indexInBuffer == 0) { // that buffer full

            // switch buffer:
            bufferIndex++;
            bufferIndex %= buffer.length;

            runOnUiThread(this::updatePlots);
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



    @Override
    public boolean onBackPressed() {
        //IntentUtil.stopService(PlayerService.class);
        return false; // not consumed -> app will close
    }
}
