package com.tobi.voicebooks.db.entities;

import com.tobi.voicebooks.db.DurationConverter;
import com.tobi.voicebooks.models.Word;

import java.time.Duration;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(foreignKeys = @ForeignKey(
        entity = BookEntity.class,
        parentColumns = {"id"},
        childColumns = {"bookId"})
)
public class BookWord {
    // Boxed (Long) as requirement so Room framework can check for null
    // equivalence (no parent), on a cascade delete
    @NonNull
    public final Long bookId;
    @NonNull
    public final String word;
    @NonNull
    @TypeConverters(DurationConverter.class)
    public final Duration startTime;
    @NonNull
    @TypeConverters(DurationConverter.class)
    public final Duration endTime;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public BookWord(@NonNull Long bookId, Word word) {
        this(bookId, word.word, word.startTime, word.endTime);
    }

    public BookWord(
            @NonNull Long bookId,
            @NonNull String word,
            @NonNull Duration startTime,
            @NonNull Duration endTime
    ) {
        this.bookId = bookId;
        this.word = word;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Ignore
    public boolean equals(BookWord titleWord) {
        return id == titleWord.id &&
                startTime == titleWord.startTime &&
                endTime == titleWord.endTime &&
                word.equals(titleWord.word);
    }
}
