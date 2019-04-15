package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.models.Word;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Locale;

import javax.annotation.Nullable;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

enum State {
    READY,
    STARTED,
    CLOSED
}

public class TranscriberBuilder implements AutoCloseable {
    private final Locale locale;
    private final Transcriber.Listener listener;
    private final BookBuilder bookBuilder = new BookBuilder();
    private Duration elapsed = Duration.ZERO;
    private Transcriber transcriber;
    private State state = State.READY;

    public TranscriberBuilder(Locale locale, Transcriber.Listener listener) {
        this.listener = listener;
        this.locale = locale;
    }

    /**
     * Starts recording with a new transcriber
     *
     * @throws Exception if previous transcriber fails to close
     */

    public void start() throws Exception {
        if (stopped) throw new IllegalStateException("Can not start a stopped transcriber");
        if (transcriber != null) transcriber.close();
        transcriber = new Transcriber(locale, Transcriber.generateMicSource(), new Listener() {
            @Override
            protected void onResult(ApiResult apiResult) {
                bookBuilder.append(apiResult);
                listener.onUpdate(bookBuilder.buildTranscript());
            }

            @Override
            protected void onPartialResult(String transcript) {
                listener.onPartial(transcript);
            }

            @Override
            public void onError(Throwable err) {
                listener.onError(err);
            }

            @Override
            protected void onClose() {
                if (transcriber != null) {
                    synchronized (transcriber) {
                        elapsed = getElapsed();
                        try {
                            transcriber.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        transcriber = null;
                    }
                }
                if (state == State.CLOSED) sendFinalBook();
            }
        });
    }

    /**
     * @return current summation of elapsed time across all transcriptions
     */

    public Duration getElapsed() {
        return transcriber == null ? elapsed : elapsed.plus(transcriber.getDuration());
    }

    /**
     * Sends the finally built book to the passed in listener
     */
    private void sendFinalBook() {
        listener.onClose(bookBuilder.build(elapsed));
    }

    /**
     * Fully stops / closes this transcriber asynchronously triggering
     * the final {@Book} to be generated
     *
     * @throws NullPointerException when book title is empty
     * @see #pause()
     * @see Listener#onClose()
     */
    public void stop() throws NullPointerException {
        stopped = true;
        if (!pause()) sendFinalBook();
    }

    /**
     * Pauses transcription, asynchronously closing current transcriber
     * (if one exists)
     *
     * @return whether there was a transcriber
     */
    public boolean pause() {
        if (transcriber == null) return false;
        else {
            transcriber.stop();
            return true;
        }
    }

    /**
     * @return where there's a transcriber transcribing
     */
    public boolean isTranscribing() {
        return transcriber != null && transcriber.isTranscribing();
    }

    @Override
    public void close() throws Exception {
        if (transcriber != null) transcriber.close();
    }

    /**
     * Listens to results from the API
     * being relayed through a {@link WebSocket}
     */
    abstract class Listener extends WebSocketListener {
        static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject result = new JSONObject(text).getJSONArray("results").getJSONObject(0);
                JSONObject alternative = result.getJSONArray("alternatives").getJSONObject(0);
                JSONArray JSONWords = alternative.getJSONArray("words");
                String transcriptText = alternative.getString("transcript");

                int wordCount = JSONWords.length();
                if (result.getBoolean("isFinal")) {
                    final Word[] words = new Word[wordCount];
                    for (int i = 0; i < wordCount; i++) {
                        JSONObject JSONWord = JSONWords.getJSONObject(i);
                        Duration startTime = processAPITime(JSONWord.getJSONObject("startTime"));
                        Duration endTime = processAPITime(JSONWord.getJSONObject("endTime"));
                        Word word = new Word(JSONWord.getString("word"), startTime, endTime);
                        words[i] = word;
                    }

                    onResult(new ApiResult(transcriptText, words));
                } else {
                    onPartialResult(transcriptText);
                }
            } catch (JSONException | IllegalArgumentException e) {
                webSocket.cancel();
            }
        }

        /**
         * Processes the times returned by Google's Voice API
         * Adjusts the start / end times with the current elapsed time
         * of the Transcriber Builder
         *
         * @param time JSON time
         * @return
         * @throws JSONException if seconds/nanos not found on object
         */
        private Duration processAPITime(JSONObject time) throws JSONException {
            long seconds = Long.parseLong(time.getString("seconds"));
            long nanos = Long.parseLong(time.getString("nanos"));

            return elapsed.plusSeconds(seconds).plusNanos(nanos);
        }

        protected abstract void onResult(ApiResult apiResult);

        protected abstract void onPartialResult(String transcript);

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            onClose();
            // TODO: Implementation of maxed out transcription duration
            // doesn't closed view
            // after pipe is broken
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            onError(t);
        }

        public abstract void onError(Throwable error);

        protected abstract void onClose();
    }
}
