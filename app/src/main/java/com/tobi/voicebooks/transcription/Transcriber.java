package com.tobi.voicebooks.transcription;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.tobi.voicebooks.models.Book;
import com.tobi.voicebooks.models.Transcript;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;


public class Transcriber implements AutoCloseable {
    private static final int MIC_SAMPLE_RATE = 16000; // Hz
    private static final int MIC_AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int MIC_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MIC_BUFFER_SIZE = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, MIC_AUDIO_CHANNELS, MIC_AUDIO_ENCODING);
    private static final OkHttpClient client = new OkHttpClient();
    private static final Request request = new Request.Builder().url("ws://voicebooks.herokuapp.com").build();

    private final AudioRecord audioSource;
    private final Locale locale;
    private final TranscriberBuilder.Listener listener;
    private WebSocket ws;
    private boolean stopped = false;
    private long sampleCount = 0;

    public Transcriber(Locale locale, AudioRecord audioSource, TranscriberBuilder.Listener listener) {
        this.audioSource = audioSource;
        this.locale = locale;

        this.listener = listener;
        ws = client.newWebSocket(request, listener);

        //Start recording
        this.audioSource.startRecording();

        //TODO: USE ASYNC TASK
        new Thread(this::streamToCloud).start();
    }

    /**
     * generates microphone audio source based on static final field
     *
     * @return new microphone AudioRecord
     */
    public static AudioRecord generateMicSource() {
        return new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(MIC_SAMPLE_RATE)
                        .setEncoding(MIC_AUDIO_ENCODING)
                        .setChannelMask(MIC_AUDIO_CHANNELS)
                        .build())
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                // unprocessed audio source because applying signal processing algorithms
                // such as noise reduction or gain control reduces recognition accuracy
                .setBufferSizeInBytes(MIC_BUFFER_SIZE)
                .build();
    }

    /**
     * Asynchronously - safely - closes transcriber.
     * Sets transcribing flag to false, signaling for API
     * streamer to stop
     *
     * @see #streamToCloud()
     * @see #close()
     */
    public void stop() {
        stopped = true;
    }

    public boolean isTranscribing() {
        return !stopped;
    }

    /**
     * Ran on a separate thread.
     * Implements a bidirectional websocket stream between android device and my own node.js server
     * - Sends device string locale
     * - then starts streaming unprocessed audio data
     * <p>
     * - server intermittently responds with results from the Google Speech to Text API
     * - results are processed by {@link TranscriberBuilder.Listener}
     *
     * @see TranscriberBuilder.Listener
     * @see OkHttpClient
     */
    private void streamToCloud() {
        //Send locale
        ws.send(locale.toString());

        // FileOutputStream fileStream = openFileOutput("test", Context.MODE_PRIVATE)
        try {
            byte[] data = new byte[MIC_BUFFER_SIZE];
            while (!stopped) {
                audioSource.read(data, 0, MIC_BUFFER_SIZE);
                ws.send(ByteString.of(data));
                sampleCount++;
//                listener.onRead(data);
            }
//            fileStream.flush();
        } catch (Exception e) {
            System.out.println("FAILED TO STREAM PACKET");
            listener.onError(e);
        }
// catch (IOException e) {
//            System.out.println("FAILED TO STREAM TO FILE");
//            listener.onError(e);
//        }
        try {
            close();
            listener.onClose();
        } catch (Exception e) {
            System.out.println("FAILED TO CLOSE TRANSCRIBER AND CREATE TRANSCRIPT");
            listener.onError(e);
        }
    }

    @Override
    public void close() {
        audioSource.release();
        ws.close(TranscriberBuilder.Listener.NORMAL_CLOSURE_STATUS, getClass().getName() + " closed");
    }

    /**
     * Used to obtain estimate for record time
     *
     * @return duration recorded
     */
    public Duration getDuration() {
        return Duration.ofSeconds(sampleCount / MIC_SAMPLE_RATE);
    }

    public interface Listener {
        void onPartial(String partialResult);

        void onUpdate(Transcript transcript);

        void onClose(Book book);

        void onError(Throwable t);

//        default void onRead(byte[] data) {
//        }
    }

}