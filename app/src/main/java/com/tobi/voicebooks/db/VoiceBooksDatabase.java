package com.tobi.voicebooks.db;

import com.tobi.voicebooks.db.daos.BookDao;
import com.tobi.voicebooks.db.daos.BookWordDao;
import com.tobi.voicebooks.db.daos.TitleWordDao;
import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.db.entities.TitleWord;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {BookEntity.class, BookWord.class, TitleWord.class}, version = 7)
public abstract class VoiceBooksDatabase extends RoomDatabase {
    public abstract BookDao books();

    public abstract TitleWordDao titleWords();

    public abstract BookWordDao bookWords();
}