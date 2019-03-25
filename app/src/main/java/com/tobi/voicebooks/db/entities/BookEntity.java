package com.tobi.voicebooks.db.entities;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "book")
public class BookEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public Date creation;
}
