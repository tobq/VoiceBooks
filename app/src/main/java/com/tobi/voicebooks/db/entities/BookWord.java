package com.tobi.voicebooks.db.entities;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "word",
        foreignKeys = {@ForeignKey(entity = BookSentence.class, parentColumns = {"id"}, childColumns = {"sentence_id"})}
)
public class BookWord {
    @ColumnInfo(name = "sentence_id")
    public final int sentenceId;
    public final String word;
    public final Date startTime;
    public final Date endTime;
    @PrimaryKey(autoGenerate = true)
    public int id;

    public BookWord(int sentenceID, String word, Date startTime, Date endTime) {
        this.sentenceId = sentenceID;
        this.word = word;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
