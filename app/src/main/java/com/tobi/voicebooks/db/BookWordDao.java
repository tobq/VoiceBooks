package com.tobi.voicebooks.db;

import com.tobi.voicebooks.db.entities.BookWord;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface BookWordDao {
    @Query("SELECT * FROM bookword WHERE bookId = :bookId ORDER BY startTime")
    LiveData<List<BookWord>> load(long bookId);

    @Insert
    long[] insert(BookWord... book);

    @Insert
    long insert(BookWord book);
}

