package com.tobi.voicebooks.db.entities;

import com.tobi.voicebooks.db.DurationConverter;
import com.tobi.voicebooks.db.InstantConverter;

import java.time.Duration;
import java.time.Instant;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity
public class BookEntity {
    @NonNull
    @TypeConverters(DurationConverter.class)
    public final Duration duration;
    @NonNull
    @TypeConverters(InstantConverter.class)
    public final Instant creation;
    @PrimaryKey(autoGenerate = true)
    public long id;

    public BookEntity(@NonNull Instant creation, @NonNull Duration duration) {
        this.duration = duration;
        this.creation = creation;
    }
}
