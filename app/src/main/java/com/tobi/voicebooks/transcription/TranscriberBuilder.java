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
    RECORDING,
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
     * @throws IllegalStateException if closed
     */

    public void start() throws IllegalStateException {
        switch (state) {
            case CLOSED:
                throw new IllegalStateException("Can not start a stopped transcriber");
            case RECORDING:
                return;
        }

        transcriber = new Transcriber(locale, Transcriber.generateMicSource(), new APIListener(elapsed) {
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
                switch (state) {
                    case RECORDING:
                        elapsed = getElapsed();
                        try {
                            transcriber.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        state = State.READY;
                        break;

                    case CLOSED:
                        sendFinalBook();
                }
            }
        });
        state = State.RECORDING;
    }

    /**
     * @return current summation of elapsed time across all transcriptions
     */

    public Duration getElapsed() {
        System.out.println(transcriber.getDuration());
        return state == State.RECORDING ? elapsed.plus(transcriber.getDuration()) : elapsed;
    }

    /**
     * Sends the finally built book to the passed in listener
     *
     * @throws IllegalArgumentException when book title is empty
     */
    private void sendFinalBook() throws IllegalArgumentException {
        listener.onClose(bookBuilder.build(getElapsed()));
    }

    /**
     * Fully stops / closes this transcriber asynchronously triggering
     * the final {@link com.tobi.voicebooks.models.Book} to be generated
     *
     * @throws IllegalArgumentException when book title is empty
     * @see #pause()
     * @see APIListener#onClose()
     */

    @Override
    public void close() throws IllegalArgumentException {
        if (state == State.RECORDING) pause();
        state = State.CLOSED;
        sendFinalBook();
    }

    /**
     * Pauses transcription, asynchronously closing current transcriber
     * (if one exists)
     */
    public void pause() {
        transcriber.stop();
        state = State.READY;
    }

    /**
     * @return where there's a transcriber transcribing
     */
    public boolean isTranscribing() {
        return state == State.RECORDING;
    }

    /**
     * Listens to results from the API
     * being relayed through a {@link WebSocket}
     */
    abstract static class APIListener extends WebSocketListener {
        static final int NORMAL_CLOSURE_STATUS = 1000;
        private final Duration startTime;

        protected APIListener(Duration startTime) {
            this.startTime = startTime;
        }

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

            return startTime.plusSeconds(seconds).plusNanos(nanos);
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
