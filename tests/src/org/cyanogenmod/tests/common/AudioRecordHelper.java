package org.cyanogenmod.tests.common;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A wrapper on AudioRecord class.
 * Generously kanged from CTS.
 */
public class AudioRecordHelper {

    private static final int[] SAMPLE_RATES_HZ = {48000, 44100};

    private static final int CHANNEL = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final String TAG = "AudioRecordHelper";
    private static AudioRecordHelper instance;
    private final int bufferSize;
    private final int sampleRate;
    private int source;
    private ByteArrayOutputStream os;
    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;

    private AudioRecordHelper(int source) {
        int tmpBufferSize = 0;
        int tmpSampleRate = 0;
        int tmpSource = 0;
        initialization:
        for (int rate : SAMPLE_RATES_HZ) {
            tmpBufferSize = AudioRecord.getMinBufferSize(rate, CHANNEL, ENCODING);
            AudioRecord testAudioRecord = new AudioRecord(source, rate, CHANNEL, ENCODING,
                    tmpBufferSize);
            if (testAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                testAudioRecord.release();
                tmpSampleRate = rate;
                tmpSource = source;
                break initialization;
            }
        }
        if (tmpBufferSize == 0 || tmpSampleRate == 0) {
            Log.e(TAG, "Failed to initialize");
        }
        bufferSize = tmpBufferSize;
        sampleRate = tmpSampleRate;
        source = tmpSource;
        Log.d(TAG, "Sample rate = " + sampleRate + "Hz, Source = "
                + source + " (VOICE_RECOGNITION = 6 , MIC = 1)");
    }

    public static AudioRecordHelper getInstance(int audioSource) {
        if (instance == null) {
            instance = new AudioRecordHelper(audioSource);
        }
        return instance;
    }

    /**
     * Start recording.
     */
    public void start() {
        if (!isRecording) {
            isRecording = true;
            os = new ByteArrayOutputStream();
            audioRecord = new AudioRecord(source, sampleRate, CHANNEL, ENCODING, bufferSize);
            audioRecord.startRecording();
            startPullingData();
        }
    }

    /**
     * Stop recording
     */
    public void stop() {
        if (isRecording) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startPullingData() {
        new Thread() {
            @Override
            public void run(){
                byte data[] = new byte[bufferSize];
                while (isRecording) {
                    int read = audioRecord.read(data, 0, bufferSize);
                    if (read > 0) {
                        os.write(data, 0, read);
                    }
                    if (read < 0) {
                        break;
                    }
                }
            }
        }.start();
    }

    /**
     * Returns the sample rate for this recorder.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the audio source currently being used.
     */
    public int getAudioSource() {
        return source;
    }

    /**
     * Returns true if recorder is recording; False if not.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Returns the raw data.
     */
    public byte[] getByte() {
        return os.toByteArray();
    }
}
