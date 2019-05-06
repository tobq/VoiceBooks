package com.tobi.voicebooks.transcription;

import android.app.Activity;

import com.tobi.voicebooks.Utils.AudioUtils;
import com.tobi.voicebooks.Utils.Utils;
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
import java.util.Locale;

import javax.annotation.Nullable;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


abstract public class BookBuilder implements AutoCloseable {
    enum State {
        READY,
        RECORDING,
        CLOSED
    }
    // BOOK BUILDING PARAMS
    protected final Instant creation = Instant.now();
    protected final ArrayList<ApiResult> apiResults = new ArrayList<>();

    private final Locale locale;
    //    private Duration elapsed = Duration.ZERO;
//    private Transcriber transcriber;
    private State state = State.READY;
    private ArrayList<Transcriber> transcribers = new ArrayList<>();

    public BookBuilder(Activity activity) {
        this(Utils.getCurrentLocale(activity));
    }

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

        transcribers.add(new Transcriber(locale, AudioUtils.generateMicSource()) {
            @Override
            protected void onResult(ApiResult apiResult) {
                apiResults.add(apiResult);
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
            protected void onClosing(TranscriberResult transcriberResult) {
//                if (state == State.RECORDING) {
                // TODO: ADVANCED STATE INDEPENDENT OF TRANSCRIBER COUNT
                // TODO: FIX SIDE-EFFECT: one might close after the next opens

//                    elapsed = getElapsed();
                state = State.READY;
//                        break;
//
//                    case CLOSED:
//                        sendFinalBook();
//                }
            }

            @Override
            protected void onRead(byte[] read, int byteCount) {
                try {
                    BookBuilder.this.onRead(read, byteCount);
                } catch (IOException e) {
                    onError(e);
                }
            }
        });
        state = State.RECORDING;
    }

    abstract public void onUpdate(Transcript update);

    public Transcript buildTranscript() {
        final int apiResultCount = apiResults.size();
        if (apiResultCount == 0) return new Transcript(new Word[0], new Word[0]);
        else {
            final ApiResult titleResult = apiResults.get(0);
            final Word[] titleWords = titleResult.getWords();

            Duration totalDuration = titleResult.getDuration();
            final ArrayList<Word> contentWords = new ArrayList<>();
            for (int i = 1; i < apiResultCount; i++) {
                ApiResult apiResult = apiResults.get(i);

                for (Word word : apiResult.getWords()) {
                    Word compensatedWord = new Word(
                            word.word,
                            word.startTime.plus(totalDuration),
                            word.endTime.plus(totalDuration)
                    );
                    contentWords.add(compensatedWord);
                }
                totalDuration = totalDuration.plus(apiResult.getDuration());
            }

            return new Transcript(titleWords, contentWords.toArray(new Word[0]));
        }
    }

    abstract public void onPartial(String partialResult);

    abstract public void onError(Throwable t);

    abstract public void onRead(byte[] read, int byteCount) throws IOException;

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
        onClose(buildBook());
    }

    /**
     * Pauses transcription, asynchronously closing current transcriber
     * (if one exists)
     */
    public void pause() {
        transcribers.forEach(Transcriber::stop);
        state = State.READY;
    }

    abstract public void onClose(Book result);

    /**
     * @return the buildBook book
     * @throws IllegalArgumentException when book title is empty
     */
    public Book buildBook() throws IllegalArgumentException {
        return new Book(buildTranscript(), creation, getElapsed());
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
         * @return duration of word
         * @throws JSONException if seconds/nanos not found on object
         */
        private static Duration processAPITime(JSONObject time) throws JSONException {
            long seconds = Long.parseLong(time.getString("seconds"));
            long nanos = Long.parseLong(time.getString("nanos"));

            return Duration.ofSeconds(seconds).plusNanos(nanos);
        }

        abstract protected void onResult(ApiResult result);

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
