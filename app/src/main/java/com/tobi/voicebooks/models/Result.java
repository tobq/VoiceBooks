package com.tobi.voicebooks.models;

import com.tobi.voicebooks.transcription.Transcriber;

import androidx.annotation.NonNull;

public class Result {
    private final String transcript;

    public Result(String transcript) {
        this.transcript = transcript;
    }

    @NonNull
    @Override
    public String toString() {
        return transcript;
    }
}
