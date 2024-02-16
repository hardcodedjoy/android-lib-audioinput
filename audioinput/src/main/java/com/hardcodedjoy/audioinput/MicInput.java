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

package com.hardcodedjoy.audioinput;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;

public class MicInput {

    private AudioRecord ar;
    private Thread t;
    private boolean tStop;
    private boolean paused;

    private int encoding;

    private final int audioSource;
    private final int chFormat;
    private final int numChannels;
    private final int sampleRate;

    private AudioCable output;

    public MicInput(int audioSource, int chFormat, int sampleRate) {
        this.audioSource = audioSource;
        this.chFormat = chFormat;

        if(this.chFormat == AudioFormat.CHANNEL_IN_MONO) {
            this.numChannels = 1;
        } else if(this.chFormat == AudioFormat.CHANNEL_IN_STEREO) {
            this.numChannels = 2;
        } else {
            this.numChannels = 0;
        }

        this.sampleRate = sampleRate;
    }

    public int getAudioSource() { return audioSource; }
    public int getChFormat()    { return chFormat;    }
    public int getNumChannels() { return numChannels; }
    public int getSampleRate()  { return sampleRate;  }

    public boolean init(Context context) {

        if(Build.VERSION.SDK_INT >= 23) {
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                new Exception("permission RECORD_AUDIO not granted").printStackTrace(System.err);
                return false;
            }
        }

        encoding = AudioFormat.ENCODING_PCM_16BIT;

        int minBufSize;
        int bufferSize;
        int state;

        minBufSize = getMinBufSizeInBytes();
        bufferSize = minBufSize*256;

        // create AudioRecord with biggest accepted buffer size
        // (max. minBufSize*256)

        while (true) {

            ar = null;

            try {
                ar = new AudioRecord(audioSource, sampleRate, chFormat, encoding, bufferSize);
                // increase buffer size if overrun

                state = ar.getState();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                state = AudioRecord.STATE_UNINITIALIZED;
            }

            if(state == AudioRecord.STATE_INITIALIZED) { break; } // ar init OK

            // else -> bufferSize too big

            if(ar != null) { ar.release(); ar = null; }

            bufferSize /= 2;
            if(bufferSize < minBufSize) {
                new Exception("AudioRecord init ERR").printStackTrace(System.err);
                break; // ar init ERR
            }
        }

        return (ar != null); // true -> init OK, false -> init failed
    }


    public void connectOutputTo(AudioCable cable) { this.output = cable; }

    public void start() {
        if(ar == null) { return; }

        //noinspection FieldCanBeLocal
        t = new Thread() {

            private int n;
            private int i;
            private int ch;
            private final float[] sample = new float[numChannels];
            private int read;
            private short[] buf;

            public void run() {

                n = getMinBufSizeInBytes()/2; // 1 short = 2 bytes
                buf = new short[n];

                ar.startRecording();

                tStop = !(ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);

                while(!tStop) {

                    read = ar.read(buf, 0, n); // read max n shorts

                    if(read == 0) {
                        // new audio data not available yet
                        try {
                            //noinspection BusyWait
                            Thread.sleep(0, 250);
                        } catch (Exception e) { /**/ }
                    }

                    if(paused) { continue; }
                    if(output == null) { continue; }

                    try {
                        for(i=0; i<read; i++) {
                            sample[ch++] = buf[i]/32768.0f;
                            if(ch >= numChannels) {
                                ch = 0;
                                output.send(sample);
                            }
                        }
                        output.endOfFrame(); // indicator for receiver to start processing

                    } catch (Exception e) { // something wrong with the receiver of the output
                        e.printStackTrace(System.err);
                        outputEndOfStream();
                        break;
                    }
                }

                ar.stop();
                ar.release();
                ar = null;
                t = null;

                outputEndOfStream();
            }
        };

        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private void outputEndOfStream() {
        try { output.endOfStream(); }
        catch (Exception e) { e.printStackTrace(System.err); }
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }
    public boolean isPaused() { return paused; }

    public void stop() { paused = false; tStop = true; }

    public boolean isStopped() { return (t == null); }

    private int getMinBufSizeInBytes() {
        return AudioRecord.getMinBufferSize(sampleRate, chFormat, encoding);
    }
}