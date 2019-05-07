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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


abstract public class BookBuilder {
    // BOOK BUILDING PARAMS
    protected final Instant creation = Instant.now();
    private final Locale locale;
    //    private final ArrayList<TranscriberResult> transcriberResults = new ArrayList<>();
    //    private Duration elapsed = Duration.ZERO;
//    private Transcriber transcriber;
    private volatile State state = State.READY;
    private ArrayList<Transcriber> transcribers = new ArrayList<>();

    public BookBuilder(Activity activity) {
        this(Utils.getCurrentLocale(activity));
    }

    public BookBuilder(Locale locale) {
        this.locale = locale;
    }

    private static Word shiftWord(Word word, Duration shift) {
        // TODO: INVESTIGATE USING A "DURATION" for WORD object INSTEAD OF "ENDTIME"
        // TODO: WOULD MAKE SHIFTING ONLY REQUIRE SHIFTING OF THE START TIME
        return new Word(
                word.word,
                word.startTime.plus(shift),
                word.endTime.plus(shift)
        );
    }

    private void actionCheckOpened(String action) {
        if (state == State.CLOSED)
            throw new RuntimeException("Can not " + action + " a closed Transcriber");
    }

    public Transcript buildTranscript() {
        final int transcriberCount = transcribers.size();
        if (transcriberCount == 0) return Transcript.EMPTY;
        else {

//            int i = 0;

            Duration totalDuration = Duration.ZERO;

            Word[] titleWords = new Word[0];
            // TODO: INVESTIGATE UNNECESSARY INITIALISATION OF TITLE WORDS;

            final ArrayList<Word> contentWords = new ArrayList<>();

            // TODO: don't append empty transcribers so we can trust first transcriber as the title
            // TODO: would require editing of audio to remove white noise
            boolean foundTitle = false;
            int i = 0;
            for (; i < transcriberCount; i++) {
                if (foundTitle) break;
                final Transcriber firstTranscriber = transcribers.get(i);
                final ApiResult[] results = firstTranscriber.getResults();

                int j = 0;
                for (; j < results.length; j++) {
                    final Word[] result = results[j].getWords();

                    if (foundTitle) {
                        addWords(contentWords, results, totalDuration, j);
                    } else if (result.length != 0) {
                        foundTitle = true;
                        titleWords = shiftWords(result, totalDuration);
                        i++;
                    }
                }
                totalDuration = totalDuration.plus(firstTranscriber.getDuration());
            }
            if (!foundTitle) return Transcript.EMPTY;


            for (; i < transcriberCount; i++) {
                // get current transcriber and increment
                Transcriber transcriber = transcribers.get(i);
                // accommodate word timing for previous transcriber durations
                ApiResult[] results = transcriber.getResults();
                addWords(contentWords, results, totalDuration, 0);
                totalDuration = totalDuration.plus(transcriber.getDuration());
            }

            return new Transcript(titleWords, contentWords.toArray(new Word[0]));
        }
    }

    private void addWords(List<Word> wordList, ApiResult[] results, Duration shift, int offset) {
        for (int j = offset; j < results.length; j++) {
            ApiResult apiResult = results[j];
            final Word[] shiftedWords = shiftWords(apiResult.getWords(), shift);
            Collections.addAll(wordList, shiftedWords);
        }
    }

    private Word[] shiftWords(Word[] words, Duration shift) {
        return Arrays.stream(words)
                .map(word -> shiftWord(word, shift))
                .toArray(Word[]::new);
    }

//    /**
//     * @return whether there's a transcriber transcribing
//     */
//    public boolean isRunning() {
//        return state == State.RUNNING;
//    }

    /**
     * @return whether this builder is closed
     */
    public boolean isClosed() {
        return state == State.CLOSED;
    }

    /**
     * Starts recording with a new transcriber
     *
     * @throws IllegalStateException if closed
     */
    public void start() throws IllegalStateException {
        actionCheckOpened("start");
        if (state == State.RUNNING) return;


        transcribers.add(new Transcriber(locale, AudioUtils.getMicRecorder()) {
            @Override
            protected void onResult(ApiResult apiResult) {
                onUpdate(buildTranscript());
            }

            @Override
            protected void onPartialResult(String transcript) {
                onPartial(transcript);
            }

            @Override
            public void onError(Throwable err) {
                BookBuilder.this.onError(err);
            }

            @Override
            protected void onClosing() {
//                if (state == State.RUNNING) {
                // TODO: ADVANCED STATE INDEPENDENT OF TRANSCRIBER COUNT
                // TODO: FIX SIDE-EFFECT: one might close after the next opens

//                    elapsed = buildDuration();
//                transcriberResults.add(transcriberResult);
                state = State.READY;
                onStopped();
                // TODO: refactor transcriber audio / word serialisation, will no longer concatenate together, but be dynamically merged
//                        break;
//
//                    case CLOSED:
//                        sendFinalBook();
//                }
            }

            @Override
            protected void onRead(byte[] read, int byteCount) throws IOException {
                BookBuilder.this.onRead(read, byteCount);
            }
        });
        state = State.RUNNING;
    }

    /**
     * Pauses transcription, asynchronously closing current transcriber
     * (if one exists)
     */
    public void pause() {
        actionCheckOpened("pause");

        transcribers.forEach(Transcriber::stop);
        state = State.READY;
    }

    /**
     * Fully stops / closes this transcriber asynchronously triggering
     * the final {@link com.tobi.voicebooks.models.Book} to be generated
     *
     * @return built book
     * @throws IllegalArgumentException when book title is empty
     * @see #pause()
     * @see APIListener#onClosing()
     */
    public Book close() throws Exception {
        actionCheckOpened("close");
        pause();
        state = State.CLOSED;
        return buildBook();
    }

    /**
     * @return the buildBook book
     * @throws IllegalArgumentException when book title is empty
     */
    private Book buildBook() throws IllegalArgumentException {
        return new Book(buildTranscript(), creation, buildDuration());
    }

    /**
     * @return current summation of elapsed time across all transcriptions
     */

    public Duration buildDuration() {
        return transcribers.stream()
                .map(Transcriber::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    abstract public void onUpdate(Transcript update);

    abstract public void onPartial(String partialResult);

    abstract public void onError(Throwable t);

    abstract public void onStopped();

    abstract public void onRead(byte[] read, int byteCount) throws IOException;

    enum State {
        READY,
        RUNNING,
        CLOSED
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

        abstract protected void onResult(ApiResult result);

        protected abstract void onPartialResult(String transcript);

        protected abstract void onError(Throwable error);

        protected abstract void onClosing();
    }
}
