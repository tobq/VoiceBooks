package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.models.Book;
import com.tobi.voicebooks.models.Transcript;
import com.tobi.voicebooks.models.Word;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

abstract public class BookBuilder implements AutoCloseable {
    private final Locale locale;

    // BOOK BUILDING PARAMS
    private final Instant creation = Instant.now();
    private final ArrayList<Word> titleWords = new ArrayList<>();
    private final ArrayList<Word> bookWords = new ArrayList<>();
    private boolean buildingTitle = true;
    private Duration elapsed = Duration.ZERO;


    //    private Duration elapsed = Duration.ZERO;
//    private Transcriber transcriber;
    private State state = State.READY;
    private ArrayList<Transcriber> transcribers = new ArrayList<>();

    public BookBuilder(Locale locale) {
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

        transcribers.add(new Transcriber(locale, Transcriber.generateMicSource()) {
            @Override
            protected void onResult(ApiResult apiResult) {
                appendResult(apiResult);
                onUpdate(buildTranscript());
            }

            @Override
            protected void onPartialResult(String transcript) {
                onPartial(transcript);
            }

            @Override
            public void onError(Throwable err) {
                close();
                BookBuilder.this.onError(err);
            }

            @Override
            protected void onClosing() {
//                if (state == State.RECORDING) {
                // TODO: ADVANCED STATE INDEPENDENT OF TRANSCRIBER COUNT
                // TODO: FIX SIDE-EFFECT: one might close after the next opens

//                    elapsed = getElapsed();
                close();
                state = State.READY;
//                        break;
//
//                    case CLOSED:
//                        sendFinalBook();
//                }
            }

            @Override
            protected void onRead(byte[] read) {
                try {
                    BookBuilder.this.onRead(read);
                } catch (IOException e) {
                    onError(e);
                }
            }
        });
        state = State.RECORDING;
    }

    public void appendResult(ApiResult result) {
        Word[] words = result.getWords();
        if (buildingTitle) Collections.addAll(titleWords, words);
        else Collections.addAll(bookWords, words);
        elapsed = elapsed.plus(result.getDuration());
        buildingTitle = false;
    }

    abstract public void onUpdate(Transcript update);

    public Transcript buildTranscript() {
        return new Transcript(titleWords.toArray(new Word[0]), bookWords.toArray(new Word[0]));
    }

    abstract public void onPartial(String partialResult);

    abstract public void onError(Throwable t);

    abstract public void onRead(byte[] read) throws IOException;

    /**
     * Fully stops / closes this transcriber asynchronously triggering
     * the final {@link com.tobi.voicebooks.models.Book} to be generated
     *
     * @throws IllegalArgumentException when book title is empty
     * @see #pause()
     * @see APIListener#onClosing()
     */

    @Override
    public void close() throws Exception {
        if (state == State.RECORDING) pause();
        state = State.CLOSED;
        sendFinalBook();
    }

    /**
     * Pauses transcription, asynchronously closing current transcriber
     * (if one exists)
     */
    public void pause() {
        transcribers.forEach(Transcriber::stop);
        state = State.READY;
    }

    /**
     * Sends the finally built book to the passed in listener
     *
     * @throws IllegalArgumentException when book title is empty
     */
    private void sendFinalBook() throws IllegalArgumentException {
        onClose(buildBook());
    }

    abstract public void onClose(Book result);

    /**
     * @return the buildBook book
     * @throws IllegalArgumentException when book title is empty
     */
    public Book buildBook() throws IllegalArgumentException {
        return new Book(buildTranscript(), creation, elapsed);
    }

    /**
     * @return current summation of elapsed time across all transcriptions
     */

    public Duration getElapsed() {
        return transcribers.stream()
                .map(Transcriber::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
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
//        private final Duration startTime;
//
//        protected APIListener(Duration startTime) {
//            this.startTime = startTime;
//        }

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

                    onResult(transcriptText, words);
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
         * @return duration of word
         * @throws JSONException if seconds/nanos not found on object
         */
        private static Duration processAPITime(JSONObject time) throws JSONException {
            long seconds = Long.parseLong(time.getString("seconds"));
            long nanos = Long.parseLong(time.getString("nanos"));

            return Duration.ofSeconds(seconds).plusNanos(nanos);
        }

        abstract protected void onResult(String transcriptText, Word[] words);

        protected abstract void onPartialResult(String transcript);

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            onClosing();
            // TODO: Implementation of maxed out transcription duration
            // doesn't closed view
            // after pipe is broken
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            onError(t);
        }

        protected abstract void onError(Throwable error);

        protected abstract void onClosing();
    }
}
