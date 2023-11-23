# AudioInput

<code>com.hardcodedjoy.audioinput</code> <code>v1.0.0</code><br/>
minSdkVersion: <code>8</code><br/>
targetSdkVersion: <code>33</code><br/>

## Short description

Low-level Audio Input library for Android.


## Description

This is a low-level Audio Input library for Android. It can be used to read RAW data from the microphone. The resulting data is provided as individual float values in the range +/- 1.0 (0.0 = silence, +/-1.0 = super loud sound). Each float value corresponds to one sample, so a sample of 44100 Hz will result in 44100 floats per second that you receive in your app and have to process. That is for MONO mode. If you open the MIC in STEREO mode, that number will be 88200. For other sample rates, the number of received values changes accordingly.

The MIC has high dinamic range, so for usual sound like speech, values will probably never reach amplitude of 1.0, not even 0.5, so you may want to amplify. Amplifying is simply multiplying each float value with a constant.


## Links

developer website: [https://hardcodedjoy.com](https://hardcodedjoy.com)<br/>

