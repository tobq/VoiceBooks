package com.tobi.voicebooks.db.daos;

import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.db.entities.TitleWord;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface TitleWordDao {
    @Query("SELECT * FROM TitleWord WHERE bookId = :bookId  ORDER BY startTime")
    LiveData<List<TitleWord>> load(long bookId);

    @Query("SELECT * FROM TitleWord WHERE bookId = :bookId  ORDER BY startTime")
    BookWord[] get(long bookId);

    @Insert
    long[] insert(TitleWord... books);

    @Insert
    long insert(TitleWord book);
}

