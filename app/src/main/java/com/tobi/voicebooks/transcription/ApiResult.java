package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.models.Word;

import java.time.Duration;

public class ApiResult {
    private final Word[] words;
    private final String transcript;
    private final Duration duration;

    public ApiResult(String transcript, Word[] words, Duration duration) {
        this.transcript = transcript;
        this.words = words;
        this.duration = duration;
    }

    public Word[] getWords() {
        return words;
    }

    public Duration getDuration() {
        return duration;
    }
}
