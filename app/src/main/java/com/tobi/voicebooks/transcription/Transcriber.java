package com.tobi.voicebooks.transcription;

import android.media.AudioFormat;
import android.media.AudioRecord;

import com.tobi.voicebooks.Utils.AudioUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;

enum State {
    TRANSCRIBING,
    STOPPING,
    CLOSED
}

abstract public class Transcriber implements AutoCloseable {
    public static final int MIC_SAMPLE_RATE = 16000; // Hz
    public static final int MIC_AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int MIC_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat.Builder()
            .setSampleRate(MIC_SAMPLE_RATE)
            .setEncoding(MIC_AUDIO_ENCODING)
            .setChannelMask(MIC_AUDIO_CHANNELS)
            .build();
    public static final int AUDIO_BYTE_RATE = AudioUtils.getByteRate(AUDIO_FORMAT);
    public static final int MIC_BUFFER_SIZE = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, MIC_AUDIO_CHANNELS, MIC_AUDIO_ENCODING);
    private static final OkHttpClient client = new OkHttpClient();
    private static final Request request = new Request.Builder().url("ws://voicebooks.herokuapp.com").build();

    private final AudioRecord audioSource;
    private final Locale locale;
    private final WebSocket ws;
    private final ArrayList<ApiResult> apiResults = new ArrayList<>();
    private volatile long totalRead = 0;
    /**
     * Marked as volatile so changes to this field
     * are respected across threads
     */
    private volatile State state = State.TRANSCRIBING;

    public Transcriber(Locale locale, AudioRecord audioSource) {
        this.audioSource = audioSource;
        this.locale = locale;

        ws = client.newWebSocket(request, new BookBuilder.APIListener() {
            @Override
            protected void onResult(ApiResult result) {
                apiResults.add(result);
                Transcriber.this.onResult(result);
            }

            @Override
            protected void onPartialResult(String transcript) {
                Transcriber.this.onPartialResult(transcript);
            }

            @Override
            public void onError(Throwable error) {
                Transcriber.this.onError(error);
            }

            @Override
            protected void onClosing() {
                close();
            }
        });

        //Start recording
        audioSource.startRecording();

        //TODO: USE ASYNC TASK
        new Thread(this::streamToCloud).start();
    }

    abstract protected void onResult(ApiResult apiResult);

    abstract protected void onPartialResult(String transcript);

    abstract protected void onError(Throwable err);

    @Override
    public void close() {
        // Early exit if already closed to remain idempotent
        if (state == State.CLOSED) return;
        state = State.CLOSED;
        audioSource.release();
        ws.close(BookBuilder.APIListener.NORMAL_CLOSURE_STATUS, getClass().getName() + " closed");
        onClosing();
    }

    /**
     * Used to obtain estimate for record time
     *
     * @return duration recorded
     */
    public Duration getDuration() {
        return Duration.ofSeconds(totalRead / AUDIO_BYTE_RATE);
    }

    abstract protected void onClosing();

    /**
     * Asynchronously - safely - closes transcriber.
     * Sets transcribing flag to false, signaling for API
     * streamer to stop
     * <p>
     * {@link #close()} will EVENTUALLY be called
     *
     * @see #streamToCloud()
     * @see #close()
     */
    public void stop() {
        state = State.STOPPING;
    }

    public ApiResult[] getResults() {
        return apiResults.toArray(new ApiResult[0]);
    }

    /**
     * To be ran on a separate thread.
     * Implements a bidirectional websocket stream between android device and my own node.js server
     * - Sends device string locale
     * - then starts streaming unprocessed audio data
     * <p>
     * - server intermittently responds with results from the Google Speech to Text API
     * - results are processed by {@link BookBuilder.APIListener}
     *
     * @see BookBuilder.APIListener
     * @see OkHttpClient
     */
    private void streamToCloud() {
        try {
            //Send locale to my web server
            ws.send(locale.toString());

            byte[] data = new byte[MIC_BUFFER_SIZE];

            // loop *ON THIS THREAD* while transcriber is running
            while (state == State.TRANSCRIBING) {
                // reads microphone data
                final int read = audioSource.read(data, 0, MIC_BUFFER_SIZE);
                // and then relays data though my intermediary server,
                // via a websocket, to the Google Speech API.
                ws.send(ByteString.of(data, 0, read));

                // count of bytes read incremented
                // used to calculated duration
                totalRead += read;

                onRead(data, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
            onError(e);
        } finally {
            // Close after either transcription stopped or
            // an error occurred.
            close();
        }
    }

    /**
     * @param read      microphone input
     * @param byteCount
     */
    abstract protected void onRead(byte[] read, int byteCount) throws IOException;
}