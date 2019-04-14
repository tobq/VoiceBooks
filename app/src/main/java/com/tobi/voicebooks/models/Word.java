package com.tobi.voicebooks.models;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import androidx.annotation.NonNull;

public class Word {
    public final String word;
    public final Duration startTime;
    public final Duration endTime;

    public Word(String word, Duration startTime, Duration endTime) {
        this.word = word;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @NonNull
    @Override
    public String toString() {
        return word;
    }
}
