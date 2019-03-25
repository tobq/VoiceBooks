package com.tobi.voicebooks.db;

import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.db.entities.BookWord;

import java.util.Date;
import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SentenceDao {
    @Query("SELECT MIN(startTime) FROM word WHERE sentence_id = :id")
    Date getStart(int id);

    @Query("SELECT MAX(endTime) FROM word WHERE sentence_id = :id")
    Date getEnd(int id);

    @Query("SELECT * FROM book WHERE id IN (:ids)")
    List<BookEntity> loadAllByIds(int[] ids);

    @Query("SELECT * FROM word WHERE sentence_id = :id ORDER BY startTime")
    List<BookWord> getWords(int id);

    @Query("SELECT * FROM book WHERE title LIKE :title LIMIT 1")
    BookEntity findByTitle(String title);

    @Insert
    void insertAll(BookEntity... books);

    @Delete
    void delete(BookEntity books);
}

