package com.tobi.voicebooks.models;

public class FinalResult extends Result {
    private final Word[] words;

    public FinalResult(String transcript, Word[] words) {
        super(transcript);
        this.words = words;
    }
}
