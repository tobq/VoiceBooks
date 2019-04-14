package com.tobi.voicebooks.db;

import com.tobi.voicebooks.db.entities.BookEntity;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface BookDao {
    @Query("SELECT * FROM BookEntity")
    LiveData<List<BookEntity>> load();

    @Insert
    long[] insert(BookEntity... books);

    @Insert
    long insert(BookEntity book);

    @Delete
    void delete(BookEntity books);
}

