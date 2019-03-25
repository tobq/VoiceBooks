package com.tobi.voicebooks.transcription;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.tobi.voicebooks.models.FinalResult;
import com.tobi.voicebooks.models.Result;
import com.tobi.voicebooks.models.Word;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class Transcriber implements AutoCloseable {
    private static final int MIC_SAMPLE_RATE = 16000; // Hz
    private static final int MIC_AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int MIC_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MIC_BUFFER_SIZE = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, MIC_AUDIO_CHANNELS, MIC_AUDIO_ENCODING);
    private static final OkHttpClient client = new OkHttpClient();
    private static final Request request = new Request.Builder().url("ws://voicebooks.herokuapp.com").build();

    private final Listener listener;
    private final AudioRecord source;
    private final Locale locale;
    private WebSocket ws;
    private Book.Builder builder = new Book.Builder();
    private boolean transcribing = true;


    private Transcriber(AudioRecord source, Listener listener, Locale locale) {
        this.source = source;
        this.listener = listener;
        this.locale = locale;

        //Start recording
        this.source.startRecording();
        ws = client.newWebSocket(request, new APIListener());
        new Thread(this::streamToCloud).start();

        //TODO: USE ASYNC TASK
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

    private static Date processAPITime(JSONObject time) throws JSONException {
        long seconds = Long.parseLong(time.getString("seconds"));
        long nanos = Long.parseLong(time.getString("nanos"));
        return new Date(seconds * 1000 + nanos * 1000000);
    }

    /**
     * Asynchronously closes transcriber.
     * Sets transcribing flag to false, signaling for API
     * streamer to stop
     *
     * @see #streamToCloud()
     * @see #close()
     */
    public void stop() {
        transcribing = false;
    }

    public boolean isTranscribing() {
        return transcribing;
    }

    /**
     * Ran on a separate thread.
     * Implements a bidirectional websocket stream between android device and my own node.js server
     * - Sends device string locale
     * - then starts streaming unprocessed audio data
     * <p>
     * - server intermittently responds with results from the Google Speech to Text API
     * - results are processed by {@link APIListener}
     *
     * @see APIListener
     * @see OkHttpClient
     */
    private void streamToCloud() {
        //Send locale
        ws.send(locale.toString());

        byte[] data = new byte[MIC_BUFFER_SIZE];

// FileOutputStream fileStream = openFileOutput("test", Context.MODE_PRIVATE)
        try {
            while (transcribing) {
                source.read(data, 0, MIC_BUFFER_SIZE);
                ws.send(ByteString.of(data));
                listener.onRead(data);
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
            listener.onClose(builder.build());
        } catch (Exception e) {
            System.out.println("FAILED TO CLOSE TRANSCRIBER AND CREATE TRANSCRIPT");
            listener.onError(e);
        }
//        if (apiListener.getTranscript().)
    }

    @Override
    public void close() throws Exception {
        source.release();
        ws.close(APIListener.NORMAL_CLOSURE_STATUS, getClass().getName() + " closed");
    }

    public interface Listener {
        void onResult(Book book);

        void onClose(Book book);

        void onError(Throwable t);

        default void onRead(byte[] data) {
        }
    }

    public static class Builder implements AutoCloseable {
        private final AudioRecord source;
        private final Locale locale;
        private final Listener listener;
        private Transcriber transcriber;

        public Builder(AudioRecord source, Locale locale, Listener listener) {
            this.source = source;
            this.listener = listener;
            this.locale = locale;
        }

        public void start() throws Exception {
            if (transcriber != null) transcriber.close();
            transcriber = new Transcriber(source, listener, locale);
        }

        /**
         * Closes transcriber
         */
        public void stop() {
            if (transcriber != null) transcriber.stop();
        }

        public boolean isTranscribing() {
            return transcriber != null && transcriber.isTranscribing();
        }

        @Override
        public void close() throws Exception {
            if (transcriber != null) transcriber.close();
        }


        public Duration getEstimateRecordDuration() {
            return transcriber.builder.getEstimateRecordDuration();
        }
    }

    public final class APIListener extends WebSocketListener {
        static final int NORMAL_CLOSURE_STATUS = 1000;
        static final int JSON_EXCEPTION = 1001;

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject result = new JSONObject(text).getJSONArray("results").getJSONObject(0);
                JSONObject alternative = result.getJSONArray("alternatives").getJSONObject(0);
                String transcriptText = alternative.getString("transcript");

                // Prematurely return if the result is empty
                if (transcriptText.isEmpty()) return;

                JSONArray JSONWords = alternative.getJSONArray("words");
                Result sentence;
                int wordCount = JSONWords.length();
                Word[] words = new Word[wordCount];
                if (result.getBoolean("isFinal")) {
                    for (int i = 0; i < wordCount; i++) {
                        JSONObject JSONWord = JSONWords.getJSONObject(i);
                        Date startTime = processAPITime(JSONWord.getJSONObject("startTime"));
                        Date endTime = processAPITime(JSONWord.getJSONObject("endTime"));
                        Word word = new Word(JSONWord.getString("word"), startTime, endTime);
//                        System.out.println(word);
                        words[i] = word;
                    }
                    sentence = new FinalResult(transcriptText, words);
                } else {
                    sentence = new Result(transcriptText);
                }
                builder.set(sentence);

//                System.out.println(builder);
                listener.onResult(builder.build());
            } catch (JSONException | IllegalArgumentException e) {
                listener.onError(e);
                webSocket.close(JSON_EXCEPTION, "Error parsing API results");
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            stop();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            listener.onError(t);
        }

    }
}