package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.Utils;
import com.tobi.voicebooks.models.FinalResult;
import com.tobi.voicebooks.models.Result;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;

public class Book {
    public final Result[] transcript;
    public final Date creation;
    private final Result title;
    private final Duration length;

    public Book(Result title, Date creation, Result[] transcript, Duration length) throws IllegalArgumentException {
        Objects.requireNonNull(title);
        this.title = title;
        this.creation = creation;
        this.transcript = transcript;
        this.length = length;
    }

    public Duration getLength() {
        return length;
    }

    public Result getTitle() {
        return title;
    }

    @NonNull
    @Override
    public String toString() {
        return Utils.join(" ", transcript);
    }

    public static class Builder {
        private final ArrayList<Result> results = new ArrayList<>();
        private Date recordDate = new Date();
        private Result title;
        private boolean titleSet = false;
        private boolean editingLastSentence = false;
        private int sentenceCount = 0;
        private Temporal estimateRecordStart = Instant.now();

        public void setRecordDate(Date recordDate) {
            this.recordDate = recordDate;
        }

        public void set(Result sentence) {
            if (titleSet) {
                if (editingLastSentence) results.set(sentenceCount, sentence);
                else {
                    results.add(sentence);
                    editingLastSentence = true;
                }
                if (sentence instanceof FinalResult) {
                    sentenceCount++;
                    editingLastSentence = false;
                }
            } else {
                title = sentence;
                if (sentence instanceof FinalResult) titleSet = true;
            }
        }

        public Book build() throws IllegalArgumentException {
            return new Book(title, recordDate, results.toArray(new Result[0]), getEstimateRecordDuration());
        }

        public Duration getEstimateRecordDuration() {
            return Duration.between(estimateRecordStart, Instant.now());
        }
    }
}
