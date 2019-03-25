package com.tobi.voicebooks.models;

import java.util.Date;

import androidx.annotation.NonNull;

public class Word {
    public final String word;
    public final Date startTime;
    public final Date endTime;

    public Word(String word, Date startTime, Date endTime) {
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
