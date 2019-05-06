package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.models.Word;

import java.time.Duration;

public class TranscriberResult {
    private final Word[] words;
    private final Duration duration;

    public TranscriberResult(Word[] words, Duration duration) {
        this.words = words;
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
    }

    public Word[] getWords() {
        return words;
    }
}
