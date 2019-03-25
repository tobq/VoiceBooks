package com.tobi.voicebooks.db.entities;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "sentence",
        foreignKeys = {@ForeignKey(entity = BookEntity.class, parentColumns = {"id"}, childColumns = {"book_id"})}
)
public class BookSentence {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "book_id")
    public final int bookId;

    public BookSentence(int bookId) {
        this.bookId = bookId;
    }
}
