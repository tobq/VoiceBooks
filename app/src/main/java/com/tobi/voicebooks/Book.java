package com.tobi.voicebooks;

import android.annotation.SuppressLint;

import java.time.Duration;
import java.util.Date;

public class Book {
    public final String title;
    public final Date creation;
    private final Duration length;

    public Book(String title, Date creation, Duration length) {
        this.title = title;
        this.creation = creation;
        this.length = length;
    }

    @SuppressLint("DefaultLocale")
    public String getLength() {
        long seconds = length.getSeconds();
        return String.format(
                "%d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60
        );
    }
}
