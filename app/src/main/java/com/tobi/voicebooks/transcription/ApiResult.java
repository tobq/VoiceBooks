package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.models.Word;

import androidx.annotation.NonNull;

public class ApiResult {
    private final Word[] words;
    private final String transcript;

    public ApiResult(String transcript, Word[] words) {
        this.transcript = transcript;
        this.words = words;
    }

    public Word[] getWords() {
        return words;
    }
}
