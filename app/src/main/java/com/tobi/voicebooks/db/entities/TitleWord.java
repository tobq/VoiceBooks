package com.tobi.voicebooks.db.entities;

import com.tobi.voicebooks.models.Word;

import java.time.Duration;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(foreignKeys = {@ForeignKey(entity = BookEntity.class, parentColumns = {"id"}, childColumns = {"bookId"})}
)
public class TitleWord extends BookWord {
    public TitleWord(@NonNull Long bookId, @NonNull String word, @NonNull Duration startTime, @NonNull Duration endTime) {
        super(bookId, word, startTime, endTime);
    }

    public TitleWord(@NonNull Long bookId, Word word) {
        super(bookId, word);
    }
}